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
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.hibernate.search.annotations.Analyzer;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithIdAndVersion;
import com.romeikat.datamessie.core.processing.service.fulltext.query.FullTextIndexingAnalyzer;
import com.romeikat.datamessie.model.core.RawContent;

@Entity
@Table(name = RawContentImpl.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "rawContent_id_version",
        columnNames = {"document_id", "version"}))
@Analyzer(impl = FullTextIndexingAnalyzer.class)
public class RawContentImpl extends AbstractEntityWithIdAndVersion implements RawContent {

  public static final String TABLE_NAME = "content";

  private long documentId;

  private String content;

  public RawContentImpl() {}

  public RawContentImpl(final long documentId, final String content) {
    this.documentId = documentId;
    this.content = content;
  }

  @Override
  @Id
  @Column(name = "document_id", nullable = false)
  public long getDocumentId() {
    return documentId;
  }

  @Override
  public RawContent setDocumentId(final long documentId) {
    this.documentId = documentId;
    return this;
  }

  @Override
  @Lob
  @Column(name = "rawContent", nullable = false)
  public String getContent() {
    return content;
  }

  @Override
  public RawContent setContent(final String content) {
    this.content = content;
    return this;
  }

  @Override
  @Transient
  public long getId() {
    return getDocumentId();
  }

  @Override
  public void setId(final long id) {
    setDocumentId(id);
  }

}
