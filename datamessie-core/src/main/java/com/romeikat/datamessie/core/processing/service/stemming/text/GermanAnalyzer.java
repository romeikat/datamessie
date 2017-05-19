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

// This file is encoded in UTF-8


import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.tartarus.snowball.ext.German2Stemmer;

public final class GermanAnalyzer extends StopwordAnalyzerBase {

  /** File containing default German stopwords. */
  public final static String DEFAULT_STOPWORD_FILE = "german_stop.txt";

  /**
   * Returns a set of default German-stopwords
   *
   * @return a set of default German-stopwords
   */
  public static final CharArraySet getDefaultStopSet() {
    return DefaultSetHolder.DEFAULT_SET;
  }

  private static class DefaultSetHolder {
    private static final CharArraySet DEFAULT_SET;

    static {
      try {
        DEFAULT_SET = WordlistLoader.getSnowballWordSet(
            IOUtils.getDecodingReader(SnowballFilter.class, DEFAULT_STOPWORD_FILE, StandardCharsets.UTF_8));
      } catch (final IOException ex) {
        // default set should always be present as it is part of the
        // distribution (JAR)
        throw new RuntimeException("Unable to load default stopword set");
      }
    }
  }

  /**
   * Contains the stopwords used with the {@link StopFilter}.
   */

  /**
   * Contains words that should be indexed but not stemmed.
   */
  private final CharArraySet exclusionSet;

  /**
   * Builds an analyzer with the default stop words: {@link #getDefaultStopSet()}.
   */
  public GermanAnalyzer() {
    this(DefaultSetHolder.DEFAULT_SET);
  }

  /**
   * Builds an analyzer with the given stop words
   *
   * @param stopwords a stopword set
   */
  public GermanAnalyzer(final CharArraySet stopwords) {
    this(stopwords, CharArraySet.EMPTY_SET);
  }

  /**
   * Builds an analyzer with the given stop words
   *
   * @param stopwords a stopword set
   * @param stemExclusionSet a stemming exclusion set
   */
  public GermanAnalyzer(final CharArraySet stopwords, final CharArraySet stemExclusionSet) {
    super(stopwords);
    exclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
  }

  /**
   * Creates {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} used to tokenize all
   * the text in the provided {@link Reader}.
   *
   * @return
   */
  @Override
  protected TokenStreamComponents createComponents(final String fieldName) {
    final Tokenizer source = new StandardTokenizer();
    TokenStream result = new StandardFilter(source);

    // Remove terms that do not contain any alphabetic character
    result = new NumberFilter(result);

    // Converting to lower case is not necessary as this is done before stemming
    // result = new LowerCaseFilter(result);

    // Remove stopwords
    result = new StopFilter(result, stopwords);

    // Mark keywords
    if (!exclusionSet.isEmpty()) {
      result = new SetKeywordMarkerFilter(result, exclusionSet);
    }

    // Normalize German special characters
    result = new KeywordAwareGermanNormalizationFilter(result);

    // Stem
    result = new SnowballFilter(result, new German2Stemmer());
    // Alternatives to the SnowballFilter:
    // result = new GermanStemFilter(result);
    // result = new GermanLightStemFilter(result);

    return new TokenStreamComponents(source, result);
  }


}
