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

import com.romeikat.datamessie.core.domain.enums.NamedEntityType;

public class NamedEntityDto implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String parentName;

  private String categories;

  private NamedEntityType type;

  private int quantity;

  private long document;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getParentName() {
    return parentName;
  }

  public void setParentName(final String parentName) {
    this.parentName = parentName;
  }

  public String getCategories() {
    return categories;
  }

  public void setCategories(final String categories) {
    this.categories = categories;
  }

  public NamedEntityType getType() {
    return type;
  }

  public void setType(final NamedEntityType type) {
    this.type = type;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(final int quantity) {
    this.quantity = quantity;
  }

  public long getDocument() {
    return document;
  }

  public void setDocument(final long document) {
    this.document = document;
  }

  public boolean hasDifferentParent() {
    return !name.equals(parentName);
  }

  @Override
  public NamedEntityDto clone() {
    final NamedEntityDto clone = new NamedEntityDto();
    clone.name = name;
    clone.parentName = parentName;
    clone.categories = categories;
    clone.type = type;
    clone.quantity = quantity;
    clone.document = document;
    return clone;
  }

}
