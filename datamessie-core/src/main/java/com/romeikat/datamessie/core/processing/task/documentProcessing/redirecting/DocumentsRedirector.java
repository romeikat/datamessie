package com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.service.download.DownloadSession;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.DisjointSet;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.core.base.util.ManyToOne;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.comparator.MasterDocumentWithDownloadsComparator;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsForUrlsAndSourceCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDocumentIdsWithEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetDownloadsPerDocumentIdCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentProcessingOutputCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentsProcessingOutputCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.RedirectCallback;
import jersey.repackaged.com.google.common.collect.Maps;

public class DocumentsRedirector {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsRedirector.class);

  private final CollectionUtil collectionUtil;
  private final SessionFactory sessionFactory;
  private final Double processingParallelismFactor;

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  private final RedirectCallback redirectCallback;
  private final GetDownloadsPerDocumentIdCallback getDownloadsPerDocumentIdCallback;
  private final GetDocumentIdsWithEntitiesCallback getDocumentIdsWithEntitiesCallback;
  private final GetDocumentIdsForUrlsAndSourceCallback getDocumentIdsForUrlsAndSourceCallback;
  private final PersistDocumentProcessingOutputCallback persistDocumentProcessingOutputCallback;
  private final PersistDocumentsProcessingOutputCallback persistDocumentsProcessingOutputCallback;

  private final String userAgent;
  private final int timeout;

  public DocumentsRedirector(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput,
      final RedirectCallback redirectCallback,
      final GetDownloadsPerDocumentIdCallback getDownloadsPerDocumentIdCallback,
      final GetDocumentIdsWithEntitiesCallback getDocumentIdsWithEntitiesCallback,
      final GetDocumentIdsForUrlsAndSourceCallback getDocumentIdsForUrlsAndSourceCallback,
      final PersistDocumentProcessingOutputCallback persistDocumentProcessingOutputCallback,
      final PersistDocumentsProcessingOutputCallback persistDocumentsProcessingOutputCallback,
      final ApplicationContext ctx) {
    collectionUtil = ctx.getBean(CollectionUtil.class);
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));

    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;

    this.redirectCallback = redirectCallback;
    this.getDownloadsPerDocumentIdCallback = getDownloadsPerDocumentIdCallback;
    this.getDocumentIdsWithEntitiesCallback = getDocumentIdsWithEntitiesCallback;
    this.getDocumentIdsForUrlsAndSourceCallback = getDocumentIdsForUrlsAndSourceCallback;
    this.persistDocumentProcessingOutputCallback = persistDocumentProcessingOutputCallback;
    this.persistDocumentsProcessingOutputCallback = persistDocumentsProcessingOutputCallback;

    userAgent = SpringUtil.getPropertyValue(ctx, "crawling.userAgent");
    timeout = Integer.parseInt(SpringUtil.getPropertyValue(ctx, "crawling.timeout"));
  }

  /**
   * Performs the redirection for the documents in {@code documentsProcessingInput}.
   *
   * Depending on the result, {@code documentsProcessingInput} and {@code documentsProcessingInput}
   * are modified as follows.
   * <ul>
   * <li>Documents whose state is not {@code DocumentProcessingState.DOWNLOADED} are ignored.</li>
   * <li>If no redirection is found, the state of the document is set to
   * {@code DocumentProcessingState.REDIRECTED}. Hence, the document is added to the
   * {@code documentsProcessingOutput}.</li>
   * <li>If a redirection is found and the download of the redirected URL succeeded, the state of
   * the document is set to {@code DocumentProcessingState.REDIRECTED}. The properties of the
   * document ({@code url}, {@code statusCode}, and {@code downloaded}) and raw content
   * ({@code content}) are updated. Hence, both are added to the {@code documentsProcessingOutput}.
   * Redirections can also result in the merging of documents. If so, the respective documents and
   * downloads are updated, and hence added to the {@code documentsProcessingOutput}.</li>
   * <li>If a redirection is found for a document but the download of the redirected URL failed, the
   * document is removed from the {@code documentsProcessingInput}. Its state is set to
   * {@code DocumentProcessingState.REDIRECTING_ERROR}. Hence, the document is added to the
   * {@code documentsProcessingOutput}. Also, empty cleaned content, stemmed content, and named
   * entity occurrences are added to the {@code documentsProcessingOutput}.</li>
   * <li>If redirecting fails for another reason, the document is removed from the
   * {@code documentsProcessingInput}. Its state is set to
   * {@code DocumentProcessingState.TECHNICAL_ERROR}. Hence, the document is added to the
   * {@code documentsProcessingOutput}. Also, empty cleaned content, stemmed content, and named
   * entity occurrences are added to the {@code documentsProcessingOutput}.</li>
   * </ul>
   *
   * Sources are processed in parallel. Documents within a source are processed one after another,
   * due to possible merges as a result of redirections.
   *
   */
  public void redirectDocuments() {
    // Do the redirection in parallel per source
    final Multimap<Long, Document> sourceId2Documents =
        filterAndPartitionDocumentsPerSourceId(documentsProcessingInput);
    final Set<Long> sourceIds = sourceId2Documents.keySet();
    new ParallelProcessing<Long>(sessionFactory, sourceIds, processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Long sourceId) {
        final Collection<Document> documents = sourceId2Documents.get(sourceId);
        final String cookie = documentsProcessingInput.getCookie(sourceId);
        redirectDocuments(sessionProvider, documents, sourceId, cookie);
      }
    };
  }

  private Multimap<Long, Document> filterAndPartitionDocumentsPerSourceId(
      final DocumentsProcessingInput documentsProcessingInput) {
    final Multimap<Long, Document> result = HashMultimap.create();

    for (final Document document : documentsProcessingInput.getDocuments()) {
      if (document.getState() != DocumentProcessingState.DOWNLOADED) {
        continue;
      }

      final long sourceId = documentsProcessingInput.getSource(document.getId()).getId();
      result.put(sourceId, document);
    }

    return result;
  }

  private void redirectDocuments(final HibernateSessionProvider sessionProvider,
      final Collection<Document> documents, final long sourceId, final String cookie) {
    // Download redirected URLs
    final DownloadSession downloadSession = DownloadSession.create(userAgent, timeout);
    downloadSession.addCookie(cookie);
    final Map<Long, DocumentRedirectingResult> documentId2DocumentRedirectingResult =
        downloadRedirectedUrls(documents, downloadSession);
    downloadSession.close();

    // Apply redirecting results
    applyRedirectingResults(documents, documentId2DocumentRedirectingResult);

    // Determine relevant document IDs for merging documents
    final Collection<Long> documentIds = determineRelevantDocumentIds(sessionProvider, sourceId,
        documentId2DocumentRedirectingResult);

    // Load downloads with document for all relevant document IDs:
    final Multimap<Long, Download> relevantDownloads = getDownloadsPerDocumentIdCallback
        .getDownloadIdsWithEntities(sessionProvider.getStatelessSession(), documentIds);
    final Map<Long, Document> relevantDocuments = Maps.newHashMap(getDocumentIdsWithEntitiesCallback
        .getDocumentIdsWithEntities(sessionProvider.getStatelessSession(), documentIds));
    // Prefer output cache
    final BiFunction<Long, Document, Document> replacingWithOutputDocumentFunction =
        new BiFunction<Long, Document, Document>() {
          @Override
          public Document apply(final Long documentId, final Document document) {
            final Document outputDocument = documentsProcessingOutput.getDocument(documentId);
            return outputDocument != null ? outputDocument : document;
          }
        };
    relevantDocuments.replaceAll(replacingWithOutputDocumentFunction);
    final ManyToOne<DownloadEntry, DocumentEntry> downloadsWithDocument =
        getDownloadsWithDocument(relevantDownloads, relevantDocuments);

    // Create missing downloads and merge overlapping documents
    final Collection<? extends Collection<Document>> overlappingDocuments = createUrUpdateDownloads(
        documentId2DocumentRedirectingResult, downloadsWithDocument, relevantDocuments, sourceId);
    mergeOverlappingDocuments(overlappingDocuments, downloadsWithDocument, relevantDownloads,
        relevantDocuments);

    persistProperResults(documents);
  }

  private Map<Long, DocumentRedirectingResult> downloadRedirectedUrls(
      final Collection<Document> documents, final DownloadSession downloadSession) {
    final ConcurrentMap<Long, DocumentRedirectingResult> documentId2DocumentRedirectingResult =
        new ConcurrentHashMap<Long, DocumentRedirectingResult>(documents.size());
    new ParallelProcessing<Document>(null, documents, processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Document document) {
        try {
          final DocumentRedirectingResult documentRedirectingResult =
              downloadRedirectedUrl(document, downloadSession);
          documentId2DocumentRedirectingResult.put(document.getId(), documentRedirectingResult);
        } catch (final Exception e) {
          final String msg = String.format("Could not redirect document %s", document.getId());
          LOG.error(msg, e);

          document.setState(DocumentProcessingState.TECHNICAL_ERROR);

          documentsProcessingInput.removeDocument(document);

          outputEmptyResults(document);
        }
      }
    };
    return documentId2DocumentRedirectingResult;
  }

  private DocumentRedirectingResult downloadRedirectedUrl(final Document document,
      final DownloadSession downloadSession) {
    final RawContent rawContent = documentsProcessingInput.getRawContent(document.getId());

    // Determine active rules
    final LocalDate downloadDate = document.getDownloaded().toLocalDate();
    final List<RedirectingRule> activeRedirectingRules =
        documentsProcessingInput.getActiveRedirectingRules(document, downloadDate);

    // Redirect
    final DocumentRedirectingResult documentRedirectingResult =
        redirectCallback.redirect(document, rawContent, activeRedirectingRules, downloadSession);
    return documentRedirectingResult;
  }

  private void applyRedirectingResults(final Collection<Document> documents,
      final Map<Long, DocumentRedirectingResult> documentId2DocumentRedirectingResult) {
    for (final Document document : documents) {
      final DocumentRedirectingResult documentRedirectingResult =
          documentId2DocumentRedirectingResult.get(document.getId());
      try {
        applyRedirectingResult(document, documentRedirectingResult);
      } catch (final Exception e) {
        final String msg = String.format("Could not redirect document %s", document.getId());
        LOG.error(msg, e);

        document.setState(DocumentProcessingState.TECHNICAL_ERROR);

        documentsProcessingInput.removeDocument(document);

        outputEmptyResults(document);
      }
    }
  }

  private void applyRedirectingResult(final Document document,
      final DocumentRedirectingResult documentRedirectingResult) {
    final boolean wasRedirectingUrlFound =
        StringUtils.isNotBlank(documentRedirectingResult.getRedirectedUrl());
    final boolean downloadSuccess = documentRedirectingResult.getRedirectedDownloadResult() != null
        && documentRedirectingResult.getRedirectedDownloadResult().getContent() != null;

    // No URL for redirection was found
    if (!wasRedirectingUrlFound) {
      document.setState(DocumentProcessingState.REDIRECTED);

      documentsProcessingOutput.putDocument(document);
    }
    // Redirection download was successful
    else if (downloadSuccess) {
      final RawContent rawContent = documentsProcessingInput.getRawContent(document.getId());
      updateDocumentAndRawContent(document, rawContent, documentRedirectingResult);

      document.setState(DocumentProcessingState.REDIRECTED);

      documentsProcessingOutput.putDocument(document);
      documentsProcessingOutput.putRawContent(rawContent);
    }
    // Redirection download failed
    else {
      document.setState(DocumentProcessingState.REDIRECTING_ERROR);

      documentsProcessingInput.removeDocument(document);

      outputEmptyResults(document);
    }
  }

  private void updateDocumentAndRawContent(final Document document, final RawContent rawContent,
      final DocumentRedirectingResult documentRedirectingResult) {
    final String url = documentRedirectingResult.getRedirectedUrl();
    final LocalDateTime downloaded =
        documentRedirectingResult.getRedirectedDownloadResult().getDownloaded();
    final Integer statusCode =
        documentRedirectingResult.getRedirectedDownloadResult().getStatusCode();
    final String content = documentRedirectingResult.getRedirectedDownloadResult().getContent();

    // Update document
    document.setUrl(url);
    document.setDownloaded(downloaded);
    document.setStatusCode(statusCode);

    // Update raw content
    rawContent.setContent(content);
  }

  private Set<Long> determineRelevantDocumentIds(final HibernateSessionProvider sessionProvider,
      final Long sourceId,
      final Map<Long, DocumentRedirectingResult> documentId2DocumentRedirectingResult) {
    // Determine existing document IDs for redirected URLs
    final Set<String> redirectedUrls = Sets.newHashSet();
    for (final DocumentRedirectingResult documentRedirectingResult : documentId2DocumentRedirectingResult
        .values()) {
      final String redirectedUrl = documentRedirectingResult.getRedirectedUrl();
      final String redirectedOriginalUrl =
          documentRedirectingResult.getRedirectedDownloadResult() == null ? null
              : documentRedirectingResult.getRedirectedDownloadResult().getOriginalUrl();

      if (StringUtils.isNotBlank(redirectedUrl)) {
        redirectedUrls.add(redirectedUrl);
      }
      if (StringUtils.isNotBlank(redirectedOriginalUrl)) {
        redirectedUrls.add(redirectedOriginalUrl);
      }
    }
    final Collection<Long> existingDocumentIds =
        getDocumentIdsForUrlsAndSourceCallback.getDocumentIdsForUrlsAndSource(
            sessionProvider.getStatelessSession(), redirectedUrls, sourceId);

    // Determine processing document IDs
    final Collection<Long> processingDocumentIds = documentId2DocumentRedirectingResult.keySet();

    // Determine all relevant document IDs (= redirected documents + processing documents):
    final Set<Long> allDocumentIds =
        Sets.newHashSetWithExpectedSize(existingDocumentIds.size() + processingDocumentIds.size());
    allDocumentIds.addAll(existingDocumentIds);
    allDocumentIds.addAll(processingDocumentIds);
    return allDocumentIds;
  }

  private ManyToOne<DownloadEntry, DocumentEntry> getDownloadsWithDocument(
      final Multimap<Long, Download> downloads, final Map<Long, Document> documents) {
    final ManyToOne<DownloadEntry, DocumentEntry> result =
        new ManyToOne<DownloadEntry, DocumentEntry>();

    // Combine
    for (final Entry<Long, Download> entry : downloads.entries()) {
      final long documentId = entry.getKey();
      final Download download = entry.getValue();

      final Document document = documents.get(documentId);
      if (document == null) {
        LOG.warn("No document found for download {}", download.getId());
        continue;
      }

      final DownloadEntry downloadEntry = new DownloadEntry(download);
      final DocumentEntry documentEntry = new DocumentEntry(document);
      result.put(downloadEntry, documentEntry);
    }

    return result;
  }

  private Collection<? extends Collection<Document>> createUrUpdateDownloads(
      final Map<Long, DocumentRedirectingResult> documentId2DocumentRedirectingResult,
      final ManyToOne<DownloadEntry, DocumentEntry> downloadsWithDocument,
      final Map<Long, Document> documents, final long sourceId) {
    final DisjointSet<Document> documentsToBeMerged = new DisjointSet<Document>(documents.values());

    // Process all redirecting results
    for (final Entry<Long, DocumentRedirectingResult> entry : documentId2DocumentRedirectingResult
        .entrySet()) {
      final long documentId = entry.getKey();
      final DocumentRedirectingResult documentRedirectingResult = entry.getValue();

      // Prepare new download/s
      final String redirectedUrl = documentRedirectingResult.getRedirectedUrl();
      final String redirectedOriginalUrl =
          documentRedirectingResult.getRedirectedDownloadResult() == null ? null
              : documentRedirectingResult.getRedirectedDownloadResult().getOriginalUrl();
      final boolean downloadSuccess =
          documentRedirectingResult.getRedirectedDownloadResult() != null
              && documentRedirectingResult.getRedirectedDownloadResult().getContent() != null;

      // Create or update the two downloads
      if (StringUtils.isNotBlank(redirectedUrl)) {
        createUrUpdateDownload(downloadsWithDocument, documents, redirectedUrl, sourceId,
            documentId, downloadSuccess, documentsToBeMerged);
      }
      if (StringUtils.isNotBlank(redirectedOriginalUrl)) {
        createUrUpdateDownload(downloadsWithDocument, documents, redirectedOriginalUrl, sourceId,
            documentId, downloadSuccess, documentsToBeMerged);
      }
    }

    // Determine documents to me merged
    final Collection<? extends Collection<Document>> subsets = documentsToBeMerged.getSubsets();
    final Collection<? extends Collection<Document>> subsetsWithAtLeastTwoDocuments =
        subsets.stream().filter(s -> s.size() > 1).collect(Collectors.toSet());
    return subsetsWithAtLeastTwoDocuments;
  }

  private void createUrUpdateDownload(
      final ManyToOne<DownloadEntry, DocumentEntry> downloadsWithDocument,
      final Map<Long, Document> documents, final String url, final long sourceId,
      final long documentId, final boolean downloadSuccess,
      final DisjointSet<Document> documentsToBeMerged) {
    // Prepare new download
    final Download possiblyNewDownload = new Download(0, sourceId, documentId, downloadSuccess);
    possiblyNewDownload.setUrl(url);
    final DownloadEntry possiblyNewDownloadEntry = new DownloadEntry(possiblyNewDownload);

    final Document document = documents.get(documentId);
    final DocumentEntry documentEntry = new DocumentEntry(document);

    // Check whether download already exists
    final DocumentEntry existingDocumentEntry =
        downloadsWithDocument.getValue(possiblyNewDownloadEntry);

    // Download does not yet exist => create
    if (existingDocumentEntry == null) {
      downloadsWithDocument.put(possiblyNewDownloadEntry, documentEntry);
      documentsProcessingOutput.putDownload(possiblyNewDownload);
    }
    // Download already exists
    else {
      final Document existingDocument = existingDocumentEntry.getDocument();

      // Update success, if necessary
      final Download existingDownload =
          downloadsWithDocument.getEqualKey(possiblyNewDownloadEntry).getDowload();
      if (!existingDownload.getSuccess() && downloadSuccess) {
        existingDownload.setSuccess(downloadSuccess);
        documentsProcessingOutput.putDownload(existingDownload);
      }

      // Assigned to a different document => documents to be merged
      if (document != existingDocument) {
        documentsToBeMerged.union(document, existingDocument);
      }
    }
  }

  private void mergeOverlappingDocuments(
      final Collection<? extends Collection<Document>> overlappingDocuments,
      final ManyToOne<DownloadEntry, DocumentEntry> downloadsWithDocument,
      final Multimap<Long, Download> relevantDownloads,
      final Map<Long, Document> relevantDocuments) {
    // Prepare data structure for merge
    final Map<Long, DocumentWithDownloads> documentsWithDownloads = Maps.newHashMap();
    for (final Collection<Document> documents : overlappingDocuments) {
      for (final Document document : documents) {
        final DocumentWithDownloads documentWithDownloads =
            new DocumentWithDownloads(document.getId(), document.getState());

        final DocumentEntry documentEntry = new DocumentEntry(document);
        final Collection<DownloadEntry> downloadEntries =
            downloadsWithDocument.getKeys(documentEntry);
        final Collection<Download> downloads =
            downloadEntries.stream().map(dE -> dE.getDowload()).collect(Collectors.toList());
        documentWithDownloads.addDownloads(downloads);

        documentsWithDownloads.put(document.getId(), documentWithDownloads);
      }
    }

    // Merge documents
    for (final Collection<Document> documentsToBeMerged : overlappingDocuments) {
      mergeDocuments(documentsToBeMerged, documentsWithDownloads, relevantDownloads,
          relevantDocuments);
    }
  }

  private void mergeDocuments(final Collection<Document> documentsToBeMerged,
      final Map<Long, DocumentWithDownloads> documentsWithDownloads,
      final Multimap<Long, Download> relevantDownloads,
      final Map<Long, Document> relevantDocuments) {
    final Collection<Long> documentsToBeMergedIds =
        documentsToBeMerged.stream().map(d -> d.getId()).collect(Collectors.toSet());
    final Set<DocumentWithDownloads> documentsToBeMergedWithDownloads = documentsWithDownloads
        .entrySet().stream().filter(e -> documentsToBeMergedIds.contains(e.getKey()))
        .map(e -> e.getValue()).collect(Collectors.toSet());

    // Divide into master and slaves
    final DocumentWithDownloads master = chooseMaster(documentsToBeMergedWithDownloads);
    final Collection<DocumentWithDownloads> slaves =
        collectionUtil.getOthers(documentsToBeMergedWithDownloads, master);

    // Merge slaves into master
    for (final DocumentWithDownloads slave : slaves) {
      mergeSlaveIntoMaster(slave, master, relevantDownloads, relevantDocuments);
    }
  }

  public DocumentWithDownloads chooseMaster(
      final Collection<DocumentWithDownloads> documentsWithDownloads) {
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

  private void mergeSlaveIntoMaster(final DocumentWithDownloads slave,
      final DocumentWithDownloads master, final Multimap<Long, Download> relevantDownloads,
      final Map<Long, Document> relevantDocuments) {
    // Reassign downloads from slave to master
    final Collection<Long> downloadIds = slave.getDownloadIds();
    for (final long downloadId : downloadIds) {
      final Collection<Download> downloads = relevantDownloads.get(downloadId);
      for (final Download download : downloads) {
        final boolean documentIdChanged = download.getDocumentId() != master.getDocumentId();
        if (documentIdChanged) {
          download.setDocumentId(master.getDocumentId());
          documentsProcessingOutput.putDownload(download);
        }
      }
    }

    // Mark slave document to be deleted
    final Document document = relevantDocuments.get(slave.getDocumentId());
    document.setState(DocumentProcessingState.TO_BE_DELETED);
    documentsProcessingOutput.putDocument(document);
    LOG.info("Document {} to be deleted", document.getId());
  }

  private void persistProperResults(final Collection<Document> documents) {
    final Collection<RawContent> rawContents = documentsProcessingOutput.getRawContents(documents);
    final Collection<Download> downloads = documentsProcessingOutput.getDownloads(documents);

    persistDocumentsProcessingOutputCallback.persistDocumentsProcessingOutput(documents, downloads,
        rawContents, null, null);
  }

  private void outputEmptyResults(final Document document) {
    final CleanedContent cleanedContent = new CleanedContent(document.getId(), "");
    final StemmedContent stemmedContent = new StemmedContent(document.getId(), "");
    final List<NamedEntityOccurrence> namedEntityOccurrences = Collections.emptyList();

    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putCleanedContent(cleanedContent);
    documentsProcessingOutput.putStemmedContent(stemmedContent);
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), namedEntityOccurrences);

    persistEmptyResults(document, cleanedContent, stemmedContent, namedEntityOccurrences);
  }

  private void persistEmptyResults(final Document document, final CleanedContent cleanedContent,
      final StemmedContent stemmedContent,
      final List<NamedEntityOccurrence> namedEntityOccurrences) {
    persistDocumentProcessingOutputCallback.persistDocumentsProcessingOutput(document,
        cleanedContent, stemmedContent, namedEntityOccurrences);
  }

}
