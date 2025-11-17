package org.cs7is3.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.CharArraySet;

public class CustomAnalyzer extends Analyzer {

    private static final CharArraySet STOP_WORDS = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;

    @Override
    protected TokenStreamComponents createComponents(String field) {

        StandardTokenizer tokenizer = new StandardTokenizer();

        TokenStream stream = tokenizer;

        stream = new LowerCaseFilter(stream);
        stream = new StopFilter(stream, STOP_WORDS);
        stream = new ASCIIFoldingFilter(stream);
        stream = new EnglishPossessiveFilter(stream);
        stream = new ShingleFilter(stream, 2, 3);
        stream = new PorterStemFilter(stream);

        return new TokenStreamComponents(tokenizer, stream);
    }
}
