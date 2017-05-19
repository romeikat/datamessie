package com.romeikat.datamessie.core.base.service.download;

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


public class DownloadResult {

  private final String originalUrl;

  private final String url;

  private final String content;

  private final LocalDateTime downloaded;

  private final Integer statusCode;

  public DownloadResult(final String originalUrl, final String url, final String content,
      final LocalDateTime downloaded, final Integer statusCode) {
    this.originalUrl = originalUrl;
    this.url = url;
    this.content = content;
    this.downloaded = downloaded;
    this.statusCode = statusCode;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public String getUrl() {
    return url;
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

}
