package com.romeikat.datamessie.core.statistics.dto;

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
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public class DocumentStatisticsDto {

  private long documentId;

  private long sourceId;

  private LocalDateTime published;

  private DocumentProcessingState state;

  public DocumentStatisticsDto() {}

  public DocumentStatisticsDto(final long documentId, final long sourceId,
      final LocalDateTime published, final DocumentProcessingState state) {
    this.documentId = documentId;
    this.sourceId = sourceId;
    this.published = published;
    this.state = state;
  }

  public long getDocumentId() {
    return documentId;
  }

  public void setDocumentId(final long documentId) {
    this.documentId = documentId;
  }

  public long getSourceId() {
    return sourceId;
  }

  public void setSourceId(final long sourceId) {
    this.sourceId = sourceId;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public LocalDate getPublishedDate() {
    if (published == null) {
      return null;
    }

    return published.toLocalDate();
  }

  public void setPublished(final LocalDateTime published) {
    this.published = published;
  }

  public DocumentProcessingState getState() {
    return state;
  }

  public void setState(final DocumentProcessingState state) {
    this.state = state;
  }

}
