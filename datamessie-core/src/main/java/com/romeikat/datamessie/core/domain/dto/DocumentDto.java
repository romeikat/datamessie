package com.romeikat.datamessie.core.domain.dto;

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
import java.time.LocalDateTime;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public class DocumentDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROTOCOL1 = "http://";
  private static final String PROTOCOL2 = "https://";

  private Long id;

  private String title;

  private String stemmedTitle;

  private String url;

  private String description;

  private String stemmedDescription;

  private LocalDateTime published;

  private LocalDateTime downloaded;

  private Integer statusCode;

  private DocumentProcessingState state;

  private String rawContent;

  private String cleanedContent;

  private String stemmedContent;

  private String namedEntities;

  private Long sourceId;

  private String sourceName;

  private String sourceUrl;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getStemmedTitle() {
    return stemmedTitle;
  }

  public void setStemmedTitle(final String stemmedTitle) {
    this.stemmedTitle = stemmedTitle;
  }

  public String getUrl() {
    if (url != null && !url.startsWith(PROTOCOL1) && !url.startsWith(PROTOCOL2)) {
      return PROTOCOL1 + url;
    }
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getStemmedDescription() {
    return stemmedDescription;
  }

  public void setStemmedDescription(final String stemmedDescription) {
    this.stemmedDescription = stemmedDescription;
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public void setDownloaded(final LocalDateTime downloaded) {
    this.downloaded = downloaded;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public void setPublished(final LocalDateTime published) {
    this.published = published;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(final Integer statusCode) {
    this.statusCode = statusCode;
  }

  public DocumentProcessingState getState() {
    return state;
  }

  public void setState(final DocumentProcessingState state) {
    this.state = state;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(final String sourceName) {
    this.sourceName = sourceName;
  }

  public String getRawContent() {
    return rawContent;
  }

  public void setRawContent(final String rawContent) {
    this.rawContent = rawContent;
  }

  public String getCleanedContent() {
    return cleanedContent;
  }

  public void setCleanedContent(final String cleanedContent) {
    this.cleanedContent = cleanedContent;
  }

  public String getStemmedContent() {
    return stemmedContent;
  }

  public void setStemmedContent(final String stemmedContent) {
    this.stemmedContent = stemmedContent;
  }

  public String getNamedEntities() {
    return namedEntities;
  }

  public void setNamedEntities(final String namedEntities) {
    this.namedEntities = namedEntities;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public void setSourceId(final Long sourceId) {
    this.sourceId = sourceId;
  }

  public String getSourceUrl() {
    if (sourceUrl != null && !sourceUrl.startsWith(PROTOCOL1) && !sourceUrl.startsWith(PROTOCOL2)) {
      return PROTOCOL1 + sourceUrl;
    }
    return sourceUrl;
  }

  public void setSourceUrl(final String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

}
