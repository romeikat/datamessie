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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Entity
@Table(name = Statistics.TABLE_NAME,
    uniqueConstraints = {@UniqueConstraint(name = "statistics_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "statistics_source_id_published_state",
            columnNames = {"source_id", "published", "state"}),
        @UniqueConstraint(name = "statistics_published_source_id_state",
            columnNames = {"published", "source_id", "state"})})
public class Statistics extends AbstractEntityWithGeneratedIdAndVersion {

  public static final String TABLE_NAME = "statistics";

  private long sourceId;

  private LocalDate published;

  private DocumentProcessingState state;

  private long documents;

  public Statistics() {}

  public Statistics(final long id, final long sourceId, final LocalDate published, final DocumentProcessingState state,
      final long documents) {
    super(id);
    this.sourceId = sourceId;
    this.published = published;
    this.state = state;
    this.documents = documents;
  }

  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  public Statistics setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Column(nullable = false)
  public LocalDate getPublished() {
    return published;
  }

  public Statistics setPublished(final LocalDate published) {
    this.published = published;
    return this;
  }

  @Column(nullable = false)
  public DocumentProcessingState getState() {
    return state;
  }

  public void setState(final DocumentProcessingState state) {
    this.state = state;
  }

  @Column(nullable = false)
  public long getDocuments() {
    return documents;
  }

  public void setDocuments(final long documents) {
    this.documents = documents;
  }

}
