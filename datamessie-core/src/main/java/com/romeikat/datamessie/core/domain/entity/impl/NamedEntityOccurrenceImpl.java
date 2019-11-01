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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.model.core.NamedEntityOccurrence;
import com.romeikat.datamessie.model.enums.NamedEntityType;

@Entity
@Table(name = NamedEntityOccurrenceImpl.TABLE_NAME, uniqueConstraints = {
    @UniqueConstraint(name = "namedEntityOccurrence_id_version", columnNames = {"id", "version"}),
    @UniqueConstraint(name = "namedEntityOccurrence_namedEntity_id_type_document_id",
        columnNames = {"namedEntity_id", "type", "document_id"})},
    indexes = {
        @Index(name = "FK_namedEntityOccurrence_namedEntity_id", columnList = "namedEntity_id"),
        @Index(name = "FK_namedEntityOccurrence_document_id", columnList = "document_id")})
public class NamedEntityOccurrenceImpl extends AbstractEntityWithGeneratedIdAndVersion
    implements NamedEntityOccurrence {

  public static final String TABLE_NAME = "namedEntityOccurrence";

  private long namedEntityId;

  private long parentNamedEntityId;

  private NamedEntityType type;

  private int quantity;

  private long documentId;

  public NamedEntityOccurrenceImpl() {}

  public NamedEntityOccurrenceImpl(final long id, final long namedEntityId,
      final long parentNamedEntityId, final NamedEntityType type, final int quantity,
      final long documentId) {
    super(id);
    this.namedEntityId = namedEntityId;
    this.parentNamedEntityId = parentNamedEntityId;
    this.type = type;
    this.quantity = quantity;
    this.documentId = documentId;
  }

  @Override
  @Column(name = "namedEntity_id", nullable = false)
  public long getNamedEntityId() {
    return namedEntityId;
  }

  @Override
  public NamedEntityOccurrence setNamedEntityId(final long namedEntityId) {
    this.namedEntityId = namedEntityId;
    return this;
  }

  @Override
  @Column(name = "parentNamedEntity_id", nullable = false)
  public long getParentNamedEntityId() {
    return parentNamedEntityId;
  }

  @Override
  public NamedEntityOccurrence setParentNamedEntityId(final long parentNamedEntityId) {
    this.parentNamedEntityId = parentNamedEntityId;
    return this;
  }

  @Override
  @Column(nullable = false)
  @Enumerated(value = EnumType.ORDINAL)
  public NamedEntityType getType() {
    return type;
  }

  @Override
  public NamedEntityOccurrence setType(final NamedEntityType type) {
    this.type = type;
    return this;
  }

  @Override
  @Column(nullable = false)
  public int getQuantity() {
    return quantity;
  }

  @Override
  public NamedEntityOccurrence setQuantity(final int quantity) {
    this.quantity = quantity;
    return this;
  }

  @Override
  @Column(name = "document_id", nullable = false)
  public long getDocumentId() {
    return documentId;
  }

  @Override
  public NamedEntityOccurrence setDocumentId(final long documentId) {
    this.documentId = documentId;
    return this;
  }

  @Override
  @Transient
  public boolean hasDifferentParent() {
    return namedEntityId != parentNamedEntityId;
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(namedEntityId);
    if (hasDifferentParent()) {
      stringBuilder.append(" -> ");
      stringBuilder.append(parentNamedEntityId);
    }
    stringBuilder.append("/");
    stringBuilder.append(type);
    stringBuilder.append("/");
    stringBuilder.append(quantity);
    return stringBuilder.toString();
  }

}
