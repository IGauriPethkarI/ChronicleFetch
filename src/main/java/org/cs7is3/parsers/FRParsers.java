package org.cs7is3.parsers;

import java.util.*;
import java.util.regex.*;

import static org.cs7is3.constants.Constants.REPLACEMENT_ENTITIES;

public class FRParsers {



    public Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        // Remove SGML comments safely
        docBlock = docBlock.replaceAll("(?s)<!--.*?-->", "").trim();

        // Extract main fields
        map.put("DOCNO", extractAllTag(docBlock, "DOCNO"));
        map.put("PARENT", extractAllTag(docBlock, "PARENT"));
        map.put("TEXT", extractAllTag(docBlock, "TEXT"));
        map.put("USDEPT", extractAllTag(docBlock, "USDEPT"));
        map.put("USBUREAU", extractAllTag(docBlock, "USBUREAU"));
        map.put("CFRNO", extractAllTag(docBlock, "CFRNO"));
        map.put("RINDOCK", extractAllTag(docBlock, "RINDOCK"));
        map.put("AGENCY", extractAllTag(docBlock, "AGENCY"));
        map.put("ACTION", extractAllTag(docBlock, "ACTION"));
        map.put("SUMMARY", extractAllTag(docBlock, "SUMMARY"));
        map.put("DATE", extractAllTag(docBlock, "DATE"));
        map.put("FURTHER", extractAllTag(docBlock, "FURTHER"));
        map.put("SUPPLEM", extractAllTag(docBlock, "SUPPLEM"));
        map.put("SOURCE", "fr");

        // Decode SGML entities
        map.replaceAll((k, v) -> decodeEntities(v));

        return map;
    }

    // Extract all occurrences of a tag and join them
    private String extractAllTag(String text, String tag) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String content = matcher.group(1).replaceAll("\\s+", " ").trim();
            if (!content.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(content);
            }
        }
        return sb.toString();
    }

    private String decodeEntities(String input) {
        if (input == null) return "";
        String decoded = input;
        for (Map.Entry<String, String> e : REPLACEMENT_ENTITIES.entrySet()) {
            decoded = decoded.replace("[" + e.getKey() + "]", e.getValue());
        }
        return decoded;
    }
}
