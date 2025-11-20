package org.cs7is3;

// TODO: Implement your Lucene indexer
// This class should build a Lucene index from the document collection
//
// Requirements:
// 1. Parse documents from the "Assignment Two" dataset
// 2. Extract relevant fields (DOCNO, TITLE, TEXT, etc.)
// 3. Create a Lucene index with appropriate analyzers
// 4. Handle document parsing errors gracefully
//
// The GitHub Actions workflow will call:
//   indexer.buildIndex(Path docsPath, Path indexPath)

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

public class Indexer {

    private IndexWriter writer;
    private int totalDocuments = 0;
    private int skippedDuplicates = 0;
    private int skippedEmptyDocs = 0;
    private final Set<Integer> seenHashes = new HashSet<>();

    private static final int COMMIT_BATCH_SIZE = 5000;

    private static final Pattern DOC_PATTERN = Pattern.compile("(?is)<DOC>(.*?)</DOC>");

    public void buildIndex(Path docsPath, Path indexPath) throws IOException {
        try {
            // Analyzer
            //IndexWriterConfig cfg = new IndexWriterConfig(new EnglishAnalyzer());
            IndexWriterConfig cfg = new IndexWriterConfig(new CustomAnalyzer());
            cfg.setSimilarity(new BM25Similarity(1.2f, 0.75f));
            cfg.setRAMBufferSizeMB(256.0);
            cfg.setUseCompoundFile(false);
            this.writer = new IndexWriter(FSDirectory.open(indexPath), cfg);

            try (Stream<Path> paths = Files.walk(docsPath)) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                System.out.println("Indexing file: " + path);
                                indexFile(path.toFile());
                            } catch (Exception e) {
                                System.err.println("Failed to index file: " + path + " -> " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
            }

            if (writer != null) {
                writer.commit();
                writer.close();
                System.out.println("Total documents indexed: " + totalDocuments);
                System.out.println("Duplicates skipped: " + skippedDuplicates);
                System.out.println("Empty documents skipped: " + skippedEmptyDocs);
            }

            System.out.println("Indexing completed.");
        } catch (Exception e) {
            throw new IOException("Indexing failed", e);
        }
    }

    private void indexFile(java.io.File file) throws Exception {
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
                if (parser instanceof LATParser) {
                    parsed = ((LATParser) parser).parse(docBlock);
                } else if (parser instanceof FBISParser) {
                    parsed = ((FBISParser) parser).parse(docBlock);
                } else if (parser instanceof FRParsers) {
                    parsed = ((FRParsers) parser).parse(docBlock);
                } else if (parser instanceof FTParser) {
                    parsed = FTParser.parse(docBlock);
                } else {
                    parsed = new LATParser().parse(docBlock);
                }
            } catch (Exception e) {
                System.err.println("Parser failed for file " + file + " : " + e.getMessage());
                continue;
            }

            if (parsed == null || parsed.isEmpty()) {
                continue;
            }
            Document luceneDoc = normalize(parsed);
            if (luceneDoc == null) {
                skippedDuplicates++;
                continue;
            }

            String text = luceneDoc.get("text");
            if (text == null || text.isBlank()) {
                String headline = luceneDoc.get("headline");
                String metadata = luceneDoc.get("metadata");
                String fallback = (headline == null ? "" : headline) + " " + (metadata == null ? "" : metadata);

                if (fallback.isBlank()) {
                    skippedEmptyDocs++;
                    continue;
                }

                luceneDoc.removeField("text");
                luceneDoc.add(new TextField("text", fallback.trim(), Field.Store.YES));
            }

            writer.addDocument(luceneDoc);
            this.totalDocuments++;
            docsIndexedInFile++;

            if (totalDocuments % COMMIT_BATCH_SIZE == 0) {
                writer.commit();
                System.out.println("  >> Committed " + totalDocuments + " documents to index");
            }

            if (docsIndexedInFile % 1000 == 0) {
                System.out.printf("  - %d docs indexed from %s%n", docsIndexedInFile, file.getName());
            }
        }

        if (docsIndexedInFile > 0) {
            System.out.printf("✓ Indexed %d documents from %s (Total: %d)%n",
                    docsIndexedInFile, file.getName(), this.totalDocuments);
        }
    }

    private Object getParser(String path) {
        String lower = path.toLowerCase();
        if (lower.contains("latimes") || lower.contains("/la")) return new LATParser();
        if (lower.contains("fbis")) return new FBISParser();
        if (lower.contains("fr94") || lower.contains("/fr/")) return new FRParsers();
        if (lower.contains("/ft/")) return new FTParser();

        return new LATParser();
    }

    private Document normalize(Map<String, String> raw) {
        Document doc = new Document();

        String docno = pick(raw, "DOCNO", "DOCID", "ID");
        String text = pick(raw, "TEXT", "BODY", "CONTENT", "SUMMARY", "SUPPLEM");
        String headline = pick(raw, "HEADLINE", "TI", "TITLE", "H3");
        String date = pick(raw, "DATE", "DATE1", "DATELINE", "PUBDATE");
        String section = pick(raw, "SECTION", "CATEGORY", "F", "PAGE");
        String source = raw.getOrDefault("SOURCE", guessSourceFromRaw(raw));

        // I hash both headline and text to catch duplicates better
        int contentHash = Objects.hash(
                safe(headline).toLowerCase(),
                safe(text).toLowerCase().substring(0, Math.min(safe(text).length(), 500))
        );

        if (seenHashes.contains(contentHash)) {
            return null;
        }
        seenHashes.add(contentHash);

        doc.add(new StringField("docno", safe(docno), Field.Store.YES));
        doc.add(new StringField("source", safe(source), Field.Store.YES));
        doc.add(new TextField("text", safe(text), Field.Store.YES));
        doc.add(new TextField("headline", safe(headline), Field.Store.YES));
        doc.add(new StoredField("headline_raw", safe(headline)));
        doc.add(new StringField("date", safe(normalizeDate(date)), Field.Store.YES));
        doc.add(new TextField("section", safe(section), Field.Store.YES));

        Set<String> exclude = new HashSet<>(Arrays.asList(
                "DOCNO", "DOCID", "ID", "TEXT", "BODY", "CONTENT", "SUMMARY", "SUPPLEM",
                "HEADLINE", "TI", "TITLE", "H3", "DATE", "DATE1", "DATELINE", "PUBDATE",
                "SECTION", "CATEGORY", "F", "PAGE", "SOURCE"
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

        String meta = buildRawMetadata(raw);
        if (meta.length() > 35000)
            meta = meta.substring(0, 35000);
        doc.add(new TextField("metadata_raw", String.join(" ",meta), Field.Store.YES));

        return doc;
    }

    private String normalizeDate(String input) {
        if (input == null || input.isBlank()) return "";

        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) return digits.substring(0, 8);
        return digits;
    }

    private String buildRawMetadata(Map<String,String> raw) {
        StringBuilder sb = new StringBuilder();
        raw.forEach((k,v)-> {
            sb.append(k).append(": ").append(v).append("\n");
        });
        return sb.toString();
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
        if (raw.containsKey("HT" ) || raw.containsKey("PROFILE") || raw.containsKey("BYLINE")) return "ft";
        if (raw.containsKey("H2") || raw.containsKey("DATE1") || raw.containsKey("HEADER")) return "fbis";
        if (raw.containsKey("USDEPT") || raw.containsKey("CFRNO") || raw.containsKey("AGENCY")) return "fr";
        if (raw.containsKey("GRAPHIC") || raw.containsKey("TYPE") || raw.containsKey("DOCID")) return "latimes";

        return "unknown";
    }
}

