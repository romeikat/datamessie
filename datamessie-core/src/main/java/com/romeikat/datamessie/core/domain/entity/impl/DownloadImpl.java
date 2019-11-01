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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.model.core.Download;

@Entity
@Table(name = DownloadImpl.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "download_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "download_url_source_id", columnNames = {"url", "source_id"})},
    indexes = @Index(name = "FK_download_document_id", columnList = "document_id"))
public class DownloadImpl extends AbstractEntityWithGeneratedIdAndVersion implements Download {

  public static final String TABLE_NAME = "download";

  private String url;

  private long sourceId;

  private long documentId;

  private boolean success;

  public DownloadImpl() {}

  public DownloadImpl(final long id, final long sourceId, final long documentId,
      final boolean success) {
    super(id);
    this.sourceId = sourceId;
    this.documentId = documentId;
    this.success = success;
  }

  @Override
  @Column(length = 511)
  public String getUrl() {
    return url;
  }

  @Override
  public Download setUrl(final String url) {
    this.url = url;
    return this;
  }

  @Override
  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  @Override
  public Download setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
  @Column(name = "document_id", nullable = false)
  public long getDocumentId() {
    return documentId;
  }

  @Override
  public Download setDocumentId(final long documentId) {
    this.documentId = documentId;
    return this;
  }

  @Override
  @Column(nullable = false)
  public boolean getSuccess() {
    return success;
  }

  @Override
  public Download setSuccess(final boolean success) {
    this.success = success;
    return this;
  }

}
