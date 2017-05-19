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
public class HtmlUtil {

  public String addProtocolIfNecessary(final String url) {
    if (url == null) {
      return null;
    }

    String result = url.trim();
    final String protocol1 = "http://";
    final String protocol2 = "https://";
    if (!result.startsWith(protocol1) && !result.startsWith(protocol2)) {
      result = protocol1 + result;
    }
    return result;
  }

  public String removeTags(final String html) {
    if (html == null) {
      return null;
    }

    final String regex = "<[^>]*>";
    final String result = html.replaceAll(regex, "");
    return result;
  }

}
