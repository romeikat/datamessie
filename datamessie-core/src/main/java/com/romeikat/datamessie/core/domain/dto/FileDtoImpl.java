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


public class FileDtoImpl implements FileDto {

  private static final long serialVersionUID = 1L;

  private String filename;

  private String content;

  public FileDtoImpl(final String filename, final String content) {
    this.filename = filename;
    this.content = content;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  public void setFilename(final String filename) {
    this.filename = filename;
  }

  @Override
  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

}
