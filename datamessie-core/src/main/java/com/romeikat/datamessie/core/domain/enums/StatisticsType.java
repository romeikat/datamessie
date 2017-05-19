package com.romeikat.datamessie.core.domain.enums;

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

public enum StatisticsType {

  ALL_DOCUMENTS("All documents"),

  DOWNLOADED_DOCUMENTS("Downloaded documents"),

  DOWNLOAD_SUCCESS_RATE("Download success rate"),

  PREPROCESSED_DOCUMENTS("Preprocessed documents"),

  PREPROCESSING_SUCCESS_RATE("Preprocessing success rate"),

  TO_BE_PREPROCESSED("To be preprocessed"),

  DOWNLOAD_ERRORS("Download errors"),

  CLEANING_ERRORS("Cleaning errors");

  private String name;

  private StatisticsType(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
