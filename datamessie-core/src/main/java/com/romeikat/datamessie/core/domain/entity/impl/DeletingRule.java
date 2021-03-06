package com.romeikat.datamessie.core.domain.entity.impl;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.enums.DeletingRuleMode;

@Entity
@Table(name = DeletingRule.TABLE_NAME,
    uniqueConstraints = @UniqueConstraint(name = "deletingRule_id_version",
        columnNames = {"id", "version"}),
    indexes = @Index(name = "FK_deletingRule_source_id", columnList = "source_id"))
public class DeletingRule extends AbstractSourceRule {

  public static final String TABLE_NAME = "deletingRule";

  private String selector;

  private DeletingRuleMode mode = DeletingRuleMode.REGEX;

  public DeletingRule() {}

  public DeletingRule(final long id, final long sourceId) {
    super(id, sourceId);
  }

  @Override
  public String asStringHash() {
    return super.asStringHash() + "#" + selector + "#" + mode;
  }

  @Override
  @Transient
  public String getStringHashForLogic() {
    return selector + "#" + mode;
  }

  public String getSelector() {
    return selector;
  }

  public DeletingRule setSelector(final String selector) {
    this.selector = selector;
    return this;
  }

  public DeletingRuleMode getMode() {
    return mode;
  }

  public DeletingRule setMode(final DeletingRuleMode mode) {
    this.mode = mode;
    return this;
  }

}
