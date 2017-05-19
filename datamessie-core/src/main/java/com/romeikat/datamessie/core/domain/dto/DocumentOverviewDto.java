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

public class DocumentOverviewDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String title;

  private String url;

  private LocalDateTime published;

  private LocalDateTime downloaded;

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

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public LocalDateTime getPublished() {
    return published;
  }

  public void setPublished(final LocalDateTime published) {
    this.published = published;
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public void setDownloaded(final LocalDateTime downloaded) {
    this.downloaded = downloaded;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public void setSourceId(final Long sourceId) {
    this.sourceId = sourceId;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(final String sourceName) {
    this.sourceName = sourceName;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(final String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

}
