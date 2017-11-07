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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import jersey.repackaged.com.google.common.collect.Sets;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FullTextResult {

  private final Map<Long, FullTextMatch> fullTextMatches;

  public FullTextResult() {
    fullTextMatches = new HashMap<Long, FullTextMatch>();
  }

  public synchronized void addFullTextMatch(final long id, final long luceneDocumentId) {
    addFullTextMatch(id, luceneDocumentId, new ArrayList<String>());
  }

  public synchronized void addFullTextMatch(final long id, final long luceneDocumentId,
      final List<String> matchingTerms) {
    // Check whether a match for that id already exists
    FullTextMatch fullTextMatch = fullTextMatches.get(id);
    // New match => create
    if (fullTextMatch == null) {
      fullTextMatch = new FullTextMatch(id, luceneDocumentId, matchingTerms);
      addFullTextMatch(fullTextMatch);
    }
    // Existing match => add matching terms
    else {
      fullTextMatch.addNewMatchingTerms(matchingTerms);
    }
  }

  private void addFullTextMatch(final FullTextMatch fullTextMatch) {
    fullTextMatches.put(fullTextMatch.getId(), fullTextMatch);
  }

  public synchronized Set<Long> getIds() {
    final Set<Long> ids = Sets.newHashSet(fullTextMatches.keySet());
    return ids;
  }

  public synchronized int size() {
    final int size = fullTextMatches.size();
    return size;
  }

  public synchronized Set<Long> getIdsNotEmpty() {
    final Set<Long> ids = getIds();
    if (ids.isEmpty()) {
      ids.add(-1l);
    }
    return ids;
  }

  public Long getLuceneDocumentId(final long id) {
    final FullTextMatch fullTextMatch = fullTextMatches.get(id);
    if (fullTextMatch == null) {
      return null;
    }
    return fullTextMatch.getLuceneDocumentId();
  }

  public List<String> getMatchingTerms(final long id) {
    final FullTextMatch fullTextMatch = fullTextMatches.get(id);
    if (fullTextMatch == null) {
      return null;
    }
    return fullTextMatch.getMatchingTerms();
  }

}
