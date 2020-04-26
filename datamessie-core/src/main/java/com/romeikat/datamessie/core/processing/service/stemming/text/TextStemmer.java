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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.util.TextUtil;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.processing.service.stemming.text.lucene.LuceneStemmer;

@Service
public class TextStemmer {

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private TextUtil textUtil;

  public String stemText(final String unstemmedText, final Collection<String> namedEntityNames,
      final Language language) {
    if (unstemmedText == null || language == null) {
      return null;
    }

    // Convert into lower case
    String textUnderStemming = unstemmedText.toLowerCase();

    // Replace named entities (bind named entities consisting of multiple words together)
    textUnderStemming = replaceNamedEntities(textUnderStemming, namedEntityNames);

    // Stem
    final Stemmer stemmer = createStemmer(language, namedEntityNames);
    final String stemmedText = doStemming(textUnderStemming, stemmer);
    stemmer.close();

    // Done
    return stemmedText;
  }

  private String replaceNamedEntities(String textUnderStemming,
      final Collection<String> namedEntityNames) {
    final List<String> namedEntityNamesOrderdByNumberOfWords =
        getNamedEntityOccurrencesOrderedByNumberOfWords(namedEntityNames);
    for (final String namedEntityName : namedEntityNamesOrderdByNumberOfWords) {
      textUnderStemming = replaceNamedEntity(textUnderStemming, namedEntityName);
    }
    return textUnderStemming;
  }

  private String replaceNamedEntity(final String textUnderStemming, final String namedEntityName) {
    // Determine replacement
    final Pair<String, String> replacingAndReplacement =
        getReplacingAndReplacement(namedEntityName);
    if (replacingAndReplacement == null) {
      return textUnderStemming;
    }

    final String replacing = replacingAndReplacement.getLeft();
    final String replacement = replacingAndReplacement.getRight();

    // Do the replacement (to whole words only)
    final String result = textUtil.replaceAllAsWholeWord(textUnderStemming, replacing, replacement);

    // Done
    return result;
  }

  private Pair<String, String> getReplacingAndReplacement(final String namedEntityName) {
    final String replacing = namedEntityName;
    final String replacement = NamedEntity.getAsSingleWord(namedEntityName);
    // Return the replacement, if different
    if (replacement.equals(replacing)) {
      return null;
    }
    return new ImmutablePair<String, String>(replacing, replacement);
  }

  private List<String> getNamedEntityOccurrencesOrderedByNumberOfWords(
      final Collection<String> namedEntityNames) {
    // Comparator
    final Comparator<String> numberOfWordsComparator = new Comparator<String>() {
      @Override
      public int compare(final String namedEntityName1, final String namedEntityName) {
        final int numberOfWords1 = NamedEntity.getNumberOfWords(namedEntityName1);
        final int numberOfWords2 = NamedEntity.getNumberOfWords(namedEntityName);
        return numberOfWords2 - numberOfWords1;
      }
    };
    // Sort
    final List<String> namedEntityNamesOrderdByNumberOfWords = Lists.newArrayList(namedEntityNames);
    Collections.sort(namedEntityNamesOrderdByNumberOfWords, numberOfWordsComparator);
    // Done
    return namedEntityNamesOrderdByNumberOfWords;
  }

  /**
   * Decides which stemming algorithm to be used. Currently uses a Lucene-based implementation that
   * uses respective {@link org.apache.lucene.analysis.Analyzer Analyzers}. One alterantve would be
   * the <a href="https://github.com/LeonieWeissweiler/CISTEM/blob/master/Cistem.java">CISTEM</a>
   * algorithm. algorithm.
   *
   * @param language
   * @param namedEntityNames
   * @return
   */
  private Stemmer createStemmer(final Language language,
      final Collection<String> namedEntityNames) {
    return new LuceneStemmer(language, namedEntityNames, ctx);
  }

  private String doStemming(final String unstemmedText, final Stemmer stemmer) {
    // Process
    final List<String> terms = stemmer.stem(unstemmedText);
    final String processedText = StringUtils.join(terms, " ");

    // Done
    return processedText;
  }

}
