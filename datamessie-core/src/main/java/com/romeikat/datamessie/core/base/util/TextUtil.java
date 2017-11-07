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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextUtil {

  private static final Logger LOG = LoggerFactory.getLogger(TextUtil.class);

  private final String wordSplitPattern = "\\b(?:[A-Za-z0-9_-]+)\\b";

  public List<String> tokenizeStemmedText(final String stemmedText) {
    if (stemmedText == null) {
      return Collections.emptyList();
    }
    final String[] tokens = stemmedText.split(" ");
    return Arrays.asList(tokens);
  }

  public List<String> extractTerms(final String text) {
    final List<String> words = new LinkedList<String>();
    if (text != null) {
      final Pattern p = Pattern.compile(wordSplitPattern);
      final Matcher m = p.matcher(text);
      while (m.find()) {
        final String word = m.group();
        words.add(word);
      }
    }
    return words;
  }

  public String replaceAllAsWholeWord(final String text, final String replacing,
      final String replacement) {
    if (replacing == null || replacement == null) {
      return text;
    }

    final String regex = "(?i)(?<!\\w(?=\\w))(" + Pattern.quote(replacing) + ")(?!(?<=\\w)\\w)";
    // See
    // http://stackoverflow.com/questions/28285518/regex-word-boundary-pattern-quote-and-parentheses

    try {
      final String result = text.replaceAll(regex, replacement);
      return result;
    } catch (final Exception e) {
      final String msg =
          String.format("Could not replace %s by %s in %s", replacing, replacement, text);
      LOG.error(msg, e);
      return text;
    }
  }

}
