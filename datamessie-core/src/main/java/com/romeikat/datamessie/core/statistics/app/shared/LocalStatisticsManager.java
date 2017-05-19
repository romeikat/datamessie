package com.romeikat.datamessie.core.statistics.app.shared;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.StatisticsDao;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsSparseTable;
import com.romeikat.datamessie.core.domain.dto.StatisticsDto;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;
import com.romeikat.datamessie.core.statistics.cache.GetNumberOfDocumentsFunction;
import com.romeikat.datamessie.core.statistics.service.StatisticsService;
import com.romeikat.datamessie.core.statistics.task.StatisticsRebuilder;

@Service
public class LocalStatisticsManager implements IStatisticsManager {

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private StatisticsDao statisticsDao;

  @Autowired
  private StatisticsRebuilder statisticsRebuilder;

  @Autowired
  private StatisticsService statisticsService;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public void rebuildStatistics(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    statisticsRebuilder.toBeRebuilt(statisticsToBeRebuilt);
  }

  @Override
  public void rebuildStatistics(final Long sourceId, final LocalDate published) {
    statisticsRebuilder.toBeRebuilt(sourceId, published);
  }

  @Override
  public StatisticsDto getStatistics(final long projectId, final Integer numberOfDays) {
    final StatisticsDto dto = new StatisticsDto();

    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final Collection<Long> sourceIds = sourceDao.getIds(sessionProvider.getStatelessSession(), projectId, null);

    LocalDate to;
    LocalDate from;
    to = LocalDate.now();
    from = to.minusDays(ObjectUtils.defaultIfNull(numberOfDays, 0) - 1);

    final Function<LocalDate, LocalDate> transformDateFunction = Functions.identity();

    final StatisticsSparseTable baseStatistics =
        getBaseStatistics(sessionProvider.getStatelessSession(), sourceIds, from, to);


    // All documents
    DocumentProcessingState[] states = DocumentProcessingState.getWithout(DocumentProcessingState.TECHNICAL_ERROR,
        DocumentProcessingState.TO_BE_DELETED);
    GetNumberOfDocumentsFunction getNumberOfDocumentsFunction = new GetNumberOfDocumentsFunction(states);
    final SparseSingleTable<Long, LocalDate, Long> allDocumentsStatistics = statisticsService
        .getStatistics(baseStatistics, sourceIds, from, to, transformDateFunction, getNumberOfDocumentsFunction);
    final Function<Pair<Long, Long>, Long> mergeFunction = new Function<Pair<Long, Long>, Long>() {
      @Override
      public Long apply(final Pair<Long, Long> from) {
        return from.getLeft() + from.getRight();
      }
    };
    final Long allDocuments = allDocumentsStatistics.mergeAllValues(mergeFunction);
    dto.setAllDocuments(allDocuments);

    // Downloaded documents
    states = DocumentProcessingState.getWithout(DocumentProcessingState.DOWNLOAD_ERROR,
        DocumentProcessingState.REDIRECTING_ERROR, DocumentProcessingState.TECHNICAL_ERROR);
    getNumberOfDocumentsFunction = new GetNumberOfDocumentsFunction(states);
    final SparseSingleTable<Long, LocalDate, Long> downloadedDocumentsStatistics = statisticsService
        .getStatistics(baseStatistics, sourceIds, from, to, transformDateFunction, getNumberOfDocumentsFunction);
    final Long downloadedDocuments = downloadedDocumentsStatistics.mergeAllValues(mergeFunction);
    dto.setDownloadedDocuments(downloadedDocuments);

    // Preprocessed documents
    getNumberOfDocumentsFunction =
        new GetNumberOfDocumentsFunction(DocumentProcessingState.getWith(DocumentProcessingState.STEMMED));
    final SparseSingleTable<Long, LocalDate, Long> preprocessedDocumentsStatistics = statisticsService
        .getStatistics(baseStatistics, sourceIds, from, to, transformDateFunction, getNumberOfDocumentsFunction);
    final Long preprocessedDocuments = preprocessedDocumentsStatistics.mergeAllValues(mergeFunction);
    dto.setPreprocessedDocuments(preprocessedDocuments);

    // Download success
    final Double downloadSuccess;
    if (downloadedDocuments == null || allDocuments == null || allDocuments == 0) {
      downloadSuccess = null;
    } else {
      downloadSuccess = (double) downloadedDocuments / (double) allDocuments;
    }
    dto.setDownloadSuccess(downloadSuccess);

    // Preprocessing success
    getNumberOfDocumentsFunction =
        new GetNumberOfDocumentsFunction(DocumentProcessingState.getWith(DocumentProcessingState.CLEANED,
            DocumentProcessingState.CLEANING_ERROR, DocumentProcessingState.STEMMED));
    final SparseSingleTable<Long, LocalDate, Long> preprocessingAttemtedDocumentsStatistics = statisticsService
        .getStatistics(baseStatistics, sourceIds, from, to, transformDateFunction, getNumberOfDocumentsFunction);
    final Long preprocessingAttemtedDocuments = preprocessingAttemtedDocumentsStatistics.mergeAllValues(mergeFunction);

    final Double preprocessingSuccess;
    if (preprocessedDocuments == null || preprocessingAttemtedDocuments == null
        || preprocessingAttemtedDocuments == 0) {
      preprocessingSuccess = null;
    } else {
      preprocessingSuccess = (double) preprocessedDocuments / (double) preprocessingAttemtedDocuments;
    }
    dto.setPreprocessingSuccess(preprocessingSuccess);

    // Documents to be preprocessed
    getNumberOfDocumentsFunction =
        new GetNumberOfDocumentsFunction(DocumentProcessingState.getWith(DocumentProcessingState.DOWNLOADED,
            DocumentProcessingState.REDIRECTED, DocumentProcessingState.CLEANED));
    final SparseSingleTable<Long, LocalDate, Long> documentsToBePreprocessedStatistics = statisticsService
        .getStatistics(baseStatistics, sourceIds, from, to, transformDateFunction, getNumberOfDocumentsFunction);
    final Long documentsToBePreprocessed = documentsToBePreprocessedStatistics.mergeAllValues(mergeFunction);
    dto.setDocumentsToBePreprocessed(documentsToBePreprocessed);

    // Done
    sessionProvider.closeStatelessSession();
    return dto;
  }

