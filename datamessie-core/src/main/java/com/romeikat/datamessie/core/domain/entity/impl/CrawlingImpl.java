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

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.model.core.Crawling;

@Entity
@Table(name = CrawlingImpl.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "crawling_id_version",
        columnNames = {"id", "version"}),
    indexes = @Index(name = "FK_crawling_project_id", columnList = "project_id"))
public class CrawlingImpl extends AbstractEntityWithGeneratedIdAndVersion implements Crawling {

  public static final String TABLE_NAME = "crawling";

  private LocalDateTime started;

  private LocalDateTime completed;

  private long projectId;

  public CrawlingImpl() {}

  public CrawlingImpl(final long id, final long projectId) {
    super(id);
    this.projectId = projectId;
  }

  public LocalDateTime getStarted() {
    return started;
  }

  public Crawling setStarted(final LocalDateTime started) {
    this.started = started;
    return this;
  }

  public LocalDateTime getCompleted() {
    return completed;
  }

  public Crawling setCompleted(final LocalDateTime completed) {
    this.completed = completed;
    return this;
  }

  @Column(name = "project_id", nullable = false)
  public long getProjectId() {
    return projectId;
  }

  public Crawling setProjectId(final long projectId) {
    this.projectId = projectId;
    return this;
  }

}
