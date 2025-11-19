package org.cs7is3.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;

import static org.cs7is3.analyzer.StopWords.NEWS_STOP_WORDS;

/**
 * 1. StandardTokenizer - tokenizes text
 * 2. LowerCaseFilter - normalizes to lowercase
 * 3. ASCIIFoldingFilter - converts accented characters (e.g., café -> cafe)
 * 4. EnglishPossessiveFilter - removes possessives ('s)
 * 5. StopFilter - removes common English stopwords
 * 6. LengthFilter - removes very short tokens (< 2 chars)
 * 7. PorterStemFilter - applies Porter stemming algorithm
 * I removed ShingleFilter so we avoid index bloat and improve precision
 */
public class CustomAnalyzer extends Analyzer {

    private static final CharArraySet STOP_WORDS = new CharArraySet(NEWS_STOP_WORDS, true);
    private static final int MIN_TOKEN_LENGTH = 2; // to improve noise reduction
    private static final int MAX_TOKEN_LENGTH = 40; // to filter out spam and errors

    @Override
    protected TokenStreamComponents createComponents(String field) {

        StandardTokenizer tokenizer = new StandardTokenizer();

        TokenStream stream = tokenizer;
        // I changed the order of some filters for better effectiveness
        // convert to lowercase first 
        stream = new LowerCaseFilter(stream);
       
        stream = new ASCIIFoldingFilter(stream);
        
        stream = new EnglishPossessiveFilter(stream);
        
        stream = new StopFilter(stream, STOP_WORDS);
        
        // filter out tokens that are too short or too long
        stream = new LengthFilter(stream, MIN_TOKEN_LENGTH, MAX_TOKEN_LENGTH);
        
        // apply Porter stemming (running -> run, countries -> countri)
        stream = new PorterStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }
}
