package com.romeikat.datamessie.core.sync.service.template.withIdAndVersion;

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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class CreateOrUpdateDecider {

  private final Map<Long, Long> lhsIdsWithVersion;
  private final Map<Long, Long> rhsIdsWithVersion;

  private final CreateOrUpdateDecisionResults result;

  public CreateOrUpdateDecider(final Map<Long, Long> lhsIdsWithVersion,
      final Map<Long, Long> rhsIdsWithVersion) {
    this.lhsIdsWithVersion = lhsIdsWithVersion;
    this.rhsIdsWithVersion = rhsIdsWithVersion;

    result = new CreateOrUpdateDecisionResults();
  }

  public CreateOrUpdateDecider makeDecisions() {
    final Collection<Long> lhsIds = lhsIdsWithVersion.keySet();
    for (final long lhsId : lhsIds) {
      makeDecision(lhsId);
    }

    return this;
  }

  private void makeDecision(final long lhsId) {
    // Create on RHS
    if (!rhsIdsWithVersion.containsKey(lhsId)) {
      result.addToBeCreated(lhsId);
      return;
    }

    // RHS = LHS
    final Long lhsVersion = lhsIdsWithVersion.get(lhsId);
    final Long rhsVersion = rhsIdsWithVersion.get(lhsId);
    final boolean versionEquals = Objects.equals(lhsVersion, rhsVersion);
    if (versionEquals) {
      return;
    }

    // Update on RHS
    result.addToBeUpdated(lhsId);
  }

  public CreateOrUpdateDecisionResults getDecisionResults() {
    return result;
  }

}
