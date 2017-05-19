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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.hibernate.SharedSessionContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.util.DataMessieException;
import com.romeikat.datamessie.core.base.util.ParseUtil;
import com.romeikat.datamessie.core.base.util.TextUtil;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.enums.Language;

@Service
public class TextStemmer {

  @Autowired
  private ParseUtil parseUtil;

  @Autowired
  private TextUtil textUtil;

  public String stemText(final SharedSessionContract ssc, final String unstemmedText,
      final Collection<String> namedEntityNames, final Language language) {
    if (unstemmedText == null || language == null) {
      return null;
    }

    // Convert into lower case
    String textUnderStemming = unstemmedText.toLowerCase();

    // Replace named entities (bind named entities consisting of multiple words together)
    textUnderStemming = replaceNamedEntities(ssc, textUnderStemming, namedEntityNames);

    // Stem
    final Analyzer analyzer = getAnalyzer(language, namedEntityNames);
    final String stemmedText = doStemming(textUnderStemming, analyzer);
    analyzer.close();

    // Done
    return stemmedText;
  }

  private String replaceNamedEntities(final SharedSessionContract ssc, String textUnderStemming,
      final Collection<String> namedEntityNames) {
    final List<String> namedEntityNamesOrderdByNumberOfWords =
        getNamedEntityOccurrencesOrderedByNumberOfWords(namedEntityNames);
    for (final String namedEntityName : namedEntityNamesOrderdByNumberOfWords) {
      textUnderStemming = replaceNamedEntity(ssc, textUnderStemming, namedEntityName);
    }
    return textUnderStemming;
  }

  private String replaceNamedEntity(final SharedSessionContract ssc, final String textUnderStemming,
      final String namedEntityName) {
    // Determine replacement
    final Pair<String, String> replacingAndReplacement = getReplacingAndReplacement(ssc, namedEntityName);
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

  private Pair<String, String> getReplacingAndReplacement(final SharedSessionContract ssc,
      final String namedEntityName) {
    final String replacing = namedEntityName;
    final String replacement = NamedEntity.getAsSingleWord(namedEntityName);
    // Return the replacement, if different
    if (replacement.equals(replacing)) {
      return null;
    }
    return new ImmutablePair<String, String>(replacing, replacement);
  }

  private List<String> getNamedEntityOccurrencesOrderedByNumberOfWords(final Collection<String> namedEntityNames) {
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

  private String doStemming(final String unstemmedText, final Analyzer analyzer) {
    // Process
    final List<String> terms = parseUtil.parseTerms(unstemmedText, analyzer, false);
    final String processedText = StringUtils.join(terms, " ");

    // Done
    return processedText;
  }

  private Analyzer getAnalyzer(final Language language, final Collection<String> namedEntityNames) {
    final Set<String> singleWords = getAsSingleWords(namedEntityNames);

    final Analyzer analyzer;
    if (language == Language.DE) {
      analyzer = new GermanAnalyzer(GermanAnalyzer.getDefaultStopSet(), CharArraySet.copy(singleWords));
    } else if (language == Language.EN) {
      analyzer = new EnglishAnalyzer(EnglishAnalyzer.getDefaultStopSet(), CharArraySet.copy(singleWords));
    } else {
      throw new DataMessieException("");
    }

    return analyzer;
  }

  private Set<String> getAsSingleWords(final Collection<String> namedEntityNames) {
    final Set<String> singleWords = Sets.newHashSetWithExpectedSize(namedEntityNames.size());
    for (final String namedEntityName : namedEntityNames) {
      final String singleWord = NamedEntity.getAsSingleWord(namedEntityName);
      singleWords.add(singleWord);
    }
    return singleWords;
  }

}
