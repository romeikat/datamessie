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
import java.util.Queue;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingTask;
import jersey.repackaged.com.google.common.collect.Lists;

@Service
public class DocumentService {

  private final static Logger LOG = LoggerFactory.getLogger(DocumentService.class);

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

    // Determine downloaded dates
    final Collection<DocumentProcessingState> statesForDeprocessing =
        getStatesForDeprocessing(targetState);
    final Queue<LocalDate> downloadedDates = Lists.newLinkedList(
        documentDao.getDownloadedDatesWithDocuments(statelessSession, statesForDeprocessing));

    // Process all download dates one after another, starting with the minimum one
    final LocalDate minDownloadedDate = downloadedDates.isEmpty() ? null : downloadedDates.poll();
    final MutableObject<LocalDate> downloadedDate = new MutableObject<LocalDate>(minDownloadedDate);
    while (downloadedDate.getValue() != null) {
      // Deprocess
      deprocessDocumentsOfSourceAndDownloadDate(statelessSession, taskExecution, sourceId,
          targetState, statisticsToBeRebuilt, downloadedDate.getValue(), statesForDeprocessing);

      // Prepare for next iteration
      final LocalDate nextDownloadedDate = getNextDownloadedDate(downloadedDate.getValue());
      downloadedDate.setValue(nextDownloadedDate);
    }

    // Notify DocumentsProcessingTask to start processing
    final Collection<DocumentsProcessingTask> activeTasks =
        taskManager.getActiveTasks(DocumentsProcessingTask.NAME, DocumentsProcessingTask.class);
    for (final DocumentsProcessingTask activeTask : activeTasks) {
      activeTask.restartFromDownloadedDate(minDownloadedDate);
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

  private void deprocessDocumentsOfSourceAndDownloadDate(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt, final LocalDate downloadedDate,
      final Collection<DocumentProcessingState> statesForDeprocessing)
      throws TaskCancelledException {
    // Load
    final List<Document> documentsToDeprocess = documentDao.getForSourceAndDownloaded(
        statelessSession, sourceId, downloadedDate, statesForDeprocessing);

    // Deprocess
    if (CollectionUtils.isNotEmpty(documentsToDeprocess)) {
      final String singularPlural =
          stringUtil.getSingularOrPluralTerm("document", documentsToDeprocess.size());
      final TaskExecutionWork work = taskExecution.reportWorkStart(
          String.format("Deprocessing %s %s", documentsToDeprocess.size(), singularPlural));
      deprocessDocuments(statelessSession, documentsToDeprocess, targetState,
          statisticsToBeRebuilt);
      taskExecution.reportWorkEnd(work);
    }

    taskExecution.checkpoint();
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

  private LocalDate getNextDownloadedDate(final LocalDate downloadedDate) {
    // Increase only up to current date
    final LocalDate now = LocalDate.now();
    if (downloadedDate.isAfter(now)) {
      return null;
    }
    // Otherwise, go to next date
    return downloadedDate.plusDays(1);
  }

}
