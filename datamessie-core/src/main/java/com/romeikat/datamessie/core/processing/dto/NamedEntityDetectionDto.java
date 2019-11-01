package com.romeikat.datamessie.core.processing.dto;

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
import com.romeikat.datamessie.model.enums.NamedEntityType;

public class NamedEntityDetectionDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private String name;

  private String parentName;

  private NamedEntityType type;

  private Integer quantity;

  public String getName() {
    return name;
  }

  public NamedEntityDetectionDto setName(final String name) {
    this.name = name;
    return this;
  }

  public String getParentName() {
    return parentName;
  }

  public NamedEntityDetectionDto setParentName(final String parentName) {
    this.parentName = parentName;
    return this;
  }

  public NamedEntityType getType() {
    return type;
  }

  public NamedEntityDetectionDto setType(final NamedEntityType type) {
    this.type = type;
    return this;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public NamedEntityDetectionDto setQuantity(final Integer quantity) {
    this.quantity = quantity;
    return this;
  }

  public boolean hasDifferentParent() {
    return !name.equals(parentName);
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(name);
    if (hasDifferentParent()) {
      stringBuilder.append(" -> ");
      stringBuilder.append(parentName);
    }
    stringBuilder.append("/");
    stringBuilder.append(type);
    stringBuilder.append("/");
    stringBuilder.append(quantity);
    return stringBuilder.toString();
  }

}
