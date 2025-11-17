package org.cs7is3;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import org.cs7is3.analyzer.CustomAnalyzer;
import org.cs7is3.parsers.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TrialIndexer {

    private final IndexWriter writer;
    private int TotalDocuments = 0;

    private static final Pattern   DOC_PATTERN = Pattern.compile("(?is)<DOC>(.*?)</DOC>");

    public TrialIndexer(String indexDir) throws Exception {
        IndexWriterConfig cfg = new IndexWriterConfig(new CustomAnalyzer());
        cfg.setSimilarity(new BM25Similarity(1.2f, 0.75f));
        cfg.setRAMBufferSizeMB(256.0);
        cfg.setUseCompoundFile(false);
        this.writer = new IndexWriter(FSDirectory.open(Path.of(indexDir)), cfg);
    }

    public void close() throws Exception {
        if (writer != null) {
            writer.commit();
            writer.close();
        }
    }

    public static void createIndex(String dataDirectory, String indexDirectory) throws Exception {
        TrialIndexer indexer = new TrialIndexer(indexDirectory);

        try (Stream<Path> paths = Files.walk(Paths.get(dataDirectory))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            System.out.println("Indexing file: " + path);
                            indexer.indexFile(path.toFile());
                        } catch (Exception e) {
                            System.err.println("Failed to index file: " + path + " -> " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
        }

        indexer.close();
        System.out.println("Indexing completed.");
    }

    public void indexFile(java.io.File file) throws Exception {
        final String content;
        try {
            content = Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new IOException("Unable to read file: " + file.getAbsolutePath(), e);
        }

        Object parser = getParser(file.getPath());

        Matcher m = DOC_PATTERN.matcher(content);
        int docsIndexedInFile = 0;
        while (m.find()) {
            String docBlock = m.group(1);

            Map<String, String> parsed = null;
            try {
                parsed = switch (parser) {
                    case LATParser latParser -> latParser.parse(docBlock);
                    case FBISParser fbisParser -> fbisParser.parse(docBlock);
                    case FRParsers frParsers -> frParsers.parse(docBlock);
                    case FTParser ftParser -> ftParser.parse(docBlock);
                    default -> new LATParser().parse(docBlock);
                };
            } catch (Exception e) {
                System.err.println("Parser failed for file " + file + " : " + e.getMessage());
                e.printStackTrace();
            }

            if (parsed == null) continue;

            Document luceneDoc = normalize(parsed);
            String text = luceneDoc.get("text");

            if (text == null || text.isBlank()) {

                String headline = luceneDoc.get("headline");
                String metadata = luceneDoc.get("metadata");
                String fallback = (headline == null ? "" : headline) + " " + (metadata == null ? "" : metadata);

                if (fallback.isBlank()) continue;

                luceneDoc.removeField("text");
                luceneDoc.add(new TextField("text", fallback.trim(), Field.Store.YES));
            }

            writer.addDocument(luceneDoc);
            this.TotalDocuments++;
            docsIndexedInFile++;
            if ((docsIndexedInFile % 1000) == 0) {
                System.out.printf("  - %d docs indexed in file %s%n", docsIndexedInFile, file.getName());
            }
        }

        if (docsIndexedInFile > 0) {
            System.out.printf("Indexed %d documents from %s%n", docsIndexedInFile, file.getName());
            System.out.printf("Total Indexed %d documents", this.TotalDocuments);
        }
    }

    private Object getParser(String path) {
        String lower = path.toLowerCase();
        if (lower.contains("latimes") || lower.contains("la")) return new LATParser();
        if (lower.contains("fbis")) return new FBISParser();
        if (lower.contains("fr94") || lower.contains("fr")) return new FRParsers();
        if (lower.contains("ft")) return new FTParser();
        // fallback
        return new LATParser();
    }

    private Document normalize(Map<String, String> raw) {
        Document doc = new Document();

        String docno = pick(raw, "DOCNO", "DOCID", "ID");
        String text = pick(raw, "TEXT", "BODY", "CONTENT");
        String headline = pick(raw, "HEADLINE", "TI", "TITLE");
        String date = pick(raw, "DATE", "DATE1", "DATELINE", "PUBDATE");
        String section = pick(raw, "SECTION", "CATEGORY", "F", "PAGE");
        String source = raw.getOrDefault("SOURCE", guessSourceFromRaw(raw));

        doc.add(new StringField("docno", safe(docno), Field.Store.YES));
        doc.add(new StringField("source", safe(source), Field.Store.YES));
        doc.add(new TextField("text", safe(text), Field.Store.YES));
        doc.add(new TextField("headline", safe(headline), Field.Store.YES));
        doc.add(new StringField("date", safe(date), Field.Store.YES));
        doc.add(new TextField("section", safe(section), Field.Store.YES));

        Set<String> exclude = new HashSet<>(Arrays.asList(
                "DOCNO","DOCID","ID","TEXT","BODY","CONTENT","HEADLINE","TI","TITLE",
                "DATE","DATE1","DATELINE","PUBDATE","SECTION","CATEGORY","F","PAGE","SOURCE"
        ));

        StringBuilder metaBuilder = new StringBuilder();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v == null) continue;
            if (exclude.contains(k.toUpperCase())) continue;
            metaBuilder.append(k).append(": ").append(v).append("\n");
        }

        String metadata = metaBuilder.toString().trim();
        doc.add(new StoredField("metadata", metadata));

        return doc;
    }

    private String pick(Map<String, String> m, String... keys) {
        for (String k : keys) {
            if (k == null) continue;
            String val = m.get(k);
            if (val == null) {
                val = m.get(k.toUpperCase());
            }
            if (val != null && !val.isBlank()) return val.trim();
        }
        return "";
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String guessSourceFromRaw(Map<String, String> raw) {
        if (raw.containsKey("SOURCE")) return raw.get("SOURCE");
        if (raw.containsKey("HT") || raw.containsKey("BYLINE")) return "ft";
        if (raw.containsKey("H2") || raw.containsKey("DATE1")) return "fbis";
        if (raw.containsKey("USDEPT") || raw.containsKey("CFRNO")) return "fr";
        if (raw.containsKey("GRAPHIC") || raw.containsKey("DOCID")) return "latimes";
        return "unknown";
    }
}
