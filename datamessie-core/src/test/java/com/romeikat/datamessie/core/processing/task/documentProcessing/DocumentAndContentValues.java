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

import java.time.LocalDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentAndContentValues {

  private final String title;

  private final String stemmedTitle;

  private final String url;

  private final String description;

  private final String stemmedDescription;

  private final LocalDateTime published;

  private final LocalDateTime downloaded;

  private final DocumentProcessingState state;

  private final Integer statusCode;

  private final String rawContent;

  private final String cleanedContent;

  private final String stemmedContent;

  public DocumentAndContentValues(final String title, final String stemmedTitle, final String url,
      final String description, final String stemmedDescription, final LocalDateTime published,
      final LocalDateTime downloaded, final DocumentProcessingState state, final Integer statusCode,
      final String rawContent, final String cleanedContent, final String stemmedContent) {
    this.title = title;
    this.stemmedTitle = stemmedTitle;
    this.url = url;
    this.description = description;
    this.stemmedDescription = stemmedDescription;
    this.published = published;
    this.downloaded = downloaded;
    this.state = state;
    this.statusCode = statusCode;
    this.rawContent = rawContent;
    this.cleanedContent = cleanedContent;
    this.stemmedContent = stemmedContent;
  }

  public DocumentAndContentValues(final Document document, final RawContent rawContent,
      final CleanedContent cleanedContent, final StemmedContent stemmedContent) {
    this(document.getTitle(), document.getStemmedTitle(), document.getUrl(),
        document.getDescription(), document.getStemmedDescription(), document.getPublished(),
        document.getDownloaded(), document.getState(), document.getStatusCode(),
        rawContent == null ? null : rawContent.getContent(),
        cleanedContent == null ? null : cleanedContent.getContent(),
        stemmedContent == null ? null : stemmedContent.getContent());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("title", title).append("stemmedTitle", stemmedTitle)
        .append("url", url).append("description", description)
        .append("stemmedDescription", stemmedDescription).append("published", published)
        .append("downloaded", downloaded).append("state", state).append("statusCode", statusCode)
        .append("rawContent", rawContent).append("cleanedContent", cleanedContent)
        .append("stemmedContent", stemmedContent).toString();

  }

  public String getTitle() {
    return title;
  }

  public String getStemmedTitle() {
    return stemmedTitle;
  }

  public String getUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  public String getStemmedDescription() {
    return stemmedDescription;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public DocumentProcessingState getState() {
    return state;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public String getRawContent() {
    return rawContent;
  }

  public String getCleanedContent() {
    return cleanedContent;
  }

  public String getStemmedContent() {
    return stemmedContent;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final DocumentAndContentValues otherDocumentAndContentValues = (DocumentAndContentValues) other;
    final boolean equals = new EqualsBuilder().append(title, otherDocumentAndContentValues.title)
        .append(stemmedTitle, otherDocumentAndContentValues.stemmedTitle)
        .append(url, otherDocumentAndContentValues.url)
        .append(description, otherDocumentAndContentValues.description)
        .append(stemmedDescription, otherDocumentAndContentValues.stemmedDescription)
        .append(published, otherDocumentAndContentValues.published)
        .append(downloaded, otherDocumentAndContentValues.downloaded)
        .append(state, otherDocumentAndContentValues.state)
        .append(statusCode, otherDocumentAndContentValues.statusCode)
        .append(rawContent, otherDocumentAndContentValues.rawContent)
        .append(cleanedContent, otherDocumentAndContentValues.cleanedContent)
        .append(stemmedContent, otherDocumentAndContentValues.stemmedContent).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(title).append(stemmedTitle).append(url)
        .append(description).append(stemmedDescription).append(published).append(downloaded)
        .append(state).append(statusCode).append(rawContent).append(cleanedContent)
        .append(stemmedContent).toHashCode();
    return hashCode;
  }

}
