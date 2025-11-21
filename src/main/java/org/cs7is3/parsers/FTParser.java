package org.cs7is3.parsers;

import java.util.*;
import java.util.regex.*;

import static org.cs7is3.constants.Constants.REPLACEMENT_ENTITIES;

public class FTParser {


    public static Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        map.put("DOCNO", extractAll(docBlock, "DOCNO"));
        map.put("PROFILE", extractAll(docBlock, "PROFILE"));
        map.put("DATE", extractAll(docBlock, "DATE"));
        map.put("HEADLINE", extractAll(docBlock, "HEADLINE"));
        map.put("BYLINE", extractAll(docBlock, "BYLINE"));
        map.put("TEXT", extractAll(docBlock, "TEXT"));
        map.put("PUB", extractAll(docBlock, "PUB"));
        map.put("PAGE", extractAll(docBlock, "PAGE"));
        map.put("SOURCE", "ft");

        // Decode SGML entities
        map.replaceAll((k, v) -> decodeEntities(v));

        return map;
    }

    private static String extractAll(String text, String tag) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String content = clean(matcher.group(1));
            if (!content.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(content);
            }
        }
        return sb.toString();
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String decodeEntities(String input) {
        if (input == null) return "";
        String decoded = input;
        for (Map.Entry<String, String> e : REPLACEMENT_ENTITIES.entrySet()) {
            decoded = decoded.replace("&" + e.getKey() + ";", e.getValue());
        }
        return decoded;
    }
}
