package com.romeikat.datamessie.core.rss.task.rssCrawling;

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
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.service.DownloadService;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.core.base.util.comparator.MasterDocumentWithDownloadsComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.rss.service.DocumentService;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

public class DocumentCrawler {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentCrawler.class);

  private final DocumentService documentService;
  private final DocumentDao documentDao;
  private final DownloadService downloadService;
  private final CollectionUtil collectionUtil;

  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;

  public DocumentCrawler(final ApplicationContext ctx) {
    documentService = ctx.getBean("rssDocumentService", DocumentService.class);
    documentDao = ctx.getBean("documentDao", DocumentDao.class);
    downloadService = ctx.getBean(DownloadService.class);
    collectionUtil = ctx.getBean(CollectionUtil.class);

    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();
  }

  public Document performCrawling(final StatelessSession statelessSession, final String title,
      final String description, final LocalDateTime published, final DownloadResult downloadResult,
      final long crawlingId, final long sourceId) {
    // Information from download result
    final String originalUrl = downloadResult.getOriginalUrl();
    final String url = downloadResult.getUrl();
    final LocalDateTime downloaded = downloadResult.getDownloaded();
    final Integer statusCode = downloadResult.getStatusCode();
    final String content = downloadResult.getContent();
    final DocumentProcessingState state = getState(content);

    Document result = null;

    // Find existing documents for the two URLs
    final Collection<DocumentWithDownloads> existingDocumentsWithDownloads = downloadService
        .getDocumentsWithDownloads(statelessSession, sourceId, Sets.newHashSet(originalUrl, url));

    // No existing documents
    if (existingDocumentsWithDownloads.isEmpty()) {
      result = processNewDownload(crawlingId, sourceId, originalUrl, url, downloaded, statusCode,
          content, title, description, published, state, statelessSession);
    }

    // One existing document
    else if (existingDocumentsWithDownloads.size() == 1) {
      final DocumentWithDownloads masterDocumentWithDownloads =
          existingDocumentsWithDownloads.iterator().next();
      result = processRepeatedDownload(statelessSession, title, originalUrl, url, description,
          published, downloaded, state, statusCode, crawlingId, sourceId, content,
          masterDocumentWithDownloads);
    }

    // Multiple existing documents
    else {
      // Divide into master and slaves
      final DocumentWithDownloads masterDocumentWithDownloads =
          decideForExistingDocumentWithDownloads(existingDocumentsWithDownloads);
      final Collection<DocumentWithDownloads> slaveDocumentsWithDownloads =
          collectionUtil.getOthers(existingDocumentsWithDownloads, masterDocumentWithDownloads);

      // Process master
      result = processRepeatedDownload(statelessSession, title, originalUrl, url, description,
          published, downloaded, state, statusCode, crawlingId, sourceId, content,
          masterDocumentWithDownloads);

      // Process slaves
      processSlaveDocuments(statelessSession, sourceId, url, masterDocumentWithDownloads,
          slaveDocumentsWithDownloads);
    }

    return result;
  }

  private Document processNewDownload(final long crawlingId, final long sourceId,
      final String originalUrl, final String url, final LocalDateTime downloaded,
      final Integer statusCode, final String content, final String title, final String description,
      final LocalDateTime published, final DocumentProcessingState state,
      final StatelessSession statelessSession) {
    LOG.debug("Source {}: processing new download for URL {}", sourceId, url);

    // Create new document
    final Document document = documentService.createDocument(statelessSession, title, url,
        description, published, downloaded, state, statusCode, crawlingId, sourceId);
    final long documentId = document.getId();

    // Create new content
    final boolean downloadSuccess = content != null;
    if (downloadSuccess) {
      documentService.createOrUpdateContent(statelessSession, content, documentId);
    }

    // Create new downloads
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, originalUrl, sourceId,
        documentId, downloadSuccess);
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, url, sourceId, documentId,
        downloadSuccess);

    // Rebuild statistics
    if (published != null) {
      statisticsToBeRebuilt.putValue(sourceId, published.toLocalDate(), true);
    }

    return document;
  }

  private Document processRepeatedDownload(final StatelessSession statelessSession,
      final String title, final String originalUrl, final String url, final String description,
      final LocalDateTime published, final LocalDateTime downloaded,
      final DocumentProcessingState state, final Integer statusCode, final long crawlingId,
      final long sourceId, final String content,
      final DocumentWithDownloads masterDocumentWithDownloads) {
    LOG.debug("Source {}: processing repeated download for URL {}", sourceId, url);
    Document result = null;

    final long documentId = masterDocumentWithDownloads.getDocumentId();
    final boolean downloadSuccess = content != null;

    // Master download succeeded
    final boolean masterDownloadSuccess = masterDocumentWithDownloads.getSuccess();

    // Process if existing master download failed or current download succeeded
    if (!masterDownloadSuccess || downloadSuccess) {
      if (masterDownloadSuccess) {
        // Occurs if this is a slave URL that initially had failed to download, but the master URL
        // has already been downloaded successfully
        LOG.debug(
            "Ignoring slave URL {} in RSS feed of source {} as a master URL has already been downloaded",
            url, sourceId);
      }

      // Update master document
      final Document document = documentDao.getEntity(statelessSession, documentId);
      final LocalDate oldPublishedDate = document.getPublishedDate();
      final DocumentProcessingState oldState = document.getState();
      documentService.updateDocument(statelessSession, document, documentId, title, url,
          description, published, downloaded, state, statusCode, crawlingId);
      final LocalDate newPublishedDate = document.getPublishedDate();
      final DocumentProcessingState newState = document.getState();
      if (!Objects.equals(oldPublishedDate, newPublishedDate)
          || !Objects.equals(oldState, newState)) {
        statisticsToBeRebuilt.putValue(sourceId, oldPublishedDate, true);
        statisticsToBeRebuilt.putValue(sourceId, newPublishedDate, true);
      }
      result = document;

      // Create content (if successful)
      if (downloadSuccess) {
        documentService.createOrUpdateContent(statelessSession, content, documentId);
      }
    }

    // Create new downloads (if necessary)
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, originalUrl, sourceId,
        documentId, downloadSuccess);
    downloadService.insertOrUpdateDownloadForUrl(statelessSession, url, sourceId, documentId,
        downloadSuccess);

    return result;
  }

  private void processSlaveDocuments(final StatelessSession statelessSession, final long sourceId,
      final String url, final DocumentWithDownloads masterDocumentWithDownloads,
      final Collection<DocumentWithDownloads> slaveDocumentsWithDownloads) {
    LOG.debug("Source {}: processing slave documents for URL {}", sourceId, url);

    final long masterDocumentId = masterDocumentWithDownloads.getDocumentId();
    final Collection<Long> slaveDownloadIds =
        downloadService.getDownloadIds(slaveDocumentsWithDownloads);
    final Collection<Document> slaveDocuments =
        downloadService.getDocuments(statelessSession, slaveDocumentsWithDownloads);
    downloadService.mergeSlaveDocumentsIntoMasterDocument(statelessSession, sourceId,
        masterDocumentId, slaveDownloadIds, slaveDocuments);
    for (final Document slaveDocument : slaveDocuments) {
      documentDao.update(statelessSession, slaveDocument);
    }

    for (final Document slaveDocument : slaveDocuments) {
      statisticsToBeRebuilt.putValue(slaveDocument.getSourceId(), slaveDocument.getPublishedDate(),
          true);
    }
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

  private DocumentProcessingState getState(final String content) {
    if (content == null) {
      return DocumentProcessingState.DOWNLOAD_ERROR;
    }

    return DocumentProcessingState.DOWNLOADED;
  }

  public StatisticsRebuildingSparseTable getStatisticsToBeRebuilt() {
    return statisticsToBeRebuilt;
  }

}
