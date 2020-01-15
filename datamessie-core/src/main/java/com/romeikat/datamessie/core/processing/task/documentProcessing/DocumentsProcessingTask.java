package com.romeikat.datamessie.core.processing.task.documentProcessing;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.StringUtil;
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
import com.romeikat.datamessie.core.processing.util.DocumentsDatesConsumer;
import com.romeikat.datamessie.core.processing.util.ProcessingDates;
import jersey.repackaged.com.google.common.base.Objects;
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

  @Autowired
  private SourceDao sourceDao;

  @Value("${documents.processing.downloaded.date.min}")
  private String minDownloadedDate;

  @Value("${documents.processing.downloaded.date.max}")
  private String maxDownloadedDate;

  @Value("${documents.processing.batch.size}")
  private int batchSize;

  @Value("${documents.processing.batch.pause}")
  private long pause;

  private HibernateSessionProvider sessionProvider;

  private final INamedEntityCategoryProider plugin;

  /**
   * The downloaded dates consumer; {@code null} indicates that processing should be restarted
   */
  private DocumentsDatesConsumer documentsDatesConsumer;
  /**
   * The current dates range being processed
   */
  private final ProcessingDates processingDates;

  /**
   * The source IDs to be processed.
   */
  private Collection<Long> sourceIdsForProcessing;

  /**
   * Whether processing should be restarted
   */
  private boolean restartProcessing;

  private final Long documentIdForTesting = null;


  private DocumentsProcessingTask() {
    plugin = DateMessiePlugins.getInstance(ctx).getOrLoadPlugin(INamedEntityCategoryProider.class);
    processingDates = new ProcessingDates();
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
    return null;
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
    taskExecution.reportWork("Starting documents processing");

    LocalDate fromDate = parseDate(minDownloadedDate);
    LocalDate toDate = parseDate(maxDownloadedDate);

    Collection<Document> documentsToProcess = Collections.emptyList();

    while (true) {
      // Determine necessary states and sources
      final Collection<DocumentProcessingState> statesForProcessing = getStatesForProcessing();
      final Collection<Long> sourceIdsForProcessing = getSourceIdsForProcessing();

      // Testing
      Document documentForTesting = null;
      if (documentIdForTesting != null) {
        documentForTesting =
            documentDao.getEntity(sessionProvider.getStatelessSession(), documentIdForTesting);
        if (documentForTesting != null) {
          fromDate = toDate = documentForTesting.getDownloaded().toLocalDate();
        }
      }

      // Initialize
      initializeProcessingIfNecessary(taskExecution, fromDate, toDate, statesForProcessing,
          sourceIdsForProcessing);

      // Load documents within date range
      final Collection<Long> previousDocumentIds =
          Collections2.transform(documentsToProcess, new EntityWithIdToIdFunction());
      if (documentForTesting != null) {
        documentsToProcess = Lists.newArrayList(documentForTesting);
      } else {
        documentsToProcess = documentsLoader.loadDocumentsToProcess(
            sessionProvider.getStatelessSession(), taskExecution,
            processingDates.getProcessingFromDate(), processingDates.getProcessingToDate(),
            statesForProcessing, sourceIdsForProcessing, previousDocumentIds);
      }

      // Process date range
      if (CollectionUtils.isNotEmpty(documentsToProcess)) {
        final String singularPlural =
            stringUtil.getSingularOrPluralTerm("document", documentsToProcess.size());
        final TaskExecutionWork work = taskExecution.reportWorkStart(
            String.format("Processing %s %s", documentsToProcess.size(), singularPlural));

        final DocumentsProcessor documentsProcessor =
            new DocumentsProcessor(documentRedirector::redirect, downloadDao::getForDocuments,
                documentDao::getIdsWithEntities, downloadDao::getDocumentIdsForUrlsAndSource,
                documentCleaner::clean, documentStemmer::stem, namedEntityDao::getOrCreate,
                namedEntityCategoryDao::getWithoutCategories,
                plugin == null ? null : plugin::provideCategoryTitles,
                (documentsToBeUpdated, downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated,
                    cleanedContentsToBeCreatedOrUpdated, stemmedContentsToBeCreatedOrUpdated,
                    namedEntityOccurrencesToBeReplaced,
                    namedEntityCategoriesToBeSaved) -> documentDao.persistDocumentsProcessingOutput(
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
      prepareForNextIteration(taskExecution, toDate, statesForProcessing, sourceIdsForProcessing,
          documentsToProcess);
    }
  }

  private LocalDate parseDate(final String dateAsString) {
    if (StringUtils.isBlank(dateAsString)) {
      return null;
    }

    try {
      return LocalDate.parse(dateAsString);
    } catch (final DateTimeParseException e) {
      final String msg = String.format("Cound not parse date %s", dateAsString);
      LOG.error(msg, e);
      return null;
    }
  }

  private Set<DocumentProcessingState> getStatesForProcessing() {
    return stemmingEnabled
        ? Sets.newHashSet(DocumentProcessingState.DOWNLOADED, DocumentProcessingState.REDIRECTED,
            DocumentProcessingState.CLEANED)
        : Sets.newHashSet(DocumentProcessingState.DOWNLOADED, DocumentProcessingState.REDIRECTED);
  }

  private Set<Long> getSourceIdsForProcessing() {
    return Sets.newHashSet(sourceDao.getIdsToBeProcessed(sessionProvider.getStatelessSession()));
  }

  private void initializeProcessingIfNecessary(final TaskExecution taskExecution,
      final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> statesForProcessing,
      final Collection<Long> sourceIdsForProcessing) {
    // Check whether sources changed
    final boolean sourcesChanged =
        !Objects.equal(this.sourceIdsForProcessing, sourceIdsForProcessing);
    if (sourcesChanged) {
      this.sourceIdsForProcessing = sourceIdsForProcessing;
      restartProcessing = true;
    }

    // Processing should be restarted
    if (restartProcessing) {
      // Indicate that processing should be restarted
      documentsDatesConsumer = null;

      resetProcessingDates();
    }

    // Processing should be continued
    if (documentsDatesConsumer != null) {
      // No initialization necessary
      return;
    }

    final TaskExecutionWork work =
        taskExecution.reportWorkStart(String.format("Initializing processing"));

    // Initialize all numbers and processing dates
    initializeNumbersAndProcessingDates(fromDate, toDate, statesForProcessing,
        sourceIdsForProcessing);
    restartProcessing = false;

    taskExecution.reportWorkEnd(work);
  }

  private void prepareForNextIteration(final TaskExecution taskExecution, final LocalDate toDate,
      final Collection<DocumentProcessingState> statesForProcessing,
      final Collection<Long> sourceIdsForProcessing, final Collection<Document> documentsToProcess)
      throws TaskCancelledException {
    // Error while loading documents
    final boolean errorOccurred = documentsToProcess == null;
    if (errorOccurred) {
      // In case of an error, wait...
      sessionProvider.closeStatelessSession();
      taskExecution.checkpoint(pause);

      // ... and try again with the current date range
      return;
    }

    // More documents to process for that date range
    final boolean moreDocumentsToProcess = documentsToProcess.size() >= batchSize;
    if (moreDocumentsToProcess) {
      // Continue with the current date range
      return;
    }

    // No more documents to process for that date range => iterate to next date range

    // Consume dates
    documentsDatesConsumer.removeDates(processingDates.getProcessingToDate());

    // No more dates available => re-determine numbers, starting from end of previous numbers
    if (documentsDatesConsumer.isEmpty()) {
      // Pause
      sessionProvider.closeStatelessSession();
      taskExecution.checkpoint(pause);

      // Initialize next numbers and processing dates
      final LocalDate previousToDate = processingDates.getProcessingToDate();
      initializeNumbersAndProcessingDates(previousToDate, toDate, statesForProcessing,
          sourceIdsForProcessing);
    }
    // More dates available => apply next date range from existing numbers
    else {
      applyNextDateRangeFromNumbers();
    }
  }

  private void initializeNumbersAndProcessingDates(final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> statesForProcessing,
      final Collection<Long> sourceIdsForProcessing) {
    // Determine numbers
    determineNumbers(fromDate, toDate, statesForProcessing, sourceIdsForProcessing);

    // Determine initial date range to process
    if (documentsDatesConsumer.isEmpty()) {
      applyNewDateRange(fromDate, toDate);
    } else {
      applyNextDateRangeFromNumbers();
    }
  }

  private void determineNumbers(final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> statesForProcessing,
      final Collection<Long> sourceIdsForProcessing) {
    final SortedMap<LocalDate, Long> datesWithDocuments =
        documentDao.getDownloadedDatesWithNumberOfDocuments(sessionProvider.getStatelessSession(),
            fromDate, toDate, statesForProcessing, sourceIdsForProcessing);
    documentsDatesConsumer = new DocumentsDatesConsumer(datesWithDocuments, batchSize);
  }

  private void resetProcessingDates() {
    processingDates.setNumbersToDate(null);
    processingDates.setProcessingFromDate(null);
    processingDates.setProcessingToDate(null);
  }

  private void applyNewDateRange(final LocalDate fromDate, final LocalDate toDate) {
    LocalDate processingFromDate;
    LocalDate processingToDate;

    // Use the provided starting date, if available
    if (fromDate != null) {
      processingFromDate = fromDate;
    }
    // Otherwise, start from yesterday (in case we just passed midnight)
    else {
      final LocalDate yesterday = LocalDate.now().minusDays(1);
      processingFromDate = yesterday;
    }

    // Use the provided ending date, if available
    if (toDate != null) {
      processingToDate = toDate;
    }
    // Otherwise, stop at today
    else {
      final LocalDate today = LocalDate.now();
      processingToDate = today;
    }

    // Prevent negative intervals
    if (processingToDate.isBefore(processingFromDate)) {
      processingToDate = processingFromDate;
    }

    // Done
    processingDates.setProcessingFromDate(processingFromDate);
    processingDates.setProcessingToDate(processingToDate);
  }

  private void applyNextDateRangeFromNumbers() {
    final Pair<LocalDate, LocalDate> dateRange = documentsDatesConsumer.getNextDateRange();
    processingDates.setProcessingFromDate(dateRange.getLeft());
    processingDates.setProcessingToDate(dateRange.getRight());
  }

  private void rebuildStatistics(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    final IStatisticsManager statisticsManager =
        sharedBeanProvider.getSharedBean(IStatisticsManager.class);
    if (statisticsManager != null) {
      statisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    }
  }

  private void reindexDocuments(final Collection<Document> documents)
      throws TaskCancelledException {
    final Collection<Long> documentIds =
        Collections2.transform(documents, new EntityWithIdToIdFunction());
    documentsReindexer.toBeReindexed(documentIds);
  }

  public void restartProcessing() {
    restartProcessing = true;
  }

}
