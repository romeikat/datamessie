package com.romeikat.datamessie.core.processing.service.stemming.text.lucene;

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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

public class NumberFilter extends FilteringTokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public NumberFilter(final TokenStream in) {
    super(in);
  }

  @Override
  protected boolean accept() {
    return containsAtLeastOneAlphabeticCharacter(termAtt.buffer(), termAtt.length());
  }

  private boolean containsAtLeastOneAlphabeticCharacter(final char[] chars, final int length) {
    for (int i = 0; i < length; i++) {
      if (Character.isAlphabetic(chars[i])) {
        return true;
      }
    }
    return false;
  }

}
