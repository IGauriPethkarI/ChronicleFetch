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
import static org.cs7is3.constants.Constants.NEWS_STOP_WORDS;

public class CustomAnalyzer extends Analyzer {

    private static final CharArraySet STOP_WORDS = new CharArraySet(NEWS_STOP_WORDS, true);
    private static final int MIN_TOKEN_LENGTH = 2; 
    private static final int MAX_TOKEN_LENGTH = 15;

    @Override
    protected TokenStreamComponents createComponents(String field) {

        StandardTokenizer tokenizer = new StandardTokenizer();

        TokenStream stream = tokenizer;
    
        stream = new LowerCaseFilter(stream);
        stream = new ASCIIFoldingFilter(stream);
        stream = new EnglishPossessiveFilter(stream);

        if ("persons".equals(field)) {
            stream = new LengthFilter(stream, MIN_TOKEN_LENGTH, MAX_TOKEN_LENGTH);
            return new TokenStreamComponents(tokenizer, stream);
        }
        
        stream = new StopFilter(stream, STOP_WORDS);
        stream = new LengthFilter(stream, MIN_TOKEN_LENGTH, MAX_TOKEN_LENGTH);
        stream = new PorterStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }
}