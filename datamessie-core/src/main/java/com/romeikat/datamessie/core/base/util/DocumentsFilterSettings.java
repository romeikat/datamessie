package com.romeikat.datamessie.core.base.util;

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

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.romeikat.datamessie.core.base.app.shared.IFullTextSearcher;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentsFilterSettings implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  private Long projectId;
  private Long sourceId;
  private Boolean sourceVisible;
  private Collection<Long> sourceTypeIds;
  private Long crawlingId;
  private LocalDate fromDate;
  private LocalDate toDate;
  private String cleanedContent;
  private Collection<DocumentProcessingState> states;
  private Collection<Long> documentIds;

  public DocumentsFilterSettings() {}

  public DocumentsFilterSettings(final Long projectId, final Long sourceId,
      final Boolean sourceVisible, final Collection<Long> sourceTypeIds, final Long crawlingId,
      final LocalDate fromDate, final LocalDate toDate, final String cleanedContent,
      final Collection<DocumentProcessingState> states, final Collection<Long> documentIds) {
    this.projectId = projectId;
    this.sourceId = sourceId;
    this.sourceVisible = sourceVisible;
    this.sourceTypeIds = sourceTypeIds;
    this.crawlingId = crawlingId;
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.cleanedContent = cleanedContent;
    this.states = states;
    this.documentIds = documentIds;
  }

  public boolean isEmpty() {
    return projectId == null && sourceId == null && sourceVisible == null && sourceTypeIds == null
        && crawlingId == null && fromDate == null && toDate == null && cleanedContent == null
        && states == null && documentIds == null;
  }

  public Long getProjectId() {
    return projectId;
  }

  public DocumentsFilterSettings setProjectId(final Long projectId) {
    this.projectId = projectId;
    return this;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public DocumentsFilterSettings setSourceId(final Long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  public Boolean getSourceVisible() {
    return sourceVisible;
  }

  public DocumentsFilterSettings setSourceVisible(final Boolean sourceVisible) {
    this.sourceVisible = sourceVisible;
    return this;
  }

  public Collection<Long> getSourceTypeIds() {
    return sourceTypeIds;
  }

  public DocumentsFilterSettings setSourceTypeIds(final Collection<Long> sourceTypeId) {
    sourceTypeIds = sourceTypeId;
    return this;
  }

  public Long getCrawlingId() {
    return crawlingId;
  }

  public DocumentsFilterSettings setCrawlingId(final Long crawlingId) {
    this.crawlingId = crawlingId;
    return this;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public DocumentsFilterSettings setFromDate(final LocalDate fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public DocumentsFilterSettings setToDate(final LocalDate toDate) {
    this.toDate = toDate;
    return this;
  }

  public String getCleanedContent() {
    return cleanedContent;
  }

  public DocumentsFilterSettings setCleanedContent(final String cleanedContent) {
    this.cleanedContent = cleanedContent;
    return this;
  }

  public Collection<DocumentProcessingState> getStates() {
    return states;
  }

  public DocumentsFilterSettings setState(final DocumentProcessingState newState) {
    states = new ArrayList<DocumentProcessingState>(1);
    states.add(newState);
    return this;
  }

  public DocumentsFilterSettings setStates(final Collection<DocumentProcessingState> newStates) {
    if (newStates == null) {
      states = null;
    } else {
      states = new ArrayList<DocumentProcessingState>(newStates.size());
      states.addAll(newStates);
    }
    return this;
  }

  public DocumentsFilterSettings setStates(final DocumentProcessingState... newStates) {
    if (newStates == null) {
      states = null;
    } else {
      states = new ArrayList<DocumentProcessingState>(newStates.length);
      for (final DocumentProcessingState newState : newStates) {
        states.add(newState);
      }
    }
    return this;
  }

  public Collection<Long> getDocumentIds() {
    return documentIds;
  }

  public DocumentsFilterSettings setDocumentIds(final Collection<Long> documentIds) {
    this.documentIds = documentIds;
    return this;
  }

  public DocumentsFilterSettings restrictToDocumentIds(final Collection<Long> documentIds) {
    if (CollectionUtils.isEmpty(this.documentIds)) {
      this.documentIds = documentIds;
    }

    else {
      this.documentIds.retainAll(documentIds);
    }

    return this;
  }

  public void transformCleanedContentIntoDocumentIds(final SharedBeanProvider sharedBeanProvider) {
    // Modify in order to determine document IDs only once
    final String luceneQueryString = cleanedContent;
    if (StringUtils.isEmpty(luceneQueryString)) {
      return;
    }

    final FullTextResult fullTextResult = sharedBeanProvider.getSharedBean(IFullTextSearcher.class)
        .searchForCleanedContent(luceneQueryString);
    final Collection<Long> documentIds = fullTextResult.getIds();

    restrictToDocumentIds(documentIds);
    cleanedContent = null;
  }

  @Override
  public DocumentsFilterSettings clone() {
    return new DocumentsFilterSettings(projectId, sourceId, sourceVisible, sourceTypeIds,
        crawlingId, fromDate, toDate, cleanedContent, states, documentIds);
  }

}
