package com.romeikat.datamessie.core.processing.task.documentProcessing;

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

import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.CleanCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsForUrlsAndSourceCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsWithEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDownloadIdsWithEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetNamedEntityNamesWithoutCategoryCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetOrCreateNamedEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentsProcessingOutputCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.ProvideNamedEntityCategoryTitlesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.RedirectCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.StemCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentsCleaner;
import com.romeikat.datamessie.core.processing.task.documentProcessing.namedEntities.NamedEntitiesProcessor;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentsRedirector;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentsStemmer;
import com.romeikat.datamessie.core.processing.task.documentProcessing.validate.DocumentsValidator;

/**
 * Processes multiple documents at a time. The performance strategy is to use as few data access
 * operations for reading and writing as possible.
 *
 * For this purpose, required objects are loaded in the beginning and put into an input cache
 * {@link DocumentsProcessingInput}. Any input object modified during the processing is copied (by
 * reference) to an output cache {@link DocumentsProcessingOutput}. Thus, input and output cache
 * will contain changes to objects that have not yet been persisted. Persisting all objects of the
 * output cache will be delayed to the very end.
 *
 * Any object being created during the processing is added to the output cache. Thus, the output
 * cache will also contain objects that are not present in the input cache. The other way round, the
 * input cache can contain objects that are not present in the output cache, but such objects can
 * only be unmodified objects.
 *
 * For some processing phases (like redirecting), not all required objects cannot be loaded in the
 * very beginning, as those objects (e.g. required for redirecting) can only be determined during
 * the redirection phase itself. For such lazy loading, objects from the output cache must be
 * prioritized towards the input cache and the persistent storage (as the output cache might contain
 * new objects or changes to existing objects that both have not yet been persisted).
 *
 * Lazily loaded object need not be added to the input cache as long as they are not being modified.
 * Once being modified, they must be added to the input and output cache. This assures referential
 * equality between objects in both caches, which is used for persisting in the end to differentiate
 * new objects from modified ones.
 *
 * @author Dr. Raphael Romeikat
 */
public class DocumentsProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsProcessor.class);

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;
  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;

  private final DocumentsValidator documentsValidator;
  private final DocumentsRedirector documentsRedirector;
  private final DocumentsCleaner documentsCleaner;
  private final DocumentsStemmer documentsStemmer;
  private final NamedEntitiesProcessor namedEntitiesProcessor;
  private final PersistDocumentsProcessingOutputCallback persistDocumentsProcessingOutputCallback;

  private final boolean logExecutionTimes = false;
  private final StopWatch sw = new StopWatch();

  public DocumentsProcessor(final RedirectCallback redirectCallback,
      final GetDownloadIdsWithEntitiesCallback getDownloadIdsWithEntitiesCallback,
      final GetDocumentIdsWithEntitiesCallback getDocumentIdsWithEntitiesCallback,
      final GetDocumentIdsForUrlsAndSourceCallback getDocumentIdsForUrlsAndSourceCallback,
      final CleanCallback cleanCallback, final StemCallback stemCallback,
      final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback,
      final GetNamedEntityNamesWithoutCategoryCallback getNamedEntityNamesWithoutCategoryCallback,
      final ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback,
      final PersistDocumentsProcessingOutputCallback persistDocumentsProcessingOutputCallback,
      final ApplicationContext ctx) {
    documentsProcessingInput = new DocumentsProcessingInput(ctx);
    documentsProcessingOutput = new DocumentsProcessingOutput();
    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();

    documentsValidator =
        new DocumentsValidator(documentsProcessingInput, documentsProcessingOutput);
    documentsRedirector = new DocumentsRedirector(documentsProcessingInput,
        documentsProcessingOutput, redirectCallback, getDownloadIdsWithEntitiesCallback,
        getDocumentIdsWithEntitiesCallback, getDocumentIdsForUrlsAndSourceCallback, ctx);
    documentsCleaner = new DocumentsCleaner(documentsProcessingInput, documentsProcessingOutput,
        cleanCallback, ctx);
    documentsStemmer = new DocumentsStemmer(documentsProcessingInput, documentsProcessingOutput,
        stemCallback, ctx);
    namedEntitiesProcessor = new NamedEntitiesProcessor(documentsProcessingInput,
        documentsProcessingOutput, getOrCreateNamedEntitiesCallback,
        getNamedEntityNamesWithoutCategoryCallback, provideNamedEntityCategoryTitlesCallback, ctx);
    this.persistDocumentsProcessingOutputCallback = persistDocumentsProcessingOutputCallback;
  }

  public void processDocuments(final Collection<Document> documents) {
    if (logExecutionTimes) {
      sw.start();
    }

    documentsProcessingInput.addDocuments(documents);
    logExecutionTime("Add");

    // Process documents
    doProcessing();

    // Mark statistics to be rebuilt
    rebuildStatistics();
    logExecutionTime("Statistics");

    // Persist all changes
    persistDocumentsProcessingOutput();
    logExecutionTime("Persist");

    if (logExecutionTimes) {
      sw.stop();
    }
  }

  private void doProcessing() {
    documentsValidator.validateDocuments();
    logExecutionTime("Validate");

    documentsRedirector.redirectDocuments();
    logExecutionTime("Redirect");

    documentsCleaner.cleanDocuments();
    logExecutionTime("Clean");

    documentsStemmer.stemDocuments();
    logExecutionTime("Stem");

    namedEntitiesProcessor.processNamedEntities();
    logExecutionTime("Named entities");
  }

  private void rebuildStatistics() {
    for (final Document document : documentsProcessingOutput.getDocuments()) {
      statisticsToBeRebuilt.putValue(document.getSourceId(), document.getPublishedDate(), true);
    }
  }

  private void persistDocumentsProcessingOutput() {
    // Retrieve output
    final Collection<Document> documentsToBeUpdated = documentsProcessingOutput.getDocuments();
    final Collection<Download> downloadsToBeCreatedOrUpdated =
        documentsProcessingOutput.getDownloads();
    final Collection<RawContent> rawContentsToBeUpdated =
        documentsProcessingOutput.getRawContents();
    final Collection<CleanedContent> cleanedContentsToBeCreatedOrUpdated =
        documentsProcessingOutput.getCleanedContents();
    final Collection<StemmedContent> stemmedContentsToBeCreatedOrUpdated =
        documentsProcessingOutput.getStemmedContents();
    final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced =
        documentsProcessingOutput.getNamedEntityOccurrences();
    final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved =
        documentsProcessingOutput.getNamedEntityCategories();

    // Persist
    persistDocumentsProcessingOutputCallback.persistDocumentsProcessingOutput(documentsToBeUpdated,
        downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated, cleanedContentsToBeCreatedOrUpdated,
        stemmedContentsToBeCreatedOrUpdated, namedEntityOccurrencesToBeReplaced,
        namedEntityCategoriesToBeSaved);
  }

  protected DocumentsProcessingInput getDocumentsProcessingInput() {
    return documentsProcessingInput;
  }

  protected DocumentsProcessingOutput getDocumentsProcessingOutput() {
    return documentsProcessingOutput;
  }

  public StatisticsRebuildingSparseTable getStatisticsToBeRebuilt() {
    return statisticsToBeRebuilt;
  }

  private void logExecutionTime(final String section) {
    if (logExecutionTimes) {
      LOG.info("{}: {}s", section, sw.getTime() / 1000);
    }
  }

}
