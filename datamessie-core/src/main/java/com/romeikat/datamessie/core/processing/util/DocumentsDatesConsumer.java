package com.romeikat.datamessie.core.processing.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class DocumentsDatesConsumer {

  private final SortedMap<LocalDate, Long> datesWithDocuments;
  private final int batchSize;

  public DocumentsDatesConsumer(final SortedMap<LocalDate, Long> datesWithDocuments,
      final int batchSize) {
    this.datesWithDocuments = datesWithDocuments;
    this.batchSize = batchSize;
  }

  public Pair<LocalDate, LocalDate> getNextDateRange() {
    if (datesWithDocuments.isEmpty()) {
      return null;
    }

    final LocalDate fromDate = datesWithDocuments.keySet().iterator().next();
    LocalDate toDate = fromDate;

    int currentBatchSize = 0;
    for (final Entry<LocalDate, Long> entry : datesWithDocuments.entrySet()) {
      final LocalDate date = entry.getKey();
      final long documents = ObjectUtils.defaultIfNull(entry.getValue(), 0l);

      toDate = date;
      currentBatchSize += documents;

      if (currentBatchSize > batchSize) {
        break;
      }
    }

    return new ImmutablePair<LocalDate, LocalDate>(fromDate, toDate);
  }

  public void removeDates(final LocalDate until) {
    for (final Iterator<Entry<LocalDate, Long>> it = datesWithDocuments.entrySet().iterator(); it
        .hasNext();) {
      final Entry<LocalDate, Long> entry = it.next();
      final LocalDate date = entry.getKey();
      if (date.isAfter(until)) {
        break;
      } else {
        it.remove();
      }
    }
  }

  public boolean isEmpty() {
    return datesWithDocuments.isEmpty();
  }

}
