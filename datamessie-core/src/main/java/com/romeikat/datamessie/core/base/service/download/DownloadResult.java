package com.romeikat.datamessie.core.base.service.download;

import java.nio.charset.Charset;

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

  private String originalUrl;

  private String url;

  private String content;

  private Charset charset;

  private LocalDateTime downloaded;

  private Integer statusCode;

  public DownloadResult(final String originalUrl, final String url, final String content,
      final Charset charset, final LocalDateTime downloaded, final Integer statusCode) {
    this.originalUrl = originalUrl;
    this.url = url;
    this.content = content;
    this.charset = charset;
    this.downloaded = downloaded;
    this.statusCode = statusCode;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public void setOriginalUrl(final String originalUrl) {
    this.originalUrl = originalUrl;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(final Charset charset) {
    this.charset = charset;
  }

  public LocalDateTime getDownloaded() {
    return downloaded;
  }

  public void setDownloaded(final LocalDateTime downloaded) {
    this.downloaded = downloaded;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(final Integer statusCode) {
    this.statusCode = statusCode;
  }

}
