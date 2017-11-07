package com.romeikat.datamessie.core.base.util.fullText;

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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;

public class FullTextMatch {

  private long id;

  private long luceneDocumentId;

  private List<String> matchingTerms;

  public FullTextMatch() {
    this(0, 0, Lists.<String>newLinkedList());
  }

  public FullTextMatch(final long id, final long luceneDocumentId,
      final List<String> matchingTerms) {
    this.id = id;
    this.luceneDocumentId = luceneDocumentId;
    this.matchingTerms = matchingTerms;
  }

  public void addNewMatchingTerms(final List<String> matchingTerms) {
    final List<String> newMatchingTerms = new ArrayList<String>(matchingTerms);
    newMatchingTerms.removeAll(this.matchingTerms);
    this.matchingTerms.addAll(newMatchingTerms);
  }

  public long getId() {
    return id;
  }

  public void setId(final long id) {
    this.id = id;
  }

  public long getLuceneDocumentId() {
    return luceneDocumentId;
  }

  public void setLuceneDocumentId(final long luceneDocumentId) {
    this.luceneDocumentId = luceneDocumentId;
  }

  public List<String> getMatchingTerms() {
    return matchingTerms;
  }

  public void setMatchingTerms(final List<String> matchingTerms) {
    this.matchingTerms = matchingTerms;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("id: " + id);
    sb.append("; ");
    sb.append("luceneDocumentId: " + luceneDocumentId);
    sb.append("; ");
    sb.append("matchingTerms: " + StringUtils.join(matchingTerms, ","));

    return sb.toString();
  }

}
