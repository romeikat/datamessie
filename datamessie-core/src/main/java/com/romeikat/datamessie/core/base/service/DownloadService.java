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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;

@Service
public class DownloadService {

  @Autowired
  private DownloadDao downloadDao;

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;

  @Autowired
  @Qualifier("documentService")
  private DocumentService documentService;

  private DownloadService() {}

  public boolean existsWithDownloadSuccess(final SharedSessionContract ssc, final String url,
      final long sourceId) {
    final Download download = downloadDao.getForUrlAndSource(ssc, url, sourceId);
    if (download == null) {
      return false;
    }

    return download.getSuccess();
  }

  public List<DocumentWithDownloads> getDocumentsWithDownloads(final SharedSessionContract ssc,
      final long sourceId, final Set<String> urls) {
    final List<DocumentWithDownloads> documentsWithDownloads =
        Lists.newArrayListWithExpectedSize(urls.size());

    final Set<Long> processedDocumentIds = Sets.newHashSetWithExpectedSize(urls.size());
    for (final String url : urls) {
      final DocumentWithDownloads documentWithDownloads =
          getDocumentWithDownloads(ssc, sourceId, url);
      if (documentWithDownloads == null) {
        continue;
      }

      final long documentId = documentWithDownloads.getDocumentId();
      if (processedDocumentIds.contains(documentId)) {
        continue;
      }

      processedDocumentIds.add(documentId);
      documentsWithDownloads.add(documentWithDownloads);
    }

    return documentsWithDownloads;
  }

  public DocumentWithDownloads getDocumentWithDownloads(final SharedSessionContract ssc,
      final long sourceId, final String url) {
    if (url == null) {
      return null;
    }

    final Download download = downloadDao.getForUrlAndSource(ssc, url, sourceId);
    if (download == null) {
      return null;
    }

    final long documentId = download.getDocumentId();
    final DocumentWithDownloads documentWithDownloads = getDocumentWithDownloads(ssc, documentId);

    return documentWithDownloads;
  }

  public DocumentWithDownloads getDocumentWithDownloads(final SharedSessionContract ssc,
      final long documentId) {
    final Document document = documentDao.getEntity(ssc, documentId);

    final DocumentWithDownloads documentWithDownloads =
        new DocumentWithDownloads(documentId, document.getState());

    final Collection<Download> downloads = downloadDao.getForDocument(ssc, documentId);
    documentWithDownloads.addDownloads(downloads);

    return documentWithDownloads;
  }

  public void insertOrUpdateDownloadForUrl(final StatelessSession statelessSession,
      final String url, final long sourceId, final long documentId, final boolean downloadSuccess) {
    if (url == null) {
      return;
    }

    Download download = downloadDao.getForUrlAndSource(statelessSession, url, sourceId);
    if (download != null) {
      download.setDocumentId(documentId);
      if (!download.getSuccess() && downloadSuccess) {
        // May only change from false to true
        download.setSuccess(downloadSuccess);
      }
      downloadDao.update(statelessSession, download);
    } else {
      download = new Download();
      download.setUrl(url);
      download.setSourceId(sourceId);
      download.setDocumentId(documentId);
      download.setSuccess(downloadSuccess);
      downloadDao.insert(statelessSession, download);
    }
  }

  public void reassignDownloadsToDocument(final StatelessSession statelessSession,
      final Collection<Long> downloadIds, final long targetDocumentId) {
    for (final long downloadId : downloadIds) {
      final Download download = downloadDao.getEntity(statelessSession, downloadId);
      if (download != null) {
        final boolean documentIdChanged = download.getDocumentId() != targetDocumentId;
        if (documentIdChanged) {
          download.setDocumentId(targetDocumentId);
          downloadDao.update(statelessSession, download);
        }
      }
    }
  }

  public void mergeSlaveDocumentsIntoMasterDocument(final long sourceId,
      final StatelessSession statelessSession, final long masterDocumentId,
      final Collection<Long> slaveDownloadIds, final Collection<Document> slaveDocuments) {
    // Reassign existing slave downloads
    reassignDownloadsToDocument(statelessSession, slaveDownloadIds, masterDocumentId);

    // Remove existing slave documents
    documentService.markDocumentsToBeDeleted(statelessSession, slaveDocuments);
  }

  public Set<Long> getDownloadIds(final Collection<DocumentWithDownloads> documentsWithDownloads) {
    final Set<Long> downloadIds = Sets.newHashSet();

    for (final DocumentWithDownloads documentWithDownloads : documentsWithDownloads) {
      downloadIds.addAll(documentWithDownloads.getDownloadIds());
    }

    return downloadIds;
  }

  public List<Document> getDocuments(final SharedSessionContract ssc,
      final Collection<DocumentWithDownloads> documentsWithDownloads) {
    final Set<Long> documentIds = getDocumentIdsForDocumentsWithDownloads(documentsWithDownloads);
    final List<Document> documents = documentDao.getEntities(ssc, documentIds);
    return documents;
  }

  private Set<Long> getDocumentIdsForDocumentsWithDownloads(
      final Collection<DocumentWithDownloads> documentsWithDownloads) {
    final Set<Long> documentIds = Sets.newHashSet();

    for (final DocumentWithDownloads documentWithDownloads : documentsWithDownloads) {
      documentIds.add(documentWithDownloads.getDocumentId());
    }

    return documentIds;
  }

}
