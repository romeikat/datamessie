package com.romeikat.datamessie.core.domain.entity.impl;

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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Entity
@Table(name = Document.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "document_id_version",
        columnNames = {"id", "version"}),
    indexes = {@Index(name = "document_downloaded", columnList = "downloaded"),
        @Index(name = "document_published_source_id_state",
            columnList = "published, source_id, state"),
        @Index(name = "FK_document_crawling_id", columnList = "crawling_id")})
public class Document extends AbstractEntityWithGeneratedIdAndVersion {

  public static final String TABLE_NAME = "document";

  private String title;

  private String stemmedTitle;

  private String url;

  private String description;

  private String stemmedDescription;

  private LocalDateTime published;

  private LocalDateTime downloaded;

  private DocumentProcessingState state;

  private Integer statusCode;

  private long crawlingId;

  private long sourceId;

  public Document() {}

  public Document(final long id, final long crawlingId, final long sourceId) {
    super(id);
    this.crawlingId = crawlingId;
    this.sourceId = sourceId;
  }

  @Column(length = 511)
  public String getTitle() {
    return title;
  }

  public Document setTitle(final String title) {
    this.title = title;
    return this;
  }

  @Column(length = 511)
  public String getStemmedTitle() {
    return stemmedTitle;
  }

  public Document setStemmedTitle(final String stemmedTitle) {
    this.stemmedTitle = stemmedTitle;
    return this;
  }

  @Column(length = 511)
  public String getUrl() {
    return url;
  }

  public Document setUrl(final String url) {
    this.url = url;
    return this;
  }

  @Lob
  public String getDescription() {
    return description;
  }

  public Document setDescription(final String description) {
    this.description = description;
    return this;
  }

  @Lob
  public String getStemmedDescription() {
    return stemmedDescription;
  }

  public Document setStemmedDescription(final String stemmedDescription) {
    this.stemmedDescription = stemmedDescription;
    return this;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public Document setPublished(final LocalDateTime published) {
    this.published = published;
    return this;
  }

  @Transient
  public LocalDate getPublishedDate() {
    if (published == null) {
      return null;
    }

    return published.toLocalDate();
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public Document setDownloaded(final LocalDateTime downloaded) {
    this.downloaded = downloaded;
    return this;
  }

  @Enumerated(value = EnumType.ORDINAL)
  public DocumentProcessingState getState() {
    return state;
  }

  public Document setState(final DocumentProcessingState state) {
    this.state = state;
    return this;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public Document setStatusCode(final Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  @Column(name = "crawling_id", nullable = false)
  public long getCrawlingId() {
    return crawlingId;
  }

  public Document setCrawlingId(final long crawlingId) {
    this.crawlingId = crawlingId;
    return this;
  }

  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  public Document setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

}
