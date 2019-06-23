package com.romeikat.datamessie.core.processing.task.documentProcessing;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.google.common.collect.Collections2;
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
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.function.EntityWithIdToIdFunction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.init.DatamessieIndexingInitializer;
import com.romeikat.datamessie.core.processing.service.stemming.namedEntity.ClassifierPipelineProvider;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaner;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentRedirector;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentStemmer;
import com.romeikat.datamessie.core.processing.task.documentReindexing.DocumentsReindexer;

@Service(DocumentsProcessingTask.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentsProcessingTask implements Task {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsProcessingTask.class);

  public static final String BEAN_NAME = "documentsProcessingTask";

  public static final String NAME = "Documents processing";

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

  private DocumentsProcessingTask() {
    plugin = DateMessiePlugins.getInstance(ctx).getOrLoadPlugin(INamedEntityCategoryProider.class);
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

    // Determine minimum downloaded date
    final LocalDate minDownloadedDate = getMinDownloadedDate(sessionProvider.getStatelessSession());

    // Process all download dates one after another, starting with the minimum downloaded date
    final MutableObject<LocalDate> downloadedDate = new MutableObject<LocalDate>(minDownloadedDate);
    while (true) {
      // Load
      final List<Document> documentsToProcess = documentsLoader.loadDocumentsToProcess(
          sessionProvider.getStatelessSession(), taskExecution, downloadedDate.getValue());

      // Process
      if (CollectionUtils.isNotEmpty(documentsToProcess)) {
        final String singularPlural =
            stringUtil.getSingularOrPluralTerm("document", documentsToProcess.size());
        final TaskExecutionWork work = taskExecution.reportWorkStart(
            String.format("Processing %s %s", documentsToProcess.size(), singularPlural));

        final DocumentsProcessor documentsProcessor = new DocumentsProcessor(
            documentRedirector::redirect, downloadDao::getIdsWithEntities,
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
      prepareForNextIteration(taskExecution, downloadedDate, documentsToProcess);
    }
  }

  private LocalDate getMinDownloadedDate(final StatelessSession statelessSession) {
    if (StringUtils.isNotBlank(minDownloadedDate)) {
      Date parseDate;
      try {
        parseDate = DateUtils.parseDate(minDownloadedDate, "yyyy-MM-dd");
        return DateUtil.toLocalDate(parseDate);
      } catch (final ParseException e) {
        final String msg = String.format("Cound not parse minDownloadedDate %s", minDownloadedDate);
        LOG.error(msg, e);
        return null;
      }
    }

    final LocalDateTime minDownloadedDateTime = documentDao.getMinDownloaded(statelessSession);
    if (minDownloadedDateTime == null) {
      return LocalDate.now();
    }

    final LocalDate minDownloadedDate = minDownloadedDateTime.toLocalDate();
    return minDownloadedDate;
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
      final MutableObject<LocalDate> downloadedDate, final List<Document> documentsToProcess)
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
      final LocalDate nextDownloadedDate = getNextDownloadedDate(previousDownloadDate);

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
      final LocalDate previousDownloadDate = downloadedDate.getValue();
      final LocalDate nextDownloadedDate = getNextDownloadedDate(previousDownloadDate);

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
    }
  }

  private LocalDate getNextDownloadedDate(final LocalDate downloadedDate) {
    // If download date is current date (or future), remain at current date
    final LocalDate now = LocalDate.now();
    if (!downloadedDate.isBefore(now)) {
      return now;
    }
    // Otherwise, go to next date
    return downloadedDate.plusDays(1);
  }

}
