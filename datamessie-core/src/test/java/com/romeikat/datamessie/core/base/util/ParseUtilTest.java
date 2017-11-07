package com.romeikat.datamessie.core.base.util;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import org.apache.lucene.analysis.Analyzer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.ParseUtil;
import com.romeikat.datamessie.core.processing.service.fulltext.query.FullTextIndexingAnalyzer;

public class ParseUtilTest extends AbstractTest {

  @Autowired
  private ParseUtil parseUtil;

  private static final String WORD_1 = "first_word";

  private static final String WORD_2 = "second_word";

  private static final String WORD_3_PART_1 = "part1";

  private static final String WORD_3_PART_2 = "part2";

  private static final String WORD_3 = WORD_3_PART_1 + "-" + WORD_3_PART_2;

  private static Analyzer analyzer;

  @BeforeClass
  public static void initializeClass() {
    analyzer = new FullTextIndexingAnalyzer();
  }

  @Test
  public void findsWordsInBetween() throws Exception {
    final boolean matchFound =
        parseUtil.containsWordsInTheRightSequence(getTextWithAllWordsInBetween(),
            getWordVariants(WORD_1), getWordVariants(WORD_2), 2, analyzer);
    assertTrue(matchFound);
  }

  @Test
  public void findsWordPartsInBetween() throws Exception {
    final boolean matchFound =
        parseUtil.containsWordsInTheRightSequence(getTextWithAllWordsInBetween(),
            getWordVariants(WORD_3_PART_1), getWordVariants(WORD_3_PART_2), 2, analyzer);
    assertTrue(matchFound);
  }

  @Test
  public void findsWordsAtBorders() throws Exception {
    final boolean matchFound =
        parseUtil.containsWordsInTheRightSequence(getTextWithWords1And2AtBorders(),
            getWordVariants(WORD_1), getWordVariants(WORD_2), 2, analyzer);
    assertTrue(matchFound);
  }

  @Test
  public void ignoresWordsIfAboveDistance() throws Exception {
    final boolean matchFound =
        parseUtil.containsWordsInTheRightSequence(getTextWithAllWordsInBetween(),
            getWordVariants(WORD_1), getWordVariants(WORD_2), 1, analyzer);
    assertFalse(matchFound);
  }

  @Test
  public void ignoresBlankWord1() throws Exception {
    final boolean matchFound = parseUtil.containsWordsInTheRightSequence(
        getTextWithAllWordsInBetween(), null, getWordVariants(WORD_2), 2, analyzer);
    assertTrue(matchFound);
  }

  @Test
  public void ignoresBlankWord2() throws Exception {
    final boolean matchFound = parseUtil.containsWordsInTheRightSequence(
        getTextWithAllWordsInBetween(), getWordVariants(WORD_1), null, 2, analyzer);
    assertTrue(matchFound);
  }

  private String getTextWithAllWordsInBetween() {
    final StringBuilder text = new StringBuilder();
    text.append("bla bla ");
    text.append(WORD_1);
    text.append(" bla bla ");
    text.append(WORD_2);
    text.append(" bla bla ");
    text.append(WORD_3);
    text.append(" bla bla");
    return text.toString();
  }

  private String getTextWithWords1And2AtBorders() {
    final StringBuilder text = new StringBuilder();
    text.append(WORD_1);
    text.append(" bla bla ");
    text.append(WORD_2);
    return text.toString();
  }

  private Collection<String> getWordVariants(final String word) {
    return Sets.newHashSet(word);
  }

}
