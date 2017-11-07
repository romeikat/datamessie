package com.romeikat.datamessie.core.statistics.task;

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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import org.apache.commons.lang3.BooleanUtils;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.util.comparator.DescendingComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.Cell;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;


public class StatisticsToBeRebuilt {

  private final Queue<LocalDate> publishedDatesToBeRebuilt;
  private final Queue<Long> sourcesToBeRebuilt;
  private final StatisticsRebuildingSparseTable sourcesAndPublishedTatesToBeRebuilt;

  StatisticsToBeRebuilt() {
    publishedDatesToBeRebuilt = Lists.newLinkedList();
    sourcesToBeRebuilt = Lists.newLinkedList();
    sourcesAndPublishedTatesToBeRebuilt = new StatisticsRebuildingSparseTable();
  }

  synchronized void toBeRebuilt(final Long sourceId) {
    if (sourceId == null) {
      return;
    }

    sourcesToBeRebuilt.add(sourceId);
  }

  synchronized void toBeRebuilt(final LocalDate published) {
    if (published == null) {
      return;
    }

    publishedDatesToBeRebuilt.add(published);
  }

  synchronized void toBeRebuilt(final Long sourceId, final LocalDate published) {
    if (sourceId == null && published == null) {
      return;
    }
    // Published date
    else if (sourceId == null) {
      toBeRebuilt(published);
    }
    // Source
    else if (published == null) {
      toBeRebuilt(sourceId);
    }
    // Source and published date
    else {
      sourcesAndPublishedTatesToBeRebuilt.putValue(sourceId, published, true);
    }
  }

  synchronized void toBeRebuilt(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    final Collection<Cell<Long, LocalDate, Boolean>> cells = statisticsToBeRebuilt.getCells();
    for (final Cell<Long, LocalDate, Boolean> cell : cells) {
      final Boolean toBeRebuilt = cell.getValue();
      if (BooleanUtils.isNotTrue(toBeRebuilt)) {
        continue;
      }

      final Long sourceId = cell.getRowHeader();
      final LocalDate publishedDate = cell.getColumnHeader();
      toBeRebuilt(sourceId, publishedDate);
    }
  }

  synchronized SourceAndPublished poll() {
    // Prio 1: published date
    LocalDate publishedDate = publishedDatesToBeRebuilt.poll();
    if (publishedDate != null) {
      sourcesAndPublishedTatesToBeRebuilt.removeColumn(publishedDate);
      return new SourceAndPublished(null, publishedDate);
    }

    // Prio 2: source
    final Long sourceId = sourcesToBeRebuilt.poll();
    if (sourceId != null) {
      sourcesAndPublishedTatesToBeRebuilt.removeRow(sourceId);
      return new SourceAndPublished(sourceId, null);
    }

    // Prio 3: source and published date
    // In this case, use highest published date for all sources
    final List<LocalDate> publishedDates = sourcesAndPublishedTatesToBeRebuilt
        .getColumnHeadersSorted(new DescendingComparator<LocalDate>());
    final Iterator<LocalDate> publishedDatesIt = publishedDates.iterator();
    if (!publishedDatesIt.hasNext()) {
      return null;
    }
    publishedDate = publishedDatesIt.next();
    sourcesAndPublishedTatesToBeRebuilt.removeColumn(publishedDate);
    return new SourceAndPublished(null, publishedDate);
  }

}
