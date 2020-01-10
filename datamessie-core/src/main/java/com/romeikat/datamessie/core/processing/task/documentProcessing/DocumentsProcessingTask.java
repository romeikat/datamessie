package com.romeikat.datamessie.core.processing.task.documentProcessing;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.INamedEntityCategoryProider;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.function.EntityWithIdToIdFunction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.init.DatamessieIndexingInitializer;
import com.romeikat.datamessie.core.processing.service.stemming.namedEntity.ClassifierPipelineProvider;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaner;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentRedirector;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentStemmer;
import com.romeikat.datamessie.core.processing.task.documentReindexing.DocumentsReindexer;
import jersey.repackaged.com.google.common.collect.Lists;

@Service(DocumentsProcessingTask.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentsProcessingTask implements Task {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsProcessingTask.class);

  public static final String BEAN_NAME = "documentsProcessingTask";

  public static final String NAME = "Documents processing";

  @Value("${documents.processing.stemming.enabled}")
  private boolean stemmingEnabled;

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private DatamessieIndexingInitializer indexingInitializer;

  @Autowired
  private ClassifierPipelineProvider classifierPipelineProvider;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private DocumentsLoader documentsLoader;

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Autowired
  private DocumentsReindexer documentsReindexer;

  @Autowired
  private StringUtil stringUtil;

  @Autowired
  private DocumentRedirector documentRedirector;

  @Autowired
  private DocumentCleaner documentCleaner;

  @Autowired
  private DocumentStemmer documentStemmer;

  @Autowired
  @Qualifier("processingDocumentDao")
  private DocumentDao documentDao;

  @Autowired
  private DownloadDao downloadDao;

  @Autowired
  private NamedEntityDao namedEntityDao;

  @Autowired
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Value("${documents.processing.downloaded.date.min}")
  private String minDownloadedDate;

  @Value("${documents.processing.batch.size}")
  private int batchSize;

  @Value("${documents.processing.batch.pause}")
  private long pause;

  private HibernateSessionProvider sessionProvider;

  private final INamedEntityCategoryProider plugin;

  /**
   * The current downloadedDate being processed
   */
  private final MutableObject<LocalDate> downloadedDate;

  /**
   * A new downloadedDate to restart from
   */
  private LocalDate restartFromDownloadedDate;


  private DocumentsProcessingTask() {
    plugin = DateMessiePlugins.getInstance(ctx).getOrLoadPlugin(INamedEntityCategoryProider.class);
    downloadedDate = new MutableObject<LocalDate>(null);
  }

  @PostConstruct
  private void initialize() {
    sessionProvider = new HibernateSessionProvider(sessionFactory);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isVisibleAfterCompleted() {
    return true;
  }

  @Override
  public Integer getPriority() {
    return 7;
  }

  @Override
  public void execute(final TaskExecution taskExecution) throws Exception {
    // Wait until everything is ready for processing
    waitUntilInitializationIsFinished(taskExecution);

    // Start processing
    performProcessing(taskExecution);
  }

  private void waitUntilInitializationIsFinished(final TaskExecution taskExecution) {
    indexingInitializer.waitUntilIndexesInitialized(taskExecution);
    classifierPipelineProvider.waitUntilPiplineInitialized(taskExecution);
  }

  private void performProcessing(final TaskExecution taskExecution) throws TaskCancelledException {
    // Initialize
    taskExecution.reportWork("Starting documents processing");

    // Determine downloaded dates
    final Collection<DocumentProcessingState> statesForProcessing = getStatesForProcessing();
    final Queue<LocalDate> downloadedDates =
        Lists.newLinkedList(documentDao.getDownloadedDatesWithDocuments(
            sessionProvider.getStatelessSession(), statesForProcessing));

    // Process all download dates one after another, starting with the minimum one
    final LocalDate minDownloadedDate =
        downloadedDates.isEmpty() ? LocalDate.now() : downloadedDates.poll();
    downloadedDate.setValue(minDownloadedDate);
    while (true) {
      // Apply provided date, if available
      applyDownloadedDateToRestartFrom(taskExecution);

      // Load
      final List<Document> documentsToProcess =
          documentsLoader.loadDocumentsToProcess(sessionProvider.getStatelessSession(),
              taskExecution, downloadedDate.getValue(), statesForProcessing);

      // Process
      if (CollectionUtils.isNotEmpty(documentsToProcess)) {
        final String singularPlural =
            stringUtil.getSingularOrPluralTerm("document", documentsToProcess.size());
        final TaskExecutionWork work = taskExecution.reportWorkStart(
            String.format("Processing %s %s", documentsToProcess.size(), singularPlural));

        final DocumentsProcessor documentsProcessor = new DocumentsProcessor(
            documentRedirector::redirect, downloadDao::getForDocuments,
            documentDao::getIdsWithEntities, downloadDao::getDocumentIdsForUrlsAndSource,
            documentCleaner::clean, documentStemmer::stem, namedEntityDao::getOrCreate,
            namedEntityCategoryDao::getWithoutCategories,
            plugin == null ? null : plugin::provideCategoryTitles,
            (documentsToBeUpdated, downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated,
                cleanedContentsToBeCreatedOrUpdated, stemmedContentsToBeCreatedOrUpdated,
                namedEntityOccurrencesToBeReplaced, namedEntityCategoriesToBeSaved) -> documentDao
                    .persistDocumentsProcessingOutput(sessionProvider.getStatelessSession(),
                        documentsToBeUpdated, downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated,
                        cleanedContentsToBeCreatedOrUpdated, stemmedContentsToBeCreatedOrUpdated,
                        namedEntityOccurrencesToBeReplaced, namedEntityCategoriesToBeSaved),
            ctx);
        documentsProcessor.processDocuments(documentsToProcess);

        rebuildStatistics(documentsProcessor.getStatisticsToBeRebuilt());
        reindexDocuments(documentsToProcess);

        taskExecution.reportWorkEnd(work);
        taskExecution.checkpoint();
      }

      // Prepare for next iteration
      prepareForNextIteration(taskExecution, documentsToProcess, downloadedDates);
    }
  }

  private Set<DocumentProcessingState> getStatesForProcessing() {
    return stemmingEnabled
        ? Sets.newHashSet(DocumentProcessingState.DOWNLOADED, DocumentProcessingState.REDIRECTED,
            DocumentProcessingState.CLEANED)
        : Sets.newHashSet(DocumentProcessingState.DOWNLOADED, DocumentProcessingState.REDIRECTED);
  }

  private void applyDownloadedDateToRestartFrom(final TaskExecution taskExecution) {
    // No date provided
    if (restartFromDownloadedDate == null) {
      return;
    }

    // Processing has not yet started
    final LocalDate currentDownloadedDate = downloadedDate.getValue();
    if (currentDownloadedDate == null) {
      return;
    }

    if (restartFromDownloadedDate.isBefore(currentDownloadedDate)) {
      taskExecution.reportWork(String.format("Re-starting processing at %s",
          (LocalDateConverter.INSTANCE_UI.convertToString(restartFromDownloadedDate))));
      downloadedDate.setValue(restartFromDownloadedDate);
      restartFromDownloadedDate = null;
    }
  }

  private void rebuildStatistics(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    final IStatisticsManager statisticsManager =
        sharedBeanProvider.getSharedBean(IStatisticsManager.class);
    if (statisticsManager != null) {
      statisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    }
  }

  private void reindexDocuments(final List<Document> documents) throws TaskCancelledException {
    final Collection<Long> documentIds =
        Collections2.transform(documents, new EntityWithIdToIdFunction());
    documentsReindexer.toBeReindexed(documentIds);
  }

  private void prepareForNextIteration(final TaskExecution taskExecution,
      final List<Document> documentsToProcess, final Queue<LocalDate> downloadedDates)
      throws TaskCancelledException {
    // No documents to process due to an error while loading
    final boolean errorOccurred = documentsToProcess == null;
    if (errorOccurred) {
      // In case of an error, wait and continue with same downloaded date
      sessionProvider.closeStatelessSession();
      taskExecution.checkpoint(pause);

      // Next download date to be processed is the same
      return;
    }

    // No documents to process for that downloaded date
    final boolean noDocumentsToProcess = documentsToProcess.isEmpty();
    if (noDocumentsToProcess) {
      // Determine next downloaded date
      final LocalDate previousDownloadDate = downloadedDate.getValue();
      final LocalDate nextDownloadedDate = getNextDownloadedDate(downloadedDates);

      // Current date is reached
      final boolean isCurrentDate = previousDownloadDate.equals(nextDownloadedDate);
      if (isCurrentDate) {
        // Pause
        sessionProvider.closeStatelessSession();
        taskExecution.checkpoint(pause);
        // Next downloaded date to be processed is the same
      }
      // Current date is not yet reached
      else {
        // Next downloaded date to be processed is the next day
        downloadedDate.setValue(nextDownloadedDate);
      }
      return;
    }

    // No more documents to process for that downloaded date
    final boolean noMoreDocumentsToProcess = documentsToProcess.size() < batchSize;
    if (noMoreDocumentsToProcess) {
      // Increase download date
      // Determine next downloaded date
      final LocalDate nextDownloadedDate = getNextDownloadedDate(downloadedDates);

      // Current date is reached
      final LocalDate previousDownloadDate = downloadedDate.getValue();
      final boolean isCurrentDate = previousDownloadDate.equals(nextDownloadedDate);
      if (isCurrentDate) {
        // Pause
        sessionProvider.closeStatelessSession();
        taskExecution.checkpoint(pause);
        // Next downloaded date to be processed is the same
      }
      // Current date is not yet reached
      else {
        // Next downloaded date to be processed is the next day
        downloadedDate.setValue(nextDownloadedDate);
      }
    }
  }

  private LocalDate getNextDownloadedDate(final Queue<LocalDate> downloadedDates) {
    // If no more download dates to process, remain at current date
    if (downloadedDates.isEmpty()) {
      return LocalDate.now();
    }
    // Otherwise, go to next date
    return downloadedDates.poll();
  }

  public void restartFromDownloadedDate(final LocalDate downloadedDate) {
    if (downloadedDate == null) {
      return;
    }

    if (restartFromDownloadedDate == null || restartFromDownloadedDate.isAfter(downloadedDate)) {
      restartFromDownloadedDate = downloadedDate;
    }
  }

}
