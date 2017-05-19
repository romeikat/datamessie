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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.romeikat.datamessie.core.domain.entity.EntityWithId;

@Entity
@Table(name = FooEntityWithId2.TABLE_NAME,
    uniqueConstraints = {@UniqueConstraint(name = "fooEntityWithId2_name", columnNames = {"name"})})
public class FooEntityWithId2 implements EntityWithId {

  public static final String TABLE_NAME = "fooEntityWithId2";

  private long id2;

  private String name;

  private boolean active;

  public FooEntityWithId2() {}

  public FooEntityWithId2(final long id2) {
    this.id2 = id2;
  }

  @Id
  public long getId2() {
    return id2;
  }

  public void setId2(final long id2) {
    this.id2 = id2;
  }

  @Override
  @Transient
  public long getId() {
    return getId2();
  }

  @Override
  @Transient
  public void setId(final long id2) {
    setId2(id2);
  }

  public String getName() {
    return name;
  }

  public FooEntityWithId2 setName(final String name) {
    this.name = name;
    return this;
  }

  public boolean getActive() {
    return active;
  }

  public FooEntityWithId2 setActive(final boolean active) {
    this.active = active;
    return this;
  }

}
