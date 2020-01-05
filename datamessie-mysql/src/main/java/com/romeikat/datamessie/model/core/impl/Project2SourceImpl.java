package com.romeikat.datamessie.model.core.impl;

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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.romeikat.datamessie.model.core.Project2Source;
import com.romeikat.datamessie.model.impl.AbstractEntityWithoutIdAndVersion;

@Entity
@Table(name = Project2SourceImpl.TABLE_NAME,
    indexes = {@Index(name = "FK_project_source_project_id", columnList = "project_id"),
        @Index(name = "FK_project_source_source_id", columnList = "source_id")})
public class Project2SourceImpl extends AbstractEntityWithoutIdAndVersion
    implements Project2Source, Serializable {

  public static final String TABLE_NAME = "project_source";

  private static final long serialVersionUID = 1L;

  private long projectId;

  private long sourceId;

  public Project2SourceImpl() {}

  public Project2SourceImpl(final long projectId, final long sourceId) {
    this.projectId = projectId;
    this.sourceId = sourceId;
  }

  @Override
  @Id
  @Column(name = "project_id", nullable = false)
  public long getProjectId() {
    return projectId;
  }

  @Override
  public Project2Source setProjectId(final long projectId) {
    this.projectId = projectId;
    return this;
  }

  @Override
  @Id
  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  @Override
  public Project2Source setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final Project2SourceImpl otherProject2Source = (Project2SourceImpl) other;
    final boolean equals = new EqualsBuilder().append(projectId, otherProject2Source.projectId)
        .append(sourceId, otherProject2Source.sourceId).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(projectId).append(sourceId).toHashCode();
    return hashCode;
  }

}
