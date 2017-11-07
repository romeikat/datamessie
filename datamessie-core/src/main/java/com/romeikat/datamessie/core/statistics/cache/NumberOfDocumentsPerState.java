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
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class NumberOfDocumentsPerState {

  private final Map<DocumentProcessingState, Long> numberOfDocumentsPerState;

  public NumberOfDocumentsPerState() {
    numberOfDocumentsPerState =
        Maps.newHashMapWithExpectedSize(DocumentProcessingState.values().length);
  }

  public long get(final DocumentProcessingState... states) {
    long numberOfDocumentsForStates = 0l;
    synchronized (numberOfDocumentsPerState) {
      for (final DocumentProcessingState state : states) {
        final Long numberOfDocumentsForState = numberOfDocumentsPerState.get(state);
        if (numberOfDocumentsForState != null) {
          numberOfDocumentsForStates += numberOfDocumentsForState;
        }
      }
    }
    return numberOfDocumentsForStates;
  }

  public long getAll() {
    return get(DocumentProcessingState.values());
  }

  public void increase(final DocumentProcessingState state) {
    synchronized (numberOfDocumentsPerState) {
      Long numberOfDocumentsForState = numberOfDocumentsPerState.get(state);
      if (numberOfDocumentsForState == null) {
        numberOfDocumentsForState = 0l;
      }
      numberOfDocumentsForState++;
      numberOfDocumentsPerState.put(state, numberOfDocumentsForState);
    }
  }

  public void decrease(final DocumentProcessingState state) {
    synchronized (numberOfDocumentsPerState) {
      Long numberOfDocumentsForState = numberOfDocumentsPerState.get(state);
      if (numberOfDocumentsForState == null) {
        numberOfDocumentsForState = 0l;
      }
      numberOfDocumentsForState--;
      numberOfDocumentsPerState.put(state, numberOfDocumentsForState);
    }
  }

}
