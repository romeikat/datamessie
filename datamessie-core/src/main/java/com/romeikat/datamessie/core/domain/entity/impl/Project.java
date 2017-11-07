package com.romeikat.datamessie.core.domain.entity.impl;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;

@Entity
@Table(name = Project.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "project_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "project_name", columnNames = {"name"}),
        @UniqueConstraint(name = "project_id_version", columnNames = {"id", "version"})})
public class Project extends AbstractEntityWithGeneratedIdAndVersion {

  public static final String TABLE_NAME = "project";

  private String name;

  private boolean crawlingEnabled;

  private Integer crawlingInterval;

  private boolean preprocessingEnabled;

  public Project() {}

  public Project(final long id, final String name, final boolean crawlingEnabled,
      final boolean preprocessingEnabled) {
    super(id);
    this.name = name;
    this.crawlingEnabled = crawlingEnabled;
    this.preprocessingEnabled = preprocessingEnabled;
  }

  @Column(nullable = false)
  public String getName() {
    return name;
  }

  public Project setName(final String name) {
    this.name = name;
    return this;
  }

  @Column(nullable = false)
  public boolean getCrawlingEnabled() {
    return crawlingEnabled;
  }

  public Project setCrawlingEnabled(final boolean crawlingEnabled) {
    this.crawlingEnabled = crawlingEnabled;
    return this;
  }

  public Integer getCrawlingInterval() {
    return crawlingInterval;
  }

  public Project setCrawlingInterval(final Integer crawlingInterval) {
    this.crawlingInterval = crawlingInterval;
    return this;
  }

  @Column(nullable = false)
  public boolean getPreprocessingEnabled() {
    return preprocessingEnabled;
  }

  public Project setPreprocessingEnabled(final boolean preprocessingEnabled) {
    this.preprocessingEnabled = preprocessingEnabled;
    return this;
  }

}
