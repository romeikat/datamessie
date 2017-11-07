package com.romeikat.datamessie.core.processing.service.fulltext.query;

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

import java.util.List;
import edu.stanford.nlp.util.StringUtils;

public class OutOfQuery implements FullTextQuery {

  private final int k;

  private final List<String> queryTerms;

  public OutOfQuery(final int k, final List<String> queryTerms) {
    this.k = k;
    this.queryTerms = queryTerms;
  }

  public int getK() {
    return k;
  }

  public List<String> getQueryTerms() {
    return queryTerms;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("OutOfQuery: ");
    stringBuilder.append("k = ");
    stringBuilder.append(k);
    stringBuilder.append(", queryTerms = ");
    stringBuilder.append(StringUtils.join(queryTerms, " "));
    return stringBuilder.toString();
  }

}
