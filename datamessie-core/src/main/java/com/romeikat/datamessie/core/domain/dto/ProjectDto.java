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

public class ProjectDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private long id;

  private String name;

  private boolean crawlingEnabled;

  private Integer crawlingInterval;

  private boolean preprocessingEnabled;

  public long getId() {
    return id;
  }

  public void setId(final long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public boolean getCrawlingEnabled() {
    return crawlingEnabled;
  }

  public void setCrawlingEnabled(final boolean crawlingEnabled) {
    this.crawlingEnabled = crawlingEnabled;
  }

  public Integer getCrawlingInterval() {
    return crawlingInterval;
  }

  public void setCrawlingInterval(final Integer crawlingInterval) {
    this.crawlingInterval = crawlingInterval;
  }

  public boolean getPreprocessingEnabled() {
    return preprocessingEnabled;
  }

  public void setPreprocessingEnabled(final boolean preprocessingEnabled) {
    this.preprocessingEnabled = preprocessingEnabled;
  }

}
