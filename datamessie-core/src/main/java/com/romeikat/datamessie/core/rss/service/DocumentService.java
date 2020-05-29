package com.romeikat.datamessie.core.rss.service;

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
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Service("rssDocumentService")
public class DocumentService extends com.romeikat.datamessie.core.base.service.DocumentService {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentService.class);

  @Autowired
  private DocumentDao documentDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Autowired
  @Qualifier("namedEntityOccurrenceDao")
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  public Document createDocument(final StatelessSession statelessSession, final String title,
      final String url, final String description, final LocalDateTime published,
      final LocalDateTime downloaded, final DocumentProcessingState state, final Integer statusCode,
      final Long crawlingId, final Long sourceId) {
    // Create
    final Document document = new Document();
    document.setTitle(title);
    document.setUrl(url);
    document.setDescription(description);
    document.setPublished(published);
    document.setDownloaded(downloaded);
    document.setState(state);
    document.setStatusCode(statusCode);
    // Associate
    document.setCrawlingId(crawlingId);
    document.setSourceId(sourceId);
    // Insert
    documentDao.insert(statelessSession, document);
    return document;
  }

  public RawContent createOrUpdateContent(final StatelessSession statelessSession,
      final String content, final long documentId) {
    RawContent rawContent = rawContentDao.getEntity(statelessSession, documentId);

    // Create
    if (rawContent == null) {
      rawContent = new RawContent(documentId, content);
      rawContentDao.insert(statelessSession, rawContent);
    }
    // Update
    else {
      rawContent.setDocumentId(documentId);
      if (StringUtils.isNotBlank(content)) {
        rawContent.setContent(content);
      }
      rawContentDao.update(statelessSession, rawContent);
    }

    return rawContent;
  }

  public void updateDocument(final StatelessSession statelessSession, final Document document,
      final long documentId, final String title, final String url, final String description,
      final LocalDateTime published, final LocalDateTime downloaded,
      final DocumentProcessingState state, final Integer statusCode, final long crawlingId) {
    if (document == null) {
      return;
    }

    if (StringUtils.isNotBlank(title)) {
      document.setTitle(title);
    }
    if (StringUtils.isNotBlank(url)) {
      document.setUrl(url);
    }
    if (StringUtils.isNotBlank(description)) {
      document.setDescription(description);
    }
    if (published != null) {
      document.setPublished(published);
    }
    if (downloaded != null) {
      document.setDownloaded(downloaded);
    }
    if (state != null) {
      document.setState(state);
    }
    if (statusCode != null) {
      document.setStatusCode(statusCode);
    }
    document.setCrawlingId(crawlingId);

    try {
      documentDao.update(statelessSession, document);
    } catch (final Exception e) {
      LOG.error("Could not update document {} in version {} with URL {} and state {}}",
          document.getId(), document.getVersion(), url, state);
      throw e;
    }
  }

}
