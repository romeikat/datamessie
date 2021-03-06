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
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.entity.SourceRule;

@MappedSuperclass
public abstract class AbstractSourceRule extends AbstractEntityWithGeneratedIdAndVersion
    implements SourceRule {

  private LocalDate activeFrom;

  private LocalDate activeTo;

  private Integer position;

  private long sourceId;

  public AbstractSourceRule() {}

  public AbstractSourceRule(final long id, final long sourceId) {
    super(id);
    this.sourceId = sourceId;
  }

  @Override
  public String asStringHash() {
    return activeFrom + "#" + activeTo + "#" + position + "#" + sourceId;
  }

  @Override
  @Transient
  public String getStringHashForDateRange() {
    return activeFrom + "#" + activeTo;
  }

  @Override
  public LocalDate getActiveFrom() {
    return activeFrom;
  }

  public AbstractSourceRule setActiveFrom(final LocalDate activeFrom) {
    this.activeFrom = activeFrom;
    return this;
  }

  @Override
  public LocalDate getActiveTo() {
    return activeTo;
  }

  public AbstractSourceRule setActiveTo(final LocalDate activeTo) {
    this.activeTo = activeTo;
    return this;
  }

  @Override
  public Integer getPosition() {
    return position;
  }

  public void setPosition(final Integer position) {
    this.position = position;
  }

  @Override
  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  public AbstractSourceRule setSourceId(final Long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
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
