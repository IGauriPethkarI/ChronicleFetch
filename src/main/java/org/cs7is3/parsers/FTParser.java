package org.cs7is3.parsers;

import java.util.*;
import java.util.regex.*;

public class FTParser {

    public static Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        map.put("DOCNO", extractSimple(docBlock, "DOCNO"));
        map.put("PROFILE", extractSimple(docBlock, "PROFILE"));
        map.put("DATE", extractSimple(docBlock, "DATE"));
        map.put("HEADLINE", extractBlock(docBlock, "HEADLINE"));
        map.put("BYLINE", extractBlock(docBlock, "BYLINE"));
        map.put("TEXT", extractBlock(docBlock, "TEXT"));
        map.put("PUB", extractSimple(docBlock, "PUB"));
        map.put("PAGE", extractSimple(docBlock, "PAGE"));
        map.put("SOURCE","ft");
        return map;
    }

    private static String extractSimple(String text, String tag) {
        Pattern p = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return clean(m.group(1));
        }
        return null;
    }

    private static String extractBlock(String text, String tag) {
        Pattern p = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String block = m.group(1);
            return clean(block);
        }
        return null;
    }

    private static String clean(String s) {
        if (s == null) return null;

        return s.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
