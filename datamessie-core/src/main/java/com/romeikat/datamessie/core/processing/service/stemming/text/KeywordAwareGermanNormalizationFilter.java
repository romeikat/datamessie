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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.util.StemmerUtil;

/**
 * Variant of the {@link GermanNormalizationFilter} that is aware of the {@link KeywordAttribute}.
 */
public final class KeywordAwareGermanNormalizationFilter extends TokenFilter {
  // FSM with 3 states:
  private static final int N = 0; /* ordinary state */
  private static final int V = 1; /* stops 'u' from entering umlaut state */
  private static final int U = 2; /* umlaut state, allows e-deletion */

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);

  public KeywordAwareGermanNormalizationFilter(final TokenStream input) {
    super(input);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      if (keywordAttr.isKeyword()) {
        return true;
      }

      int state = N;
      char buffer[] = termAtt.buffer();
      int length = termAtt.length();
      for (int i = 0; i < length; i++) {
        final char c = buffer[i];
        switch (c) {
          case 'a':
          case 'o':
            state = U;
            break;
          case 'u':
            state = (state == N) ? U : V;
            break;
          case 'e':
            if (state == U) {
              length = StemmerUtil.delete(buffer, i--, length);
            }
            state = V;
            break;
          case 'i':
          case 'q':
          case 'y':
            state = V;
            break;
          case '\u00e4':
            buffer[i] = 'a';
            state = V;
            break;
          case '\u00f6':
            buffer[i] = 'o';
            state = V;
            break;
          case '\u00fc':
            buffer[i] = 'u';
            state = V;
            break;
          case '\u00df':
            buffer[i++] = 's';
            buffer = termAtt.resizeBuffer(1 + length);
            if (i < length) {
              System.arraycopy(buffer, i, buffer, i + 1, (length - i));
            }
            buffer[i] = 's';
            length++;
            state = N;
            break;
          default:
            state = N;
        }
      }
      termAtt.setLength(length);
      return true;
    } else {
      return false;
    }
  }
}
