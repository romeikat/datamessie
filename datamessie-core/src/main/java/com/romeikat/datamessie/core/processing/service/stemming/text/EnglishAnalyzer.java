package com.romeikat.datamessie.core.processing.service.stemming.text;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2017 Dr. Raphael Romeikat
 * =====================================================================
 * This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/gpl-3.0.html>.
 * =============================LICENSE_END=============================
 */

import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.tartarus.snowball.ext.German2Stemmer;

public final class EnglishAnalyzer extends StopwordAnalyzerBase {
  private final CharArraySet stemExclusionSet;

  public static CharArraySet getDefaultStopSet() {
    return DefaultSetHolder.DEFAULT_STOP_SET;
  }

  /**
   * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class accesses the
   * static final set the first time.;
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
  }

  /**
   * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
   */
  public EnglishAnalyzer() {
    this(DefaultSetHolder.DEFAULT_STOP_SET);
  }

  /**
   * Builds an analyzer with the given stop words.
   *
   * @param stopwords a stopword set
   */
  public EnglishAnalyzer(final CharArraySet stopwords) {
    this(stopwords, CharArraySet.EMPTY_SET);
  }

  /**
   * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is provided
   * this analyzer will add a {@link SetKeywordMarkerFilter} before stemming.
   *
   * @param stopwords a stopword set
   * @param stemExclusionSet a set of terms not to be stemmed
   */
  public EnglishAnalyzer(final CharArraySet stopwords, final CharArraySet stemExclusionSet) {
    super(stopwords);
    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
  }

  /**
   * Creates a {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} which tokenizes all
   * the text in the provided {@link Reader}.
   *
   * @return A {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} built from an
   *         {@link StandardTokenizer} filtered with {@link StandardFilter},
   *         {@link EnglishPossessiveFilter}, {@link LowerCaseFilter}, {@link StopFilter} ,
   *         {@link SetKeywordMarkerFilter} if a stem exclusion set is provided and
   *         {@link PorterStemFilter}.
   */
  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    final Tokenizer source;
    source = new StandardTokenizer();
    TokenStream result = new StandardFilter(source);

    // Remove terms that do not contain any alphabetic character
    result = new NumberFilter(result);

    // Remove possessives (trailing 's)
    result = new EnglishPossessiveFilter(result);

    // Converting to lower case is not necessary as this is done before stemming
    // result = new LowerCaseFilter(result);

    // Remove stopwords
    result = new StopFilter(result, stopwords);

    // Mark keywords
    if (!stemExclusionSet.isEmpty()) {
      result = new SetKeywordMarkerFilter(result, stemExclusionSet);
    }

    // Stem
    result = new SnowballFilter(result, new German2Stemmer());
    // Alternatives to the SnowballFilter:
    // result = new PorterStemFilter(result);

    return new TokenStreamComponents(source, result);
  }
}
