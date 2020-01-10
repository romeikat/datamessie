package com.romeikat.datamessie.core.base.service;

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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingTask;
import com.romeikat.datamessie.core.processing.util.DocumentsDatesConsumer;
import jersey.repackaged.com.google.common.collect.Lists;

@Service
public class DocumentService {

  private final static Logger LOG = LoggerFactory.getLogger(DocumentService.class);

  @Value("${documents.processing.batch.size}")
  private int batchSize;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;

  @Autowired
  @Qualifier("crawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private StringUtil stringUtil;

  public void deprocessDocumentsOfSource(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState) throws TaskCancelledException {
    // Initialize
    final TaskExecutionWork work = taskExecution.reportWorkStart(String.format(
        "Deprocessing documents of source %s to state %s", sourceId, targetState.getName()));
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        new StatisticsRebuildingSparseTable();

    // Determine necessary states and sources
    final Collection<DocumentProcessingState> statesForDeprocessing =
        getStatesForDeprocessing(targetState);
    final List<Long> sourceIds = Lists.newArrayList(sourceId);

    // Determine downloaded dates
    final SortedMap<LocalDate, Long> datesWithDocuments =
        documentDao.getDownloadedDatesWithNumberOfDocuments(statelessSession, null,
            statesForDeprocessing, sourceIds);
    final DocumentsDatesConsumer documentsDatesConsumer =
        new DocumentsDatesConsumer(datesWithDocuments, batchSize);
    if (documentsDatesConsumer.isEmpty()) {
      taskExecution.reportWorkEnd(work);
      return;
    }

    // Initialize first date range
    MutablePair<LocalDate, LocalDate> downloadedDateRange = new MutablePair<LocalDate, LocalDate>();
    final Pair<LocalDate, LocalDate> firstDateRange = documentsDatesConsumer.getNextDateRange();
    downloadedDateRange.setLeft(firstDateRange.getLeft());
    downloadedDateRange.setRight(firstDateRange.getRight());

    // Process date ranges
    while (downloadedDateRange != null) {
      // Deprocess
      final int deprocessedDocuments = deprocessDocuments(statelessSession, taskExecution, sourceId,
          targetState, downloadedDateRange.getLeft(), downloadedDateRange.getRight(),
          statesForDeprocessing, statisticsToBeRebuilt);

      // Prepare for next iteration
      final boolean noMoreDocumentsToDeprocess = deprocessedDocuments < batchSize;
      if (noMoreDocumentsToDeprocess) {
        documentsDatesConsumer.removeDates(downloadedDateRange.getRight());
        if (documentsDatesConsumer.isEmpty()) {
          downloadedDateRange = null;
        } else {
          final Pair<LocalDate, LocalDate> nextDateRange =
              documentsDatesConsumer.getNextDateRange();
          downloadedDateRange.setLeft(nextDateRange.getLeft());
          downloadedDateRange.setRight(nextDateRange.getRight());
        }
      }
    }

    // Notify DocumentsProcessingTask to restart processing
    final Collection<DocumentsProcessingTask> activeTasks =
        taskManager.getActiveTasks(DocumentsProcessingTask.NAME, DocumentsProcessingTask.class);
    for (final DocumentsProcessingTask activeTask : activeTasks) {
      activeTask.restartProcessing();
    }

    // Rebuild statistics
    final IStatisticsManager statisticsManager =
        sharedBeanProvider.getSharedBean(IStatisticsManager.class);
    if (statisticsManager != null) {
      statisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    }

    // Done
    taskExecution.reportWorkEnd(work);
  }

  private int deprocessDocuments(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState, final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> statesForDeprocessing,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt) throws TaskCancelledException {
    // Load
    TaskExecutionWork work = taskExecution.reportWorkStart(
        String.format("Loading documents to deprocess from download dates %s to %s",
            LocalDateConverter.INSTANCE_UI.convertToString(fromDate),
            LocalDateConverter.INSTANCE_UI.convertToString(toDate)));
    final List<Document> documentsToDeprocess = documentDao.getForSourceAndDownloaded(
        statelessSession, sourceId, fromDate, toDate, statesForDeprocessing, batchSize);
    taskExecution.reportWorkEnd(work);

    // Deprocess
    if (CollectionUtils.isNotEmpty(documentsToDeprocess)) {
      final String singularPlural =
          stringUtil.getSingularOrPluralTerm("document", documentsToDeprocess.size());
      work = taskExecution.reportWorkStart(
          String.format("Deprocessing %s %s", documentsToDeprocess.size(), singularPlural));
      deprocessDocuments(statelessSession, documentsToDeprocess, targetState,
          statisticsToBeRebuilt);
      taskExecution.reportWorkEnd(work);
    }

    taskExecution.checkpoint();

    return documentsToDeprocess.size();
  }

  private void deprocessDocuments(final StatelessSession statelessSession,
      final Collection<Document> documents, final DocumentProcessingState targetState,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    for (final Document document : documents) {
      // Update state
      document.setState(targetState);
      documentDao.update(statelessSession, document);
      // Rebuild statistics
      final long sourceId = document.getSourceId();
      final LocalDate publishedDate = document.getPublishedDate();
      statisticsToBeRebuilt.putValue(sourceId, publishedDate, true);
    }
  }

  private Set<DocumentProcessingState> getStatesForDeprocessing(
      final DocumentProcessingState targetState) {
    switch (targetState) {
      case DOWNLOADED:
        return Sets.newHashSet(DocumentProcessingState.REDIRECTED,
            DocumentProcessingState.REDIRECTING_ERROR, DocumentProcessingState.CLEANED,
            DocumentProcessingState.CLEANING_ERROR, DocumentProcessingState.STEMMED,
            DocumentProcessingState.TECHNICAL_ERROR);
      case REDIRECTED:
        return Sets.newHashSet(DocumentProcessingState.REDIRECTING_ERROR,
            DocumentProcessingState.CLEANED, DocumentProcessingState.CLEANING_ERROR,
            DocumentProcessingState.STEMMED, DocumentProcessingState.TECHNICAL_ERROR);
      case CLEANED:
        return Sets.newHashSet(DocumentProcessingState.CLEANING_ERROR,
            DocumentProcessingState.STEMMED, DocumentProcessingState.TECHNICAL_ERROR);
      case STEMMED:
        return Sets.newHashSet(DocumentProcessingState.TECHNICAL_ERROR);
      default:
        return Collections.emptySet();
    }
  }

}
