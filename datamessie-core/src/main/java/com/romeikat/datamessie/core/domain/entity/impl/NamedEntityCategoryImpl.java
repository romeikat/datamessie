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
import com.romeikat.datamessie.core.domain.entity.NamedEntityCategory;

@Entity
@Table(name = NamedEntityCategoryImpl.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "namedEntityCategory_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "namedEntityCategory_namedEntity_id_categoryNamedEntity_id",
            columnNames = {"namedEntity_id", "categoryNamedEntity_id"})})
public class NamedEntityCategoryImpl extends AbstractEntityWithGeneratedIdAndVersion
    implements NamedEntityCategory {

  public static final String TABLE_NAME = "namedEntityCategory";

  private long namedEntityId;

  private long categoryNamedEntityId;

  public NamedEntityCategoryImpl() {}

  public NamedEntityCategoryImpl(final long id, final long namedEntityId,
      final long categoryNamedEntityId) {
    super(id);
    this.namedEntityId = namedEntityId;
    this.categoryNamedEntityId = categoryNamedEntityId;
  }

  @Override
  @Column(name = "namedEntity_id", nullable = false)
  public long getNamedEntityId() {
    return namedEntityId;
  }

  @Override
  public NamedEntityCategory setNamedEntityId(final long namedEntityId) {
    this.namedEntityId = namedEntityId;
    return this;
  }

  @Override
  @Column(name = "categoryNamedEntity_id", nullable = false)
  public long getCategoryNamedEntityId() {
    return categoryNamedEntityId;
  }

  @Override
  public NamedEntityCategory setCategoryNamedEntityId(final long categoryNamedEntityId) {
    this.categoryNamedEntityId = categoryNamedEntityId;
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Named entity " + namedEntityId);
    stringBuilder.append(" <= ");
    stringBuilder.append(" category named entity " + categoryNamedEntityId);
    return stringBuilder.toString();
  }

}
