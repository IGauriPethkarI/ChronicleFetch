package org.cs7is3.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LATParser  {

    public Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();

        docBlock = docBlock.replaceAll("\\r?\\n", " ").trim();
        
        map.put("DOCNO", extractTag(docBlock, "DOCNO"));
        map.put("DOCID", extractTag(docBlock, "DOCID"));
        map.put("DATE", extractTag(docBlock, "DATE"));
        map.put("SECTION", extractTag(docBlock, "SECTION"));
        map.put("LENGTH", extractTag(docBlock, "LENGTH"));
        map.put("HEADLINE", extractTag(docBlock, "HEADLINE"));
        map.put("BYLINE", extractTag(docBlock, "BYLINE"));
        map.put("TEXT", extractTag(docBlock, "TEXT"));
        map.put("GRAPHIC", extractTag(docBlock, "GRAPHIC"));
        map.put("TYPE", extractTag(docBlock, "TYPE"));
        map.put("SOURCE", "latimes");

        return map;
    }

    private String extractTag(String text, String tag) {
        Pattern pattern = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String content = matcher.group(1)
                .replaceAll("(?i)<P>|</P>", "")
                .replaceAll("\\s+", " ")
                .trim();
            return content;
        }
        return null;
    }
}
