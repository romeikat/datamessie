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

import org.springframework.stereotype.Service;

@Service
public class XmlUtil {

  public String stripNonValidXMLCharacters(final String in) {
    if (in == null) {
      return null;
    }
    if (in.isEmpty()) {
      return in;
    }
    final StringBuffer out = new StringBuffer(); // Used to hold the output.
    char current; // Used to reference the current character.
    for (int i = 0; i < in.length(); i++) {
      current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not
      // happen.
      if (current == 0x9 || current == 0xA || current == 0xD || current >= 0x20 && current <= 0xD7FF
          || current >= 0xE000 && current <= 0xFFFD || current >= 0x10000 && current <= 0x10FFFF) {
        out.append(current);
      }
    }
    return out.toString();
  }

}
