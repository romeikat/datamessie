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

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithoutIdAndVersion;
import com.romeikat.datamessie.core.domain.entity.Source2SourceType;

@Entity
@Table(name = Source2SourceTypeImpl.TABLE_NAME,
    indexes = {@Index(name = "FK_source_sourceType_source_id", columnList = "source_id"),
        @Index(name = "FK_source_sourceType_sourceType_id", columnList = "sourceType_id")})
public class Source2SourceTypeImpl extends AbstractEntityWithoutIdAndVersion
    implements Source2SourceType, Serializable {

  public static final String TABLE_NAME = "source_sourceType";

  private static final long serialVersionUID = 1L;

  private long sourceId;

  private long sourceTypeId;

  public Source2SourceTypeImpl() {}

  public Source2SourceTypeImpl(final long sourceId, final long sourceTypeId) {
    this.sourceId = sourceId;
    this.sourceTypeId = sourceTypeId;
  }

  @Override
  @Id
  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  @Override
  public Source2SourceType setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
  @Id
  @Column(name = "sourceType_id", nullable = false)
  public long getSourceTypeId() {
    return sourceTypeId;
  }

  @Override
  public Source2SourceType setSourceTypeId(final long sourceTypeId) {
    this.sourceTypeId = sourceTypeId;
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
    final Source2SourceTypeImpl otherSource2SourceType = (Source2SourceTypeImpl) other;
    final boolean equals = new EqualsBuilder().append(sourceId, otherSource2SourceType.sourceId)
        .append(sourceTypeId, otherSource2SourceType.sourceTypeId).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(sourceId).append(sourceTypeId).toHashCode();
    return hashCode;
  }

}
