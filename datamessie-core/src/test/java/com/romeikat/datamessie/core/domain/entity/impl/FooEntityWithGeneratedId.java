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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.GenericGenerator;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;

@Entity
@Table(name = FooEntityWithGeneratedId.TABLE_NAME, uniqueConstraints = {
    @UniqueConstraint(name = "fooEntityWithGeneratedId_name", columnNames = {"name"})})
public class FooEntityWithGeneratedId implements EntityWithId {

  public static final String TABLE_NAME = "fooEntityWithGeneratedId";

  private long id;

  private String name;

  private boolean active;

  public FooEntityWithGeneratedId() {}

  public FooEntityWithGeneratedId(final long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public FooEntityWithGeneratedId setName(final String name) {
    this.name = name;
    return this;
  }

  public boolean getActive() {
    return active;
  }

  public FooEntityWithGeneratedId setActive(final boolean active) {
    this.active = active;
    return this;
  }

  @Override
  @Id
  @GenericGenerator(name = "assigned-identity",
      strategy = "com.romeikat.datamessie.core.domain.util.AssignedIdentityGenerator")
  @GeneratedValue(generator = "assigned-identity", strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "bigserial")
  public long getId() {
    return id;
  }

  @Override
  public void setId(final long id) {
    this.id = id;
  }

}
