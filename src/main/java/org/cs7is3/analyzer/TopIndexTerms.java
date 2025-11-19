package org.cs7is3.analyzer;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.analysis.Analyzer;

import java.nio.file.Path;
import java.util.*;

public class TopIndexTerms {

    private static final List<String> TEXT_FIELDS = List.of(
            "text", "headline", "person", "org", "loc", "section", "metadata_raw"
    );

    public static void main(String[] args) throws Exception {

        Path indexPath = Path.of("index");
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(indexPath));

        Analyzer analyzer = new org.cs7is3.analyzer.CustomAnalyzer();

        Set<String> stopwords = extractStopwords(analyzer);

        Map<String, Long> freqMap = new HashMap<>();

        for (LeafReaderContext ctx : reader.leaves()) {
            LeafReader leaf = ctx.reader();

            for (String field : TEXT_FIELDS) {
                Terms terms = leaf.terms(field);
                if (terms == null) continue;

                TermsEnum te = terms.iterator();

                BytesRef br;
                while ((br = te.next()) != null) {
                    String term = br.utf8ToString();

                    // exclude stopwords
                    if (stopwords.contains(term)) continue;

                    long freq = te.totalTermFreq();
                    freqMap.merge(term, freq, Long::sum);
                }
            }
        }

        reader.close();

        freqMap.entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(1000)
                .forEach(e ->
                        System.out.println(e.getKey() + " = " + e.getValue())
                );
    }

    private static Set<String> extractStopwords(Analyzer analyzer) {
        try {
            var field = analyzer.getClass().getDeclaredField("stopWords");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> sw = (Set<String>) field.get(analyzer);
            return sw;
        } catch (Exception e) {
            System.err.println("Failed to extract stopwords from CustomAnalyzer. Returning empty set.");
            return Set.of();
        }
    }
}
