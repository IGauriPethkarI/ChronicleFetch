package org.cs7is3.parsers;

import javax.swing.text.html.parser.DocumentParser;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FRParsers  {


    public Map<String, String> parse(String docBlock) {
        Map<String, String> map = new HashMap<>();
        
        docBlock = docBlock.replaceAll("(?m)^\\s*<!--.*?-->", "").trim();
        
        map.put("DOCNO", extractTag(docBlock, "DOCNO"));
        map.put("PARENT", extractTag(docBlock, "PARENT"));
        map.put("TEXT", extractTag(docBlock, "TEXT"));
        map.put("USDEPT", extractTag(docBlock, "USDEPT"));
        map.put("USBUREAU", extractTag(docBlock, "USBUREAU"));
        map.put("CFRNO", extractTag(docBlock, "CFRNO"));
        map.put("RINDOCK", extractTag(docBlock, "RINDOCK"));
        map.put("AGENCY", extractTag(docBlock, "AGENCY"));
        map.put("ACTION", extractTag(docBlock, "ACTION"));
        map.put("SUMMARY", extractTag(docBlock, "SUMMARY"));
        map.put("DATE", extractTag(docBlock, "DATE"));
        map.put("FURTHER", extractTag(docBlock, "FURTHER"));
        map.put("SUPPLEM", extractTag(docBlock, "SUPPLEM"));
        map.put("SOURCE", "fr");

        return map;
    }

    private String extractTag(String text, String tag) {
        Pattern pattern = Pattern.compile("(?is)<" + tag + ">(.*?)</" + tag + ">");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }
}
