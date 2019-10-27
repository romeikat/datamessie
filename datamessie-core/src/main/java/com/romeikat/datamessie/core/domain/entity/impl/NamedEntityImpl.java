package com.romeikat.datamessie.core.domain.entity.impl;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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
import com.romeikat.datamessie.core.domain.entity.NamedEntity;

@Entity
@Table(name = NamedEntityImpl.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "namedEntity_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "namedEntity_name", columnNames = {"name"})})
public class NamedEntityImpl extends AbstractEntityWithGeneratedIdAndVersion
    implements NamedEntity {

  public static final String TABLE_NAME = "namedEntity";

  private String name;

  public NamedEntityImpl() {}

  public NamedEntityImpl(final long id, final String name) {
    super(id);
    this.name = name;
  }

  @Column(nullable = false)
  public String getName() {
    return name;
  }

  public NamedEntity setName(final String name) {
    this.name = name;
    return this;
  }

}
