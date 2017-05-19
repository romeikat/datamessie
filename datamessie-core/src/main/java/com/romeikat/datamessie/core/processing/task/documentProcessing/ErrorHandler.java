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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.service.DocumentService;

public class ErrorHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

  private final Document document;
  private final boolean deleteRawContent;
  private final boolean deleteCleanedContent;
  private final boolean deleteStemmedContent;
  private final boolean deleteNamedEntityOccurrences;
  private final DocumentProcessingState newState;
  private final Collection<Long> failedDocumentIds;

  private final DocumentService documentService;
  private final NamedEntityOccurrencesUpdater namedEntityOccurrencesUpdater;
  private final DocumentDao documentDao;

  public ErrorHandler(final ApplicationContext ctx, final Document document, final boolean deleteRawContent,
      final boolean deleteCleanedContent, final boolean deleteStemmedContent,
      final boolean deleteNamedEntityOccurrences, final DocumentProcessingState newState,
      final Collection<Long> failedDocumentIds) {
    this.document = document;
    this.deleteRawContent = deleteRawContent;
    this.deleteCleanedContent = deleteCleanedContent;
    this.deleteStemmedContent = deleteStemmedContent;
    this.deleteNamedEntityOccurrences = deleteNamedEntityOccurrences;
    this.newState = newState;
    this.failedDocumentIds = failedDocumentIds;

    documentService = ctx.getBean("processingDocumentService", DocumentService.class);
    namedEntityOccurrencesUpdater = ctx.getBean(NamedEntityOccurrencesUpdater.class);
    documentDao = ctx.getBean("processingDocumentDao", DocumentDao.class);
  }

  public void handleError(final StatelessSession statelessSession, final String reason, final Exception e) {
    final StringBuilder msg = new StringBuilder();
    msg.append("An unexpected error occurred while processing document ");
    msg.append(document.getId());
    if (StringUtils.isNotBlank(reason)) {
      msg.append(": ");
      msg.append(reason);
    }
    LOG.error(msg.toString(), e);

    if (deleteRawContent) {
      documentService.createUpdateOrDeleteRawContent(statelessSession, document.getId(), null);
    }
    if (deleteCleanedContent) {
      documentService.createUpdateOrDeleteCleanedContent(statelessSession, document.getId(), null);
    }
    if (deleteStemmedContent) {
      documentService.createUpdateOrDeleteStemmedContent(statelessSession, document.getId(), null);
    }
    if (deleteNamedEntityOccurrences) {
      namedEntityOccurrencesUpdater.updateNamedEntityOccurrences(statelessSession, document.getId(),
          Collections.emptyList());
    }

    if (newState != null) {
      document.setState(DocumentProcessingState.TECHNICAL_ERROR);
      documentDao.update(statelessSession, document);
    }

    if (failedDocumentIds != null) {
      failedDocumentIds.add(document.getId());
    }
  }

}
