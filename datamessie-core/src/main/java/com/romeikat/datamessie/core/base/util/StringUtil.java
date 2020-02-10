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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class StringUtil {

  private static NumberFormat INTEGER_FORMAT;

  static {
    INTEGER_FORMAT = NumberFormat.getIntegerInstance();
  }

  public String formatAsInteger(final Object o) {
    return INTEGER_FORMAT.format(o);
  }

  public String removeLineSeparators(final String s) {
    if (s == null) {
      return null;
    }

    try {
      final StringBuilder sb = new StringBuilder();
      final InputStream is = IOUtils.toInputStream(s, StandardCharsets.UTF_8);
      final LineIterator it = IOUtils.lineIterator(is, StandardCharsets.UTF_8);
      while (it.hasNext()) {
        if (sb.length() > 0) {
          sb.append(" ");
        }
        sb.append(it.nextLine());
      }
      IOUtils.closeQuietly(is);
      return sb.toString();
    } catch (final IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public String getSingularOrPluralTerm(final String singularTerm, final int number) {
    if (StringUtils.isBlank(singularTerm)) {
      return singularTerm;
    }

    return number == 1 ? singularTerm : singularTerm + "s";
  }

  public String getSingularOrPluralTerm(final String singularTerm, final String pluralTerm,
      final int number) {
    return number == 1 ? singularTerm : pluralTerm;
  }

  public boolean containsIgnoreCase(final Collection<String> existingStrings,
      final String candidateString) {
    for (final String existingString : existingStrings) {
      if (StringUtils.equalsIgnoreCase(existingString, candidateString)) {
        return true;
      }
    }

    return false;
  }

  public String removeInvisibleChars(final String s) {
    if (s == null) {
      return null;
    }

    return s.replaceAll("[\\u200b-\\u200d\\u00AD]", "");
  }

}
