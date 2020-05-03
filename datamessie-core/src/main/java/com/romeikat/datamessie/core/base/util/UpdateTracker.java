package com.romeikat.datamessie.core.base.util;

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

import com.romeikat.datamessie.core.domain.entity.SourceRule;
import jersey.repackaged.com.google.common.base.Objects;

public class UpdateTracker {

  private final SourceRule sourceRule;

  private String stringHashBeforeUpdate;
  private String stringHashAfterUpdate;

  private String stringHashForDateRangeBeforeUpdate;
  private String stringHashForDateRangeAfterUpdate;

  private String stringHashForLogicBeforeUpdate;
  private String stringHashForLogicAfterUpdate;

  public UpdateTracker(final SourceRule sourceRule) {
    this.sourceRule = sourceRule;
  }

  public UpdateTracker beginUpdate() {
    stringHashBeforeUpdate = sourceRule.asStringHash();
    stringHashForDateRangeBeforeUpdate = sourceRule.getStringHashForDateRange();
    stringHashForLogicBeforeUpdate = sourceRule.getStringHashForLogic();

    return this;
  }

  public UpdateTracker endUpdate() {
    stringHashAfterUpdate = sourceRule.asStringHash();
    stringHashForDateRangeAfterUpdate = sourceRule.getStringHashForDateRange();
    stringHashForLogicAfterUpdate = sourceRule.getStringHashForLogic();

    return this;
  }

  public boolean wasSourceRuleUpdated() {
    return !Objects.equal(stringHashBeforeUpdate, stringHashAfterUpdate);
  }

  public boolean wasSourceRuleDateRangeUpdated() {
    return !Objects.equal(stringHashForDateRangeBeforeUpdate, stringHashForDateRangeAfterUpdate);
  }

  public boolean wasSourceRuleLogicUpdated() {
    return !Objects.equal(stringHashForLogicBeforeUpdate, stringHashForLogicAfterUpdate);
  }

}
