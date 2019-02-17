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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.mutable.MutableObject;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.StemmedContentDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Service
public class DocumentService {

  private final static Logger LOG = LoggerFactory.getLogger(DocumentService.class);

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private StemmedContentDao stemmedContentDao;

  @Autowired
  @Qualifier("crawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  public void createOrUpdateRawContent(final StatelessSession statelessSession,
      final long documentId, final String content) {
    RawContent rawContent = rawContentDao.getEntity(statelessSession, documentId);
    final String contentToBeStored = content == null ? "" : content;

    // Create or update
    if (rawContent == null) {
      rawContent = new RawContent(documentId, contentToBeStored);
      rawContentDao.insert(statelessSession, rawContent);
    } else {
      rawContent.setContent(contentToBeStored);
      rawContentDao.update(statelessSession, rawContent);
    }
  }

  public void createOrUpdateCleanedContent(final StatelessSession statelessSession,
      final long documentId, final String content) {
    CleanedContent cleanedContent = cleanedContentDao.getEntity(statelessSession, documentId);
    final String contentToBeStored = content == null ? "" : content;

    // Create or update
    if (cleanedContent == null) {
      cleanedContent = new CleanedContent(documentId, contentToBeStored);
      cleanedContentDao.insert(statelessSession, cleanedContent);
    } else {
      cleanedContent.setContent(contentToBeStored);
      cleanedContentDao.update(statelessSession, cleanedContent);
    }
  }

  public void createOrUpdateStemmedContent(final StatelessSession statelessSession,
      final long documentId, final String content) {
    StemmedContent stemmedContent = stemmedContentDao.getEntity(statelessSession, documentId);
    final String contentToBeStored = content == null ? "" : content;

    // Create or update
    if (stemmedContent == null) {
      stemmedContent = new StemmedContent(documentId, contentToBeStored);
      stemmedContentDao.insert(statelessSession, stemmedContent);
    } else {
      stemmedContent.setContent(contentToBeStored);
      stemmedContentDao.update(statelessSession, stemmedContent);
    }
  }

  public void markDocumentsToBeDeleted(final StatelessSession statelessSession,
      final Collection<Document> documents) {
    for (final Document document : documents) {
      markDocumentToBeDeleted(statelessSession, document);
    }
  }

  private void markDocumentToBeDeleted(final StatelessSession statelessSession,
      final Document document) {
    if (document == null) {
      return;
    }

    LOG.info("Document {} to be deleted", document.getId());
    document.setState(DocumentProcessingState.TO_BE_DELETED);
    documentDao.update(statelessSession, document);
  }

  public void deprocessDocumentsOfSource(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState) throws TaskCancelledException {
    // Initialize
    final TaskExecutionWork work = taskExecution.reportWorkStart(String.format(
        "Deprocessing documents of source %s to state %s", sourceId, targetState.getName()));
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        new StatisticsRebuildingSparseTable();

    // Determine minimum downloaded date
    final LocalDate minDownloadedDate = getMinDownloadedDate(statelessSession);

    // Process all download dates one after another, starting with the minimum downloaded date
    final MutableObject<LocalDate> downloadedDate = new MutableObject<LocalDate>(minDownloadedDate);
    while (downloadedDate.getValue() != null) {
      // Deprocess
      deprocessDocumentsOfSourceAndDownloadDate(statelessSession, taskExecution, sourceId,
          targetState, statisticsToBeRebuilt, downloadedDate.getValue());

      // Prepare for next iteration
      final LocalDate nextDownloadedDate = getNextDownloadedDate(downloadedDate.getValue());
      downloadedDate.setValue(nextDownloadedDate);
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

  private LocalDate getMinDownloadedDate(final StatelessSession statelessSession) {
    final LocalDateTime minDownloadedDateTime = documentDao.getMinDownloaded(statelessSession);
    if (minDownloadedDateTime == null) {
      return null;
    }

    final LocalDate minDownloadedDate = minDownloadedDateTime.toLocalDate();
    return minDownloadedDate;
  }

  private void deprocessDocumentsOfSourceAndDownloadDate(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final long sourceId,
      final DocumentProcessingState targetState,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt, final LocalDate downloadedDate)
      throws TaskCancelledException {
    final TaskExecutionWork work2 =
        taskExecution.reportWorkStart(String.format("Deprocessing documents with download date %s",
            LocalDateConverter.INSTANCE_UI.convertToString(downloadedDate)));

    // Load
    final List<Document> documents =
        documentDao.getForSourceAndDownloaded(statelessSession, sourceId, downloadedDate);

    // Deprocess
    deprocessDocuments(statelessSession, documents, targetState, statisticsToBeRebuilt);

    taskExecution.reportWorkEnd(work2);
    taskExecution.checkpoint();
  }

  private void deprocessDocuments(final StatelessSession statelessSession,
      final Collection<Document> documents, final DocumentProcessingState targetState,
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    for (final Document document : documents) {
      LOG.debug("Deprocessing document {}", document.getId());
      // Update state
      if (document != null) {
        final DocumentProcessingState oldState = document.getState();
        if (!isValidDeprocessingStep(oldState, targetState)) {
          continue;
        }
        document.setState(targetState);
        documentDao.update(statelessSession, document);
        // Rebuild statistics
        final long sourceId = document.getSourceId();
        final LocalDate publishedDate = document.getPublishedDate();
        statisticsToBeRebuilt.putValue(sourceId, publishedDate, true);
      }
    }
  }

  private boolean isValidDeprocessingStep(final DocumentProcessingState sourceState,
      final DocumentProcessingState targetState) {
    // Check target state
    if (DocumentProcessingState.getErrorStates().contains(targetState)) {
      return false;
    }
    // Check source -> target states
    switch (sourceState) {
      case DOWNLOADED:
      case DOWNLOAD_ERROR:
        return false;
      case REDIRECTED:
      case REDIRECTING_ERROR:
        return targetState == DocumentProcessingState.DOWNLOADED;
      case CLEANED:
        return targetState == DocumentProcessingState.DOWNLOADED
            || targetState == DocumentProcessingState.REDIRECTED;
      case CLEANING_ERROR:
      case STEMMED:
        return targetState == DocumentProcessingState.DOWNLOADED
            || targetState == DocumentProcessingState.REDIRECTED
            || targetState == DocumentProcessingState.CLEANED;
      case TECHNICAL_ERROR:
        return targetState == DocumentProcessingState.DOWNLOADED
            || targetState == DocumentProcessingState.REDIRECTED
            || targetState == DocumentProcessingState.CLEANED
            || targetState == DocumentProcessingState.STEMMED;
      case TO_BE_DELETED:
        return false;
    }
    // Done
    return true;
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
