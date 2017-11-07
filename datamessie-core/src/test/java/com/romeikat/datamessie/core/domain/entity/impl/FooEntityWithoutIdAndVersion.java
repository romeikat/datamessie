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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithoutIdAndVersion;

@Entity
@Table(name = FooEntityWithoutIdAndVersion.TABLE_NAME, uniqueConstraints = {
    @UniqueConstraint(name = "fooEntityWithoutIdAndVersion_name", columnNames = {"name"})})
public class FooEntityWithoutIdAndVersion extends AbstractEntityWithoutIdAndVersion
    implements Serializable {

  public static final String TABLE_NAME = "fooEntityWithoutIdAndVersion";

  private static final long serialVersionUID = 1L;

  private String name;

  public FooEntityWithoutIdAndVersion() {}

  public FooEntityWithoutIdAndVersion(final String name) {
    this.name = name;
  }

  @Id
  @Column(nullable = false)
  public String getName() {
    return name;
  }

  public FooEntityWithoutIdAndVersion setName(final String name) {
    this.name = name;
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
    final FooEntityWithoutIdAndVersion otherBar = (FooEntityWithoutIdAndVersion) other;
    final boolean equals = new EqualsBuilder().append(name, otherBar.name).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(name).toHashCode();
    return hashCode;
  }

}
