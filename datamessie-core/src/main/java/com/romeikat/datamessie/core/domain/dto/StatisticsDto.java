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

public class StatisticsDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long allDocuments;

  private Long downloadedDocuments;

  private Long preprocessedDocuments;

  private Double downloadSuccess;

  private Double preprocessingSuccess;

  private Long documentsToBePreprocessed;

  public static StatisticsDto emptyStatistics() {
    final StatisticsDto emptyHistory = new StatisticsDto();
    return emptyHistory;
  }

  public Long getAllDocuments() {
    return allDocuments;
  }

  public void setAllDocuments(final Long allDocuments) {
    this.allDocuments = allDocuments;
  }

  public Long getDownloadedDocuments() {
    return downloadedDocuments;
  }

  public void setDownloadedDocuments(final Long downloadedDocuments) {
    this.downloadedDocuments = downloadedDocuments;
  }

  public Long getPreprocessedDocuments() {
    return preprocessedDocuments;
  }

  public void setPreprocessedDocuments(final Long preprocessedDocuments) {
    this.preprocessedDocuments = preprocessedDocuments;
  }

  public Double getDownloadSuccess() {
    return downloadSuccess;
  }

  public void setDownloadSuccess(final Double downloadSuccess) {
    this.downloadSuccess = downloadSuccess;
  }

  public Double getPreprocessingSuccess() {
    return preprocessingSuccess;
  }

  public void setPreprocessingSuccess(final Double preprocessingSuccess) {
    this.preprocessingSuccess = preprocessingSuccess;
  }

  public Long getDocumentsToBePreprocessed() {
    return documentsToBePreprocessed;
  }

  public void setDocumentsToBePreprocessed(final Long documentsToBePreprocessed) {
    this.documentsToBePreprocessed = documentsToBePreprocessed;
  }

}
