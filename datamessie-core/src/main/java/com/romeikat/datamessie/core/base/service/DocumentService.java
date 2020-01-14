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
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.StemmedContentDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.function.EntityWithIdToIdFunction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.SequentialAsynchronousTask;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingTask;
import com.romeikat.datamessie.core.processing.util.DocumentsDatesConsumer;
import jersey.repackaged.com.google.common.base.Objects;
import jersey.repackaged.com.google.common.collect.Lists;

@Service
public class DocumentService {

  @Value("${documents.processing.batch.size}")
  private int batchSize;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Autowired
  @Qualifier("processingDocumentDao")
  private DocumentDao documentDao;

  @Autowired
  @Qualifier("crawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private StemmedContentDao stemmedContentDao;

  @Autowired
  private StemmedContentDao downloadDao;

  @Autowired
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  private StringUtil stringUtil;

  @Autowired
  private SessionFactory sessionFactory;

  public void deprocessDocumentsOfSource(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState) throws TaskCancelledException {
    // Initialize
    taskExecution.reportWork(String.format("Resetting documents of source %s to state %s", sourceId,
        targetState.getName()));
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        new StatisticsRebuildingSparseTable();

    // Determine necessary states and sources
    final Collection<DocumentProcessingState> statesForDeprocessing =
        getStatesForDeprocessing(targetState);
    final List<Long> sourceIds = Lists.newArrayList(sourceId);

    // Determine downloaded dates
    final SortedMap<LocalDate, Long> datesWithDocuments =
        documentDao.getDownloadedDatesWithNumberOfDocuments(statelessSession, null, null,
            statesForDeprocessing, sourceIds);
    final DocumentsDatesConsumer documentsDatesConsumer =
        new DocumentsDatesConsumer(datesWithDocuments, batchSize);
    if (documentsDatesConsumer.isEmpty()) {
      taskExecution.reportWork("No documents to be reset");
      return;
    }

    // Initialize first date range
    MutablePair<LocalDate, LocalDate> downloadedDateRange = new MutablePair<LocalDate, LocalDate>();
    final Pair<LocalDate, LocalDate> firstDateRange = documentsDatesConsumer.getNextDateRange();
    downloadedDateRange.setLeft(firstDateRange.getLeft());
    downloadedDateRange.setRight(firstDateRange.getRight());

    // Process date ranges
    Collection<Document> deprocessedDocuments = Collections.emptyList();
    final SequentialAsynchronousTask persistingTask = new SequentialAsynchronousTask();
    while (downloadedDateRange != null) {
      // Deprocess
      final Collection<Long> previousDocumentIds =
          Collections2.transform(deprocessedDocuments, new EntityWithIdToIdFunction());
      deprocessedDocuments = deprocessDocuments(statelessSession, persistingTask, taskExecution,
          sourceId, targetState, downloadedDateRange.getLeft(), downloadedDateRange.getRight(),
          statesForDeprocessing, previousDocumentIds, statisticsToBeRebuilt);

      // Prepare for next iteration
      final boolean noMoreDocumentsToDeprocess = deprocessedDocuments.size() < batchSize;
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
    persistingTask.waitUntilCompleted();

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
  }

  private List<Document> deprocessDocuments(final StatelessSession statelessSession,
      final SequentialAsynchronousTask persistingTask, final TaskExecution taskExecution,
      final long sourceId, final DocumentProcessingState targetState, final LocalDate fromDate,
      final LocalDate toDate, final Collection<DocumentProcessingState> statesForDeprocessing,
      final Collection<Long> previousDocumentIds,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt) throws TaskCancelledException {
    // Load
    final boolean oneDateOnly = Objects.equal(fromDate, toDate);
    final StringBuilder msg = new StringBuilder();
    msg.append("Loading documents to reset for download date");
    if (oneDateOnly) {
      msg.append(String.format(" %s", LocalDateConverter.INSTANCE_UI.convertToString(fromDate)));
    } else {
      msg.append(
          String.format("s %s to %s", LocalDateConverter.INSTANCE_UI.convertToString(fromDate),
              LocalDateConverter.INSTANCE_UI.convertToString(toDate)));
    }
    final TaskExecutionWork work = taskExecution.reportWorkStart(msg.toString());
    final List<Document> documentsToDeprocess =
        documentDao.getToProcess(statelessSession, fromDate, toDate, statesForDeprocessing,
            Lists.newArrayList(sourceId), previousDocumentIds, batchSize);
    taskExecution.reportWorkEnd(work);

    // Deprocess
    if (CollectionUtils.isNotEmpty(documentsToDeprocess)) {
      deprocessDocumentsAsynchronously(taskExecution, persistingTask, documentsToDeprocess,
          targetState, statisticsToBeRebuilt);
    }

    taskExecution.checkpoint();

    return documentsToDeprocess;
  }

  private void deprocessDocumentsAsynchronously(final TaskExecution taskExecution,
      final SequentialAsynchronousTask persistingTask, final Collection<Document> documents,
      final DocumentProcessingState targetState,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    final Runnable runnable = () -> {
      final String singularPlural =
          stringUtil.getSingularOrPluralTerm("document", documents.size());
      final TaskExecutionWork work = taskExecution
          .reportWorkStart(String.format("Resetting %s %s", documents.size(), singularPlural));

      // Update state and persist
      final Collection<Long> documentIds =
          documents.stream().map(d -> d.getId()).collect(Collectors.toSet());
      final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
      new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
        @Override
        protected void execute(final StatelessSession statelessSession) {
          documentDao.updateStates(statelessSession, documentIds, targetState);
        }

        @Override
        protected boolean shouldRethrowException() {
          return true;
        }
      }.execute();
      sessionProvider.closeStatelessSession();

      // Rebuild statistics
      for (final Document document : documents) {
        final long sourceId = document.getSourceId();
        final LocalDate publishedDate = document.getPublishedDate();
        statisticsToBeRebuilt.putValue(sourceId, publishedDate, true);
      }

      taskExecution.reportWorkEnd(work);
    };
    persistingTask.submit(runnable);
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

  /**
   * Deletes all processed entities assigned to documents. Does not delete their original data, i.e.
   * keeps the document and its raw content.
   *
   * @param statelessSession
   * @param documentIds
   * @param deleteCleanedContents
   * @param deleteStemmedContents
   * @param deleteNamedEntityOccurrences
   * @param deleteDownloads
   */
  public void deleteProcessedEntitiesOfDocumentIdsWithState(final StatelessSession statelessSession,
      final Collection<Long> documentIds, final DocumentProcessingState state,
      final boolean deleteCleanedContents, final boolean deleteStemmedContents,
      final boolean deleteNamedEntityOccurrences, final boolean deleteDownloads) {
    final Collection<Document> documents = documentDao.getEntities(statelessSession, documentIds);
    deleteProcessedEntitiesOfDocumentsWithState(statelessSession, documents, state,
        deleteCleanedContents, deleteStemmedContents, deleteNamedEntityOccurrences,
        deleteDownloads);
  }

  /**
   * Deletes all processed entities assigned to documents. Does not delete their original data, i.e.
   * keeps the document and its raw content.
   *
   * @param statelessSession
   * @param documents
   * @param deleteCleanedContents
   * @param deleteStemmedContents
   * @param deleteNamedEntityOccurrences
   * @param deleteDownloads
   */
  public void deleteProcessedEntitiesOfDocumentsWithState(final StatelessSession statelessSession,
      final Collection<Document> documents, final DocumentProcessingState state,
      final boolean deleteCleanedContents, final boolean deleteStemmedContents,
      final boolean deleteNamedEntityOccurrences, final boolean deleteDownloads) {
    if (documents.isEmpty()) {
      return;
    }

    // Filter for documents to be deleted (security check)
    final Collection<Document> documentsForDeletion = state == null ? documents
        : documents.stream().filter(d -> d.getState() == state).collect(Collectors.toSet());
    final Collection<Long> documentIdsForDeletion =
        documentsForDeletion.stream().map(d -> d.getId()).collect(Collectors.toSet());

    // Delete
    if (deleteCleanedContents) {
      cleanedContentDao.deleteForDocuments(statelessSession, documentIdsForDeletion);
    }
    if (deleteStemmedContents) {
      stemmedContentDao.deleteForDocuments(statelessSession, documentIdsForDeletion);
    }
    if (deleteNamedEntityOccurrences) {
      namedEntityOccurrenceDao.deleteForDocuments(statelessSession, documentIdsForDeletion);
    }
    if (deleteDownloads) {
      downloadDao.deleteForDocuments(statelessSession, documentIdsForDeletion);
    }
  }

}
