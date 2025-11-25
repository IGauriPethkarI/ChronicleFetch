package org.cs7is3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermVectors;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.cs7is3.analyzer.CustomAnalyzer;

public class Searcher {

    private static final boolean USE_TITLE = true;
    private static final boolean USE_DESCRIPTION = true;
    private static final boolean USE_NARRATIVE = true;

    // PRF flags
    private static final boolean USE_PRF = true;
    private static final int PRF_FEEDBACK_DOCS = 40;   // 30-50
    private static final int PRF_EXPANSION_TERMS = 25; // 20-30
    private static final float PRF_BOOST = 0.4f;       // 0.3-0.5

    private static class Topic {
        String id;
        String title = "";
        String description = "";
        String narrative = "";
    }

    public void searchTopics(Path indexPath, Path topicsPath,
                             Path outputRun, int numDocs) throws IOException {

        List<Topic> topics = parseTopics(topicsPath);

        try (Directory dir = FSDirectory.open(indexPath);
             DirectoryReader reader = DirectoryReader.open(dir);
             BufferedWriter writer = Files.newBufferedWriter(outputRun, StandardCharsets.UTF_8)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Similarity bm25 = new BM25Similarity(0.9f, 0.8f);

            searcher.setSimilarity(bm25);

            Analyzer analyzer = new CustomAnalyzer();
            QueryParser parser = new QueryParser("text", analyzer);
            parser.setDefaultOperator(MultiFieldQueryParser.Operator.OR);

            String runTag = "cs7is3";

            for (Topic topic : topics) {
                String queryText = buildQueryText(topic);

                Query baseQuery;
                try {
                    baseQuery = parser.parse(queryText);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse query for topic " + topic.id, e);
                }

                BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
                bqBuilder.add(baseQuery, BooleanClause.Occur.SHOULD);         

                Query boostedQuery = bqBuilder.build();

                TopDocs feedback = searcher.search(boostedQuery, Math.max(numDocs, PRF_FEEDBACK_DOCS));

                Query finalQuery = boostedQuery;
                if (USE_PRF) {
                    finalQuery = expandWithPRF(boostedQuery, feedback, reader,
                                               PRF_EXPANSION_TERMS, PRF_BOOST);
                }
                TopDocs topDocs = searcher.search(finalQuery, numDocs);

                ScoreDoc[] hits = topDocs.scoreDocs;

                for (int i = 0; i < hits.length; i++) {
                    Document doc = searcher.storedFields().document(hits[i].doc);
                    String docno = doc.get("docno");

                    String line = String.format(
                            Locale.ROOT,
                            "%s Q0 %s %d %f %s",
                            topic.id, docno, i + 1, hits[i].score, runTag
                    );
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    private List<Topic> parseTopics(Path topicsPath) throws IOException {
        List<Topic> topics = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(topicsPath, StandardCharsets.UTF_8)) {
            String line;
            Topic current = null;
            StringBuilder title = null, desc = null, narr = null;
            boolean inDesc = false, inNarr = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim().replaceAll("\\s+", " ");

                if (line.startsWith("<top")) {
                    current = new Topic();
                    title = new StringBuilder();
                    desc = new StringBuilder();
                    narr = new StringBuilder();
                    inDesc = false;
                    inNarr = false;

                } else if (line.startsWith("<num")) {
                    int idx = line.indexOf("Number:");
                    current.id = line.substring(idx + "Number:".length()).trim();

                } else if (line.startsWith("<title>")) {
                    title.append(line.replaceFirst("<title>", "").trim());

                } else if (line.startsWith("<desc>")) {
                    inDesc = true;
                    inNarr = false;
                    String d = line.replaceFirst("<desc>", "").trim()
                                   .replaceFirst("Description:", "").trim();
                    if (!d.isEmpty()) {
                        if (desc.length() > 0) desc.append(' ');
                        desc.append(d);
                    }

                } else if (line.startsWith("<narr>")) {
                    inNarr = true;
                    inDesc = false;
                    String n = line.replaceFirst("<narr>", "").trim()
                                   .replaceFirst("Narrative:", "").trim();
                    if (!n.isEmpty()) {
                        if (narr.length() > 0) narr.append(' ');
                        narr.append(n);
                    }

                } else if (line.startsWith("</top>")) {
                    current.title = title.toString().trim();
                    current.description = desc.toString().trim();
                    current.narrative = narr.toString().trim();
                    topics.add(current);
                    current = null;

                } else if (current != null) {
                    if (inDesc) {
                        if (desc.length() > 0) desc.append(' ');
                        desc.append(line);
                    } else if (inNarr) {
                        if (narr.length() > 0) narr.append(' ');
                        narr.append(line);
                    }
                }
            }
        }
        return topics;
    }

    private String buildQueryText(Topic topic) {
        StringBuilder sb = new StringBuilder();

        if (USE_TITLE && !topic.title.isEmpty()) {
            sb.append("text:(").append(QueryParserBase.escape(topic.title)).append(")^6 ");
        }
    
        if (USE_DESCRIPTION && !topic.description.isEmpty()) {
            sb.append("text:(").append(QueryParserBase.escape(topic.description)).append(")^3 ");
        }
    
        if (USE_NARRATIVE) {
            String posNarr = extractPositiveNarrative(topic.narrative);
            if (!posNarr.isEmpty()) {
                sb.append("text:(").append(QueryParserBase.escape(posNarr)).append(")^1 ");
            }
        }
        return sb.toString().trim();
    }

    private String extractPositiveNarrative(String narrative) {
        if (narrative == null) return "";
        narrative = narrative.trim();
        if (narrative.isEmpty()) return "";
        narrative = narrative.replaceAll("\\s+", " ");
    
        String[] sentences = narrative.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
    
        for (String sentence : sentences) {
            String s = sentence.trim();
            if (s.isEmpty()) continue;
    
            String[] clauseLevel1 = s.split("[;:]");
            for (String clause : clauseLevel1) {
                String c1 = clause.trim();
                if (c1.isEmpty()) continue;
    
                String[] subClauses = c1.split(",");
                for (String sub : subClauses) {
                    String c = sub.trim();
                    if (c.isEmpty()) continue;
    
                    String lower = c.toLowerCase(Locale.ROOT);
                    if (lower.contains("not relevant") || lower.contains("irrelevant")) {
                        continue;
                    }
    
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
    
    private Query expandWithPRF(Query baseQuery, TopDocs feedbackDocs,
        DirectoryReader reader, int topTerms, float boost) throws IOException {

        Map<String, Float> scores = new HashMap<>();
        TermVectors tvReader = reader.termVectors();

        java.util.Set<String> originalTerms = new java.util.HashSet<>();
        collectTerms(baseQuery, "text", originalTerms);

        int maxDoc = reader.maxDoc();

        int limit = Math.min(feedbackDocs.scoreDocs.length, PRF_FEEDBACK_DOCS);
        for (int i = 0; i < limit; i++) {
            int docId = feedbackDocs.scoreDocs[i].doc;
            Fields vectors = tvReader.get(docId);
            if (vectors == null) continue;

            Terms terms = vectors.terms("text");
            if (terms == null) continue;

            TermsEnum te = terms.iterator();
            BytesRef ref;
            while ((ref = te.next()) != null) {
                String term = ref.utf8ToString();
                if (term.length() < 4) continue;
                if (originalTerms.contains(term)) continue;

                int df = reader.docFreq(new Term("text", term));
                if (df < 3) continue;
                if (df > 0.3 * maxDoc) continue;

                float idf = (float) Math.log((maxDoc - df + 0.5f) / (df + 0.5f));

                float old = scores.getOrDefault(term, 0f);
                scores.put(term, old + idf);
            }
        }

        List<Map.Entry<String, Float>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        BooleanQuery.Builder expanded = new BooleanQuery.Builder();
        expanded.add(baseQuery, BooleanClause.Occur.SHOULD);

        int added = 0;
        for (Map.Entry<String, Float> e : sorted) {
            if (added >= topTerms) break;
            TermQuery tq = new TermQuery(new Term("text", e.getKey()));
            expanded.add(new BoostQuery(tq, boost), BooleanClause.Occur.SHOULD);
            added++;
        }
        return expanded.build();
    }

    private void collectTerms(Query q, String field, java.util.Set<String> out) {
        if (q instanceof BooleanQuery) {
            for (BooleanClause c : ((BooleanQuery) q).clauses()) {
                collectTerms(c.query(), field, out);
            }
        } else if (q instanceof TermQuery) {
            Term t = ((TermQuery) q).getTerm();
            if (field.equals(t.field())) {
                out.add(t.text());
            }
        } else if (q instanceof BoostQuery) {
            collectTerms(((BoostQuery) q).getQuery(), field, out);
        }
    }    
}