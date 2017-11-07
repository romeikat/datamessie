package com.romeikat.datamessie.core.processing.task.documentProcessing;

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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.service.DownloadService;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.core.base.util.comparator.MasterDocumentWithDownloadsComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import com.romeikat.datamessie.core.processing.service.DocumentService;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cache.DocumentsProcessingCache;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaner;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaningResult;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentRedirectingResult;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentRedirector;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentStemmer;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentStemmingResult;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

public class DocumentProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentProcessor.class);

  private final ApplicationContext ctx;
  private final RawContentDao rawContentDao;
  private final DocumentRedirector documentRedirector;
  private final DocumentCleaner documentCleaner;
  private final DocumentStemmer documentStemmer;
  private final DownloadService downloadService;
  private final DocumentDao documentDao;
  private final DocumentService documentService;
  private final NamedEntityOccurrencesUpdater namedEntityOccurrencesUpdater;
  private final NamedEntityCategoriesCreator namedEntityCategoriesCreator;
  private final CollectionUtil collectionUtil;

  private final DocumentsProcessingCache documentsProcessingCache;

  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;
  private final Set<Long> failedDocumentIds;

  public DocumentProcessor(final ApplicationContext ctx,
      final DocumentsProcessingCache documentsProcessingCache) {
    this.ctx = ctx;
    rawContentDao = ctx.getBean(RawContentDao.class);
    documentRedirector = ctx.getBean(DocumentRedirector.class);
    documentCleaner = ctx.getBean(DocumentCleaner.class);
    documentStemmer = ctx.getBean(DocumentStemmer.class);
    downloadService = ctx.getBean(DownloadService.class);
    documentService = ctx.getBean("processingDocumentService", DocumentService.class);
    documentDao = ctx.getBean("processingDocumentDao", DocumentDao.class);
    namedEntityOccurrencesUpdater = ctx.getBean(NamedEntityOccurrencesUpdater.class);
    namedEntityCategoriesCreator = ctx.getBean(NamedEntityCategoriesCreator.class);
    collectionUtil = ctx.getBean(CollectionUtil.class);

    this.documentsProcessingCache = documentsProcessingCache;

    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();
    failedDocumentIds = Sets.newHashSet();
  }

  public void processDocument(final StatelessSession statelessSession, final Document document) {
    LOG.debug("Processing document {}", document.getId());

    // Remember old values
    final LocalDate oldPublishedDate = document.getPublishedDate();
    final DocumentProcessingState oldState = document.getState();

    // Process
    final Document processedDocument = doProcessing(statelessSession, document);

    // Remember new values
    final LocalDate newPublishedDate = processedDocument.getPublishedDate();
    final DocumentProcessingState newState = processedDocument.getState();

    // Rebuild statistics
    if (!Objects.equals(oldPublishedDate, newPublishedDate)
        || !Objects.equals(oldState, newState)) {
      statisticsToBeRebuilt.putValue(document.getSourceId(), oldPublishedDate, true);
      statisticsToBeRebuilt.putValue(document.getSourceId(), newPublishedDate, true);
    }
  }

  public Document doProcessing(final StatelessSession statelessSession, final Document document) {
    // Get content
    final RawContent rawContent = documentsProcessingCache.getRawContent(document.getId());
    if (rawContent == null && document.getState() != DocumentProcessingState.DOWNLOAD_ERROR) {
      // A missing raw content should only occur in case of a download error
      new ErrorHandler(ctx, document, false, true, true, true,
          DocumentProcessingState.TECHNICAL_ERROR, failedDocumentIds).handleError(statelessSession,
              "No raw content found", null);
      return document;
    }

    // Determine language
    final Source source = documentsProcessingCache.getSource(document.getId());
    if (source == null) {
      new ErrorHandler(ctx, document, false, true, true, true,
          DocumentProcessingState.TECHNICAL_ERROR, failedDocumentIds).handleError(statelessSession,
              "No source found", null);
      return document;
    }
    final Language language = source.getLanguage();

    // Process document

    if (document.getState() == DocumentProcessingState.DOWNLOAD_ERROR) {
      new ErrorHandler(ctx, document, false, true, true, true, null, null)
          .handleError(statelessSession, "Downloading had failed", null);
      return document;
    }

    // Processing step 1: downloaded -> redirected / redirection error / to be deleted
    try {
      if (document.getState() == DocumentProcessingState.DOWNLOADED) {
        final DocumentRedirectingResult documentRedirectingResult =
            documentRedirector.redirect(statelessSession, document, rawContent);
        interpretRedirectingResult(statelessSession, documentRedirectingResult, document,
            rawContent);
      }
    } catch (final Exception e) {
      new ErrorHandler(ctx, document, false, true, true, true,
          DocumentProcessingState.TECHNICAL_ERROR, failedDocumentIds).handleError(statelessSession,
              "Redirecting failed", e);
      return document;
    }

    // Processing step 2: redirected -> cleaned / cleaning error
    DocumentCleaningResult documentCleaningResult = null;
    try {
      if (document.getState() == DocumentProcessingState.REDIRECTED) {
        documentCleaningResult =
            documentCleaner.clean(statelessSession, documentsProcessingCache, document, rawContent);
        interpretCleaningResult(statelessSession, documentCleaningResult, document);
      }
    } catch (final Exception e) {
      new ErrorHandler(ctx, document, false, true, true, true,
          DocumentProcessingState.TECHNICAL_ERROR, failedDocumentIds).handleError(statelessSession,
              "Cleaning failed", e);
      return document;
    }

    // Processing step 3: cleaned -> stemmed / stemming error
    List<NamedEntityDetectionDto> namedEntityDetections = null;
    try {
      if (document.getState() == DocumentProcessingState.CLEANED) {
        final String cleanedContent =
            documentCleaningResult == null ? null : documentCleaningResult.getCleanedContent();
        final DocumentStemmingResult documentStemmingResult =
            documentStemmer.stem(statelessSession, document, cleanedContent, language);
        namedEntityDetections = documentStemmingResult.getNamedEntityDetections();
        interpretStemmingResult(statelessSession, documentStemmingResult, document);
      }
    } catch (final Exception e) {
      new ErrorHandler(ctx, document, false, false, true, true,
          DocumentProcessingState.TECHNICAL_ERROR, failedDocumentIds).handleError(statelessSession,
              "Stemming failed", e);
      return document;
    }

    // Update NamedEntityOccurrences
    final Collection<NamedEntityOccurrence> namedEntityOccurrences = namedEntityOccurrencesUpdater
        .updateNamedEntityOccurrences(statelessSession, document.getId(), namedEntityDetections);
    // Create NamedEntityCategories
    namedEntityCategoriesCreator.createNamedEntityCategories(statelessSession,
        namedEntityOccurrences);

    documentDao.update(statelessSession, document);

    return document;
  }

  private void interpretRedirectingResult(final StatelessSession statelessSession,
      final DocumentRedirectingResult documentRedirectingResult, final Document document,
      final RawContent rawContent) {

    final String url = documentRedirectingResult.getRedirectedUrl();
    final boolean wasRedirectingUrlFound = StringUtils.isNotBlank(url);
    // No URL for redirection was found
    if (!wasRedirectingUrlFound) {
      document.setState(DocumentProcessingState.REDIRECTED);
      return;
    }

    // An URL for redirection was found
    final DownloadResult redirectedDownloadResult =
        documentRedirectingResult.getRedirectedDownloadResult();
    final String originalUrl = redirectedDownloadResult.getOriginalUrl();
    final LocalDateTime downloaded = redirectedDownloadResult.getDownloaded();
    final Integer statusCode = redirectedDownloadResult.getStatusCode();
    final String content = redirectedDownloadResult.getContent();
    final DocumentProcessingState state = getRedirectingState(content);
    final boolean downloadSuccess = redirectedDownloadResult.getContent() != null;

    // Find existing documents for the two redirected URLs
    // (before their downloads might be overtaken by applying the redirection)
    final Collection<DocumentWithDownloads> existingDocumentsWithDownloads =
        downloadService.getDocumentsWithDownloads(statelessSession, document.getSourceId(),
            Sets.newHashSet(originalUrl, url));

    // Apply redirection
    applyRedirection(statelessSession, document, rawContent, url, originalUrl, downloaded,
        statusCode, content, state, downloadSuccess);

    // Merge documents, if necessary
    if (!existingDocumentsWithDownloads.isEmpty()) {
      final DocumentWithDownloads documentWithDownloads =
          downloadService.getDocumentWithDownloads(statelessSession, document.getId());
      final Collection<DocumentWithDownloads> allDocumentsWithDownloads =
          getAllDocumentsWithDownloads(documentWithDownloads, existingDocumentsWithDownloads);

      // Divide into master and slaves
      final DocumentWithDownloads masterDocumentWithDownloads =
          decideForExistingDocumentWithDownloads(allDocumentsWithDownloads);
      final Collection<DocumentWithDownloads> slaveDocumentsWithDownloads =
          collectionUtil.getOthers(allDocumentsWithDownloads, masterDocumentWithDownloads);

      // Process slaves
      final long masterDocumentId = masterDocumentWithDownloads.getDocumentId();
      final Collection<Long> slaveDownloadIds =
          downloadService.getDownloadIds(slaveDocumentsWithDownloads);
      final Collection<Document> slaveDocuments =
          downloadService.getDocuments(statelessSession, slaveDocumentsWithDownloads);
      downloadService.mergeSlaveDocumentsIntoMasterDocument(document.getSourceId(),
          statelessSession, masterDocumentId, slaveDownloadIds, slaveDocuments);

      // Rebuild statistics
      for (final Document slaveDocument : slaveDocuments) {
        statisticsToBeRebuilt.putValue(slaveDocument.getSourceId(),
            slaveDocument.getPublishedDate(), true);
      }
    }

    // Cleanup
    if (document.getState() != DocumentProcessingState.REDIRECTED) {
      documentService.createUpdateOrDeleteCleanedContent(statelessSession, document.getId(), null);
      documentService.createUpdateOrDeleteStemmedContent(statelessSession, document.getId(), null);
    }
  }

  private DocumentProcessingState getRedirectingState(final String content) {
    if (content == null) {
      return DocumentProcessingState.REDIRECTING_ERROR;
    }

    return DocumentProcessingState.REDIRECTED;
  }

  private void applyRedirection(final StatelessSession statelessSession, final Document document,
      final RawContent rawContent, final String url, final String originalUrl,
      final LocalDateTime downloaded, final Integer statusCode, final String content,
      final DocumentProcessingState state, final boolean downloadSuccess) {
    // Download succeeded
    if (downloadSuccess) {
      // Update document
      documentService.updateDocument(statelessSession, document, url, downloaded, state,
          statusCode);

      // Update content
      rawContent.setContent(content);
      rawContentDao.update(statelessSession, rawContent);
    }

    // Download failed
    else {
      documentService.updateDocument(statelessSession, document, state);
    }

    // Create new downloads (if necessary)
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, originalUrl,
        document.getSourceId(), document.getId(), downloadSuccess);
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, url, document.getSourceId(),
        document.getId(), downloadSuccess);
  }

  private Collection<DocumentWithDownloads> getAllDocumentsWithDownloads(
      final DocumentWithDownloads redirectedDocumentWithDownloads,
      final Collection<DocumentWithDownloads> existingDocumentsWithDownloads) {
    final List<DocumentWithDownloads> documentsWithDownloads =
        Lists.newArrayListWithExpectedSize(1 + existingDocumentsWithDownloads.size());

    final long redirectedDocumentId = redirectedDocumentWithDownloads.getDocumentId();

    documentsWithDownloads.add(redirectedDocumentWithDownloads);
    for (final DocumentWithDownloads existingDocumentWithDownloads : existingDocumentsWithDownloads) {
      if (existingDocumentWithDownloads.getDocumentId() != redirectedDocumentId) {
        documentsWithDownloads.add(existingDocumentWithDownloads);
      }
    }

    return documentsWithDownloads;
  }

  public DocumentWithDownloads decideForExistingDocumentWithDownloads(
      final Collection<DocumentWithDownloads> documentsWithDownloads) {
    // No existing document
    if (documentsWithDownloads.isEmpty()) {
      return null;
    }

    // Order by success and lowest ID
    final List<DocumentWithDownloads> documentsWithDownloadsOrdered =
        Lists.newArrayList(documentsWithDownloads);
    Collections.sort(documentsWithDownloadsOrdered, MasterDocumentWithDownloadsComparator.INSTANCE);
    final DocumentWithDownloads first = documentsWithDownloadsOrdered.iterator().next();
    return first;
  }

  private void interpretCleaningResult(final StatelessSession statelessSession,
      final DocumentCleaningResult documentCleaningResult, final Document document) {
    // Stemmed title and description may be null, as title and description may be null,
    // but stemmed content must not be null
    final boolean wasCleaningSuccesful = documentCleaningResult.getCleanedContent() != null;
    if (wasCleaningSuccesful) {
      documentService.createUpdateOrDeleteCleanedContent(statelessSession, document.getId(),
          documentCleaningResult.getCleanedContent());
      document.setState(DocumentProcessingState.CLEANED);
    } else {
      documentService.createUpdateOrDeleteCleanedContent(statelessSession, document.getId(), null);
      document.setState(DocumentProcessingState.CLEANING_ERROR);
    }

    // Cleanup
    if (document.getState() != DocumentProcessingState.CLEANED) {
      documentService.createUpdateOrDeleteCleanedContent(statelessSession, document.getId(), null);
      documentService.createUpdateOrDeleteStemmedContent(statelessSession, document.getId(), null);
    }
  }

  private void interpretStemmingResult(final StatelessSession statelessSession,
      final DocumentStemmingResult documentStemmingResult, final Document document) {
    document.setStemmedTitle(documentStemmingResult.getStemmedTitle());
    document.setStemmedDescription(documentStemmingResult.getStemmedDescription());
    documentService.createUpdateOrDeleteStemmedContent(statelessSession, document.getId(),
        documentStemmingResult.getStemmedContent());
    document.setState(DocumentProcessingState.STEMMED);

    // No cleanup necessary as there is no state STEMMING_ERROR
  }

  public Set<Long> getFailedDocumentIds() {
    return failedDocumentIds;
  }

  public StatisticsRebuildingSparseTable getStatisticsToBeRebuilt() {
    return statisticsToBeRebuilt;
  }

}
