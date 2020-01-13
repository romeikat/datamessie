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

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.util.StringHashProvider;

@Entity
@Table(name = DeletingRule.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "deletingRule_id_version",
        columnNames = {"id", "version"}),
    indexes = @Index(name = "FK_deletingRule_source_id", columnList = "source_id"))
public class DeletingRule extends AbstractEntityWithGeneratedIdAndVersion
    implements StringHashProvider {

  public static final String TABLE_NAME = "deletingRule";

  private String regex;

  private LocalDate activeFrom;

  private LocalDate activeTo;

  private long sourceId;

  public DeletingRule() {}

  public DeletingRule(final long id, final long sourceId) {
    super(id);
    this.sourceId = sourceId;
  }

  @Override
  public String asStringHash() {
    return regex + "#" + activeFrom + "#" + activeTo + "#" + sourceId;
  }

  public String getRegex() {
    return regex;
  }

  public DeletingRule setRegex(final String regex) {
    this.regex = regex;
    return this;
  }

  public LocalDate getActiveFrom() {
    return activeFrom;
  }

  public DeletingRule setActiveFrom(final LocalDate activeFrom) {
    this.activeFrom = activeFrom;
    return this;
  }

  public LocalDate getActiveTo() {
    return activeTo;
  }

  public DeletingRule setActiveTo(final LocalDate activeTo) {
    this.activeTo = activeTo;
    return this;
  }

  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  public DeletingRule setSourceId(final long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Transient
  public boolean isActive(final LocalDate localDate) {
    if (localDate == null) {
      return false;
    }
    final boolean activeFromOk = activeFrom == null || activeFrom.compareTo(localDate) <= 0;
    final boolean activeToOk = activeTo == null || activeTo.compareTo(localDate) >= 0;
    return activeFromOk && activeToOk;
  }

}
