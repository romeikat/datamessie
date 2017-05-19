package com.romeikat.datamessie.core.processing.task.documentProcessing.stemming;

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

import java.util.List;

import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;

public class DocumentStemmingResult {

  private final String stemmedTitle;

  private final String stemmedDescription;

  private final String stemmedContent;

  private final List<NamedEntityDetectionDto> namedEntityDetections;

  public DocumentStemmingResult(final String stemmedTitle, final String stemmedDescription, final String stemmedContent,
      final List<NamedEntityDetectionDto> namedEntityDetections) {
    this.stemmedTitle = stemmedTitle;
    this.stemmedDescription = stemmedDescription;
    this.stemmedContent = stemmedContent;
    this.namedEntityDetections = namedEntityDetections;
  }

  public String getStemmedTitle() {
    return stemmedTitle;
  }

  public String getStemmedDescription() {
    return stemmedDescription;
  }

  public String getStemmedContent() {
    return stemmedContent;
  }

  public List<NamedEntityDetectionDto> getNamedEntityDetections() {
    return namedEntityDetections;
  }

}
