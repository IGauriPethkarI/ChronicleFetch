package org.cs7is3.parsers;

import java.util.HashMap;
import java.util.Map;

public class FBISParser {

    public Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        map.put("DOCNO", extractTag(docBlock, "DOCNO"));
        map.put("HT", extractTag(docBlock, "HT"));
        String header = extractTag(docBlock, "HEADER");
        map.put("HEADER", header);
        map.put("H2", extractTag(header, "H2"));
        map.put("DATE", extractTag(header, "DATE1"));
        String h3 = extractTag(header, "H3");
        map.put("HEADLINE", extractTag(h3, "TI"));
        map.put("TEXT", extractTag(docBlock, "TEXT"));
        map.put("SOURCE", "fbis");

        return map;
    }


    private String extractTag(String text, String tag) {
        if (text == null) return null;

        String start = "<" + tag + ">";
        String end = "</" + tag + ">";

        int s = text.indexOf(start);
        int e = text.indexOf(end);

        if (s == -1 || e == -1) return null;

        s += start.length();
        return text.substring(s, e).trim();
    }
}
