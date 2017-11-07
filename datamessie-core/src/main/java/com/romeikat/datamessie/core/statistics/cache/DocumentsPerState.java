package com.romeikat.datamessie.core.statistics.cache;

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

import java.util.Map;
import java.util.Set;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import jersey.repackaged.com.google.common.collect.Maps;

public class DocumentsPerState {

  private final Map<DocumentProcessingState, Long> documentsPerState;

  public DocumentsPerState() {
    documentsPerState = Maps.newHashMap();
  }

  public synchronized long get(final DocumentProcessingState... states) {
    long documentsForStates = 0;
    for (final DocumentProcessingState state : states) {
      final Long documentsForState = documentsPerState.get(state);
      if (documentsForState != null) {
        documentsForStates += documentsForState;
      }
    }
    return documentsForStates;
  }

  public synchronized long getAll() {
    return get(DocumentProcessingState.values());
  }

  public synchronized Set<DocumentProcessingState> getStates() {
    return documentsPerState.keySet();
  }

  public synchronized void put(final DocumentProcessingState state, final Long documents) {
    if (state == null) {
      return;
    }
    documentsPerState.put(state, documents);
  }

  public synchronized void remove(final DocumentProcessingState state) {
    if (state == null) {
      return;
    }
    documentsPerState.remove(state);
  }

  public synchronized void add(final DocumentProcessingState state, final Long documents) {
    if (state == null || documents == null) {
      return;
    }
    final long documentsOld = get(state);
    final long documentsNew = documentsOld + documents;
    put(state, documentsNew);
  }

  public synchronized void addAll(final DocumentsPerState other) {
    for (final DocumentProcessingState state : DocumentProcessingState.values()) {
      final Long otherDocuments = other.get(state);
      add(state, otherDocuments);
    }
  }

}
