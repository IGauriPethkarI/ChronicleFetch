package org.cs7is3;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.cs7is3.analyzer.CustomAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Searcher {

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
            searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));

            Analyzer analyzer = new CustomAnalyzer();
            String[] fields = {"text", "headline"};
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("headline", 2.0f);
            boosts.put("text", 1.0f);

            MultiFieldQueryParser parser =
                    new MultiFieldQueryParser(fields, analyzer, boosts);
            parser.setDefaultOperator(MultiFieldQueryParser.Operator.OR);

            String runTag = "cs7is3";

            for (Topic topic : topics) {
                String queryText = buildQueryText(topic);
                if (queryText.isEmpty()) queryText = topic.title;

                Query query;
                try {
                    query = parser.parse(QueryParserBase.escape(queryText));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse query for topic " + topic.id, e);
                }

                TopDocs topDocs = searcher.search(query, numDocs);
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

        try (BufferedReader reader =
                     Files.newBufferedReader(topicsPath, StandardCharsets.UTF_8)) {

            String line;
            Topic current = null;
            StringBuilder title = null, desc = null, narr = null;
            boolean inDesc = false, inNarr = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

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

        if (!topic.title.isEmpty()) sb.append(topic.title);
        if (!topic.description.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(topic.description);
        }

        String posNarr = extractPositiveNarrative(topic.narrative);
        if (!posNarr.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(posNarr);
        }

        return sb.toString().trim();
    }

    private String extractPositiveNarrative(String narrative) {
        if (narrative == null || narrative.isEmpty()) return "";
        String[] sentences = narrative.split("\\.\\s+");
        StringBuilder sb = new StringBuilder();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.contains("not relevant") || lower.contains("irrelevant")) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(trimmed);
        }
        return sb.toString();
    }
}