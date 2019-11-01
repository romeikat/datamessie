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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.model.enums.Language;

public class TextStemmerTest extends AbstractTest {

  @Autowired
  private TextStemmer textStemmer;

  @Test
  public void stemText_germanWithNamedEntityNames() {
    final String unstemmedText =
        "This text should really be stemmed. Numbers such as 44 will be removed. German special characters such as ? are replaced. Alright?";
    final Collection<String> namedEntityNames = Lists.newArrayList("this text");

    final String stemmedText = textStemmer.stemText(unstemmedText, namedEntityNames, Language.DE);

    assertTrue(stemmedText.contains("this_text"));
    assertFalse(stemmedText.contains("44"));
    assertFalse(stemmedText.contains("?"));
  }

  @Test
  public void stemText_englishWithNamedEntityNames() {
    final String unstemmedText =
        "This text should really be stemmed. Numbers such as 44 will be removed. English possesives such as the author's name are removed. Alright?";
    final Collection<String> namedEntityNames = Lists.newArrayList("this text");

    final String stemmedText = textStemmer.stemText(unstemmedText, namedEntityNames, Language.EN);

    assertTrue(stemmedText.contains("this_text"));
    assertFalse(stemmedText.contains("44"));
    assertTrue(stemmedText.contains("author"));
  }

  @Test
  public void stemmissingLanguage() {
    final String unstemmedText = "This text should not be stemmed.";
    final String stemmedText = textStemmer.stemText(unstemmedText, Collections.emptySet(), null);

    assertNull(stemmedText);
  }

}
