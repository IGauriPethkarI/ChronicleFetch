package org.cs7is3.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cs7is3.constants.Constants.REPLACEMENT_ENTITIES;

public class FBISParser {

    public Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        String docno = extractTag(docBlock, "DOCNO");
        map.put("DOCNO", docno);
        String ht = extractTag(docBlock, "HT");
        map.put("HT", ht);
        String header = extractTag(docBlock, "HEADER");
        map.put("HEADER", header);
        map.put("H2", extractTag(header, "H2"));
        map.put("DATE", extractTag(header, "DATE1"));
        String h3 = extractTag(header, "H3");
        String headline = extractTag(h3, "TI");
        map.put("HEADLINE", headline);
        map.put("TEXT", extractTag(docBlock, "TEXT"));
        map.put("SOURCE", "fbis");
        map.replaceAll((k,v) -> decodeEntities(v));

        return map;
    }

    // Extracts first occurrence of a tag (can be extended to multiple)
    private String extractTag(String text, String tag) {
        if (text == null) return null;

        Pattern p = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String decodeEntities(String input) {
        if (input == null) return null;
        String decoded = input;
        for (Map.Entry<String, String> e : REPLACEMENT_ENTITIES.entrySet()) {
            decoded = decoded.replace("[" + e.getKey() + "]", e.getValue());
        }
        return decoded;
    }
}