  @Override
  public <T> SparseSingleTable<Long, LocalDate, T> getStatistics(final Collection<Long> sourceIds, final LocalDate from,
      final LocalDate to, final Function<LocalDate, LocalDate> transformDateFunction,
      final Function<DocumentsPerState, T> transformValueFunction) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final StatisticsSparseTable baseStatistics =
        getBaseStatistics(sessionProvider.getStatelessSession(), sourceIds, from, to);
    sessionProvider.closeStatelessSession();

    final SparseSingleTable<Long, LocalDate, T> statistics = statisticsService.getStatistics(baseStatistics, sourceIds,
        from, to, transformDateFunction, transformValueFunction);
    return statistics;
  }

  private StatisticsSparseTable getBaseStatistics(final SharedSessionContract ssc, final Collection<Long> sourceIds,
      final LocalDate from, final LocalDate to) {
    final StatisticsSparseTable statistics = new StatisticsSparseTable();

    if (CollectionUtils.isEmpty(sourceIds) || from == null || to == null) {
      return statistics;
    }

    final List<LocalDate> publishedDates = DateUtil.getLocalDatesBetween(from, to);
    for (final LocalDate publishedDate : publishedDates) {
      final StatisticsSparseTable statisticsForPublishedDate =
          statisticsDao.getStatistics(ssc, sourceIds, publishedDate);
      statistics.putValues(statisticsForPublishedDate);
    }
    return statistics;
  }

}
