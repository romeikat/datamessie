package com.romeikat.datamessie.core.statistics.service;

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
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.impl.StatisticsDao;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.sparsetable.ITableExtractor;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsSparseTable;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;
import com.romeikat.datamessie.core.statistics.cache.MergeNumberOfDocumentsFunction;
import com.romeikat.datamessie.core.statistics.dao.DocumentDao;
import com.romeikat.datamessie.core.statistics.dto.DocumentStatisticsDto;

@Service
public class StatisticsService {

  @Autowired
  private StatisticsDao statisticsDao;

  @Autowired
  @Qualifier("statisticsDocumentDao")
  private DocumentDao documentDao;

  private StatisticsService() {}

  public void rebuildStatistics(final StatelessSession statelessSession, final Long sourceId,
      final LocalDate published) {
    if (sourceId == null && published == null) {
      return;
    }

    // Delete old statistics
    statisticsDao.deleteStatistics(statelessSession, sourceId, published);

    // Calculate new statistics
    final StatisticsSparseTable statistics =
        calculateStatistics(statelessSession, sourceId, published);

    // Save new statistics
    statisticsDao.saveStatistics(statelessSession, statistics);
  }

  private StatisticsSparseTable calculateStatistics(final SharedSessionContract ssc,
      final Long sourceId, final LocalDate published) {
    final StatisticsSparseTable statistics = new StatisticsSparseTable();

    final List<DocumentStatisticsDto> dtos =
        documentDao.getAsDocumentStatisticsDtos(ssc, sourceId, published);
    for (final DocumentStatisticsDto dto : dtos) {
      final DocumentsPerState documentForState = new DocumentsPerState();
      documentForState.put(dto.getState(), 1l);
      statistics.putValue(dto.getSourceId(), dto.getPublishedDate(), documentForState);
    }

    return statistics;
  }

  public <T> SparseSingleTable<Long, LocalDate, T> getStatistics(
      final StatisticsSparseTable baseStatistics, final Collection<Long> sourceIds,
      final LocalDate from, final LocalDate to,
      final Function<LocalDate, LocalDate> transformDateFunction,
      final Function<DocumentsPerState, T> transformValueFunction) {
    final MergeNumberOfDocumentsFunction mergeNumberOfDocumentsFunction =
        new MergeNumberOfDocumentsFunction();
    final ITableExtractor<Long, LocalDate, DocumentsPerState, T> tableExtractor =
        new ITableExtractor<Long, LocalDate, DocumentsPerState, T>() {

          @Override
          public Long getExtractedRowHeader(final Long sourceId) {
            if (sourceIds.contains(sourceId)) {
              return sourceId;
            }
            return null;
          }

          @Override
          public LocalDate getExtractedColumnHeader(final LocalDate publishedDate) {
            if (from != null && publishedDate.isBefore(from)) {
              return null;
            }
            if (to != null && publishedDate.isAfter(to)) {
              return null;
            }

            final LocalDate transformedPublishedDate = transformDateFunction.apply(publishedDate);
            return transformedPublishedDate;
          }

          @Override
          public DocumentsPerState mergeValues(final DocumentsPerState documentsPerState1,
              final DocumentsPerState documentsPerState2) {
            return mergeNumberOfDocumentsFunction
                .apply(new ImmutablePair<DocumentsPerState, DocumentsPerState>(documentsPerState1,
                    documentsPerState2));
          }

          @Override
          public T getExtractedValue(final DocumentsPerState documentsPerState) {
            return transformValueFunction.apply(documentsPerState);
          }

        };

    final SparseSingleTable<Long, LocalDate, T> extractedStatistics =
        baseStatistics.extract(tableExtractor);

    // extractedStatistics only contains row headers for source IDs that were reported within the
    // time period; in order to cover all source IDs, we add all (remaining) source IDs
    extractedStatistics.addRowHeaders(sourceIds);

    // extractedStatistics only contains column headers for dates that were reported within the time
    // period; in order to cover all dates, we add all (remaining) dates
    final List<LocalDate> publishedDates = DateUtil.getLocalDatesBetween(from, to);
    final List<LocalDate> transformedPublishedDates =
        Lists.transform(publishedDates, transformDateFunction);
    extractedStatistics.addColumnHeaders(transformedPublishedDates);

    return extractedStatistics;
  }

}
