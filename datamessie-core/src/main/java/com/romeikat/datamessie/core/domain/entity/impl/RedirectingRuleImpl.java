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
import com.romeikat.datamessie.core.domain.entity.RedirectingRule;

@Entity
@Table(name = RedirectingRuleImpl.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "redirectomgRule_id_version",
        columnNames = {"id", "version"}),
    indexes = @Index(name = "FK_redirectingRule_source_id", columnList = "source_id"))
public class RedirectingRuleImpl extends AbstractEntityWithGeneratedIdAndVersion
    implements RedirectingRule {

  public static final String TABLE_NAME = "redirectingRule";

  private String regex;

  private Integer regexGroup;

  private LocalDate activeFrom;

  private LocalDate activeTo;

  private long sourceId;

  public RedirectingRuleImpl() {}

  public RedirectingRuleImpl(final long id, final long sourceId) {
    super(id);
    this.sourceId = sourceId;
  }

  @Override
  public String asStringHash() {
    return regex + "#" + regexGroup + "#" + activeFrom + "#" + activeTo + "#" + sourceId;
  }

  @Override
  public String getRegex() {
    return regex;
  }

  @Override
  public RedirectingRule setRegex(final String regex) {
    this.regex = regex;
    return this;
  }

  @Override
  public Integer getRegexGroup() {
    return regexGroup;
  }

  @Override
  public RedirectingRule setRegexGroup(final Integer regexGroup) {
    this.regexGroup = regexGroup;
    return this;
  }

  @Override
  public LocalDate getActiveFrom() {
    return activeFrom;
  }

  @Override
  public RedirectingRule setActiveFrom(final LocalDate activeFrom) {
    this.activeFrom = activeFrom;
    return this;
  }

  @Override
  public LocalDate getActiveTo() {
    return activeTo;
  }

  @Override
  public RedirectingRule setActiveTo(final LocalDate activeTo) {
    this.activeTo = activeTo;
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

  @Override
  @Column(name = "source_id", nullable = false)
  public long getSourceId() {
    return sourceId;
  }

  @Override
  public RedirectingRule setSourceId(final Long sourceId) {
    this.sourceId = sourceId;
    return this;
  }

}
