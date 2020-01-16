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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.util.ExecutionTimeLogger;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.CleanCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsForUrlsAndSourceCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsWithEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDownloadsPerDocumentIdCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetNamedEntityNamesWithoutCategoryCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetOrCreateNamedEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentProcessingOutputCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentsProcessingOutputCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.ProvideNamedEntityCategoryTitlesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.RedirectCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.StemCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentsCleaner;
import com.romeikat.datamessie.core.processing.task.documentProcessing.namedEntities.NamedEntitiesProcessor;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentsRedirector;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentsStemmer;
import com.romeikat.datamessie.core.processing.task.documentProcessing.validate.DocumentsValidator;
import jersey.repackaged.com.google.common.collect.Lists;

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

  private final Double processingParallelismFactor;
  private boolean stemmingEnabled = true;

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;
  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;

  private final DocumentsValidator documentsValidator;
  private final DocumentsRedirector documentsRedirector;
  private final DocumentsCleaner documentsCleaner;
  private final DocumentsStemmer documentsStemmer;
  private final NamedEntitiesProcessor namedEntitiesProcessor;

  private final ExecutionTimeLogger executionTimeLogger;

  public DocumentsProcessor(final RedirectCallback redirectCallback,
      final GetDownloadsPerDocumentIdCallback getDownloadIdsWithEntitiesCallback,
      final GetDocumentIdsWithEntitiesCallback getDocumentIdsWithEntitiesCallback,
      final GetDocumentIdsForUrlsAndSourceCallback getDocumentIdsForUrlsAndSourceCallback,
      final CleanCallback cleanCallback, final StemCallback stemCallback,
      final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback,
      final GetNamedEntityNamesWithoutCategoryCallback getNamedEntityNamesWithoutCategoryCallback,
      final ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback,
      final PersistDocumentProcessingOutputCallback persistDocumentProcessingOutputCallback,
      final PersistDocumentsProcessingOutputCallback persistDocumentsProcessingOutputCallback,
      final ApplicationContext ctx) {
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));
    stemmingEnabled = Boolean
        .parseBoolean(SpringUtil.getPropertyValue(ctx, "documents.processing.stemming.enabled"));

    documentsProcessingInput = new DocumentsProcessingInput(ctx);
    documentsProcessingOutput = new DocumentsProcessingOutput();
    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();

    documentsValidator = new DocumentsValidator(documentsProcessingInput, documentsProcessingOutput,
        persistDocumentProcessingOutputCallback);
    documentsRedirector = new DocumentsRedirector(documentsProcessingInput,
        documentsProcessingOutput, redirectCallback, getDownloadIdsWithEntitiesCallback,
        getDocumentIdsWithEntitiesCallback, getDocumentIdsForUrlsAndSourceCallback,
        persistDocumentProcessingOutputCallback, persistDocumentsProcessingOutputCallback, ctx);
    documentsCleaner = new DocumentsCleaner(documentsProcessingInput, documentsProcessingOutput,
        cleanCallback, persistDocumentProcessingOutputCallback, ctx);
    documentsStemmer = new DocumentsStemmer(documentsProcessingInput, documentsProcessingOutput,
        stemCallback, persistDocumentProcessingOutputCallback, ctx);
    namedEntitiesProcessor = new NamedEntitiesProcessor(documentsProcessingInput,
        documentsProcessingOutput, getOrCreateNamedEntitiesCallback,
        getNamedEntityNamesWithoutCategoryCallback, provideNamedEntityCategoryTitlesCallback,
        persistDocumentProcessingOutputCallback, persistDocumentsProcessingOutputCallback, ctx);

    executionTimeLogger = new ExecutionTimeLogger(getClass());
  }

  public void processDocuments(final Collection<Document> documents) {
    executionTimeLogger.start();

    documentsProcessingInput.addDocuments(documents);
    executionTimeLogger.log("Add");

    // Process documents
    executionTimeLogger.log("Wait");
    doProcessing();

    // Mark statistics to be rebuilt
    rebuildStatistics();
    executionTimeLogger.log("Statistics");

    executionTimeLogger.stop();
  }

  private void doProcessing() {
    documentsValidator.validateDocuments();
    executionTimeLogger.log("Validate");

    // Documents must be redirected one after another
    documentsRedirector.redirectDocuments();
    executionTimeLogger.log("Redirect");

    // Clean and stem documents in parallel
    new ParallelProcessing<Document>(null, documentsProcessingInput.getDocuments(),
        processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Document document) {
        try {
          final Collection<Document> documentAsCollection = Lists.newArrayList(document);
          documentsCleaner.cleanDocuments(documentAsCollection);
          documentsStemmer.stemDocuments(documentAsCollection, stemmingEnabled);
        } catch (final Exception e) {
          final String msg = String.format("Could not clean document %s", document.getId());
          LOG.error(msg, e);
        }
      }
    };
    executionTimeLogger.log("Clean and stem");

    // Named entities must be processed together
    namedEntitiesProcessor.processNamedEntities(stemmingEnabled);
    executionTimeLogger.log("Named entities");
  }

  protected void setStemmingEnabled(final boolean stemmingEnabled) {
    this.stemmingEnabled = stemmingEnabled;
  }

  private void rebuildStatistics() {
    for (final Document document : documentsProcessingOutput.getDocuments().values()) {
      statisticsToBeRebuilt.putValue(document.getSourceId(), document.getPublishedDate(), true);
    }
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

}
