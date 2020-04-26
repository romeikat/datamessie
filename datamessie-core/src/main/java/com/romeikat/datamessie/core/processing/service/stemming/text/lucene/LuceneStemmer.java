package com.romeikat.datamessie.core.processing.service.stemming.text.lucene;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.util.DataMessieException;
import com.romeikat.datamessie.core.base.util.ParseUtil;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.processing.service.stemming.text.Stemmer;

/**
 * A stemming algorithm based on Lucene analyzers. Supports German and English text. Allows for
 * stopwords to be ignored when stemming.
 * 
 * @author Dr. Raphael Romeikat
 *
 */
public class LuceneStemmer implements Stemmer {

  private final ParseUtil parseUtil;

  private Analyzer analyzer;

  public LuceneStemmer(final Language language, final Collection<String> stopwords,
      final ApplicationContext ctx) {
    parseUtil = ctx.getBean(ParseUtil.class);

    // Stopwords
    final Set<String> singleStopwords = getAsSingleWords(stopwords);

    // Analyzer
    createAnalyzer(language, singleStopwords);
  }

  private Set<String> getAsSingleWords(final Collection<String> words) {
    final Set<String> singleWords = Sets.newHashSetWithExpectedSize(words.size());
    for (final String namedEntityName : words) {
      final String singleWord = NamedEntity.getAsSingleWord(namedEntityName);
      singleWords.add(singleWord);
    }
    return singleWords;
  }

  private void createAnalyzer(final Language language, final Set<String> singleStopwords) {
    if (language == Language.DE) {
      analyzer = new GermanAnalyzer(GermanAnalyzer.getDefaultStopSet(),
          CharArraySet.copy(singleStopwords));
    } else if (language == Language.EN) {
      analyzer = new EnglishAnalyzer(EnglishAnalyzer.getDefaultStopSet(),
          CharArraySet.copy(singleStopwords));
    } else {
      throw new DataMessieException("Language " + language.getName() + " is not supported");
    }
  }

  @Override
  public List<String> stem(final String text) {
    return parseUtil.parseTerms(text, analyzer, false);
  }

  @Override
  public void close() {
    if (analyzer != null) {
      analyzer.close();
    }
  }

}
