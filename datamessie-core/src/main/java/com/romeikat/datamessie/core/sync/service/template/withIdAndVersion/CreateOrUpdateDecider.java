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

  private final boolean shouldCreateData;
  private final boolean shouldUpdateData;

  private final CreateOrUpdateDecisionResults result;

  public CreateOrUpdateDecider(final Map<Long, Long> lhsIdsWithVersion,
      final Map<Long, Long> rhsIdsWithVersion, final boolean shouldCreateData,
      final boolean shouldUpdateData) {
    this.lhsIdsWithVersion = lhsIdsWithVersion;
    this.rhsIdsWithVersion = rhsIdsWithVersion;

    this.shouldCreateData = shouldCreateData;
    this.shouldUpdateData = shouldUpdateData;

    result = new CreateOrUpdateDecisionResults();
  }

  public CreateOrUpdateDecider makeDecisions() {
    final Collection<Long> lhsIds = lhsIdsWithVersion.keySet();
    for (final long lhsId : lhsIds) {
      final Long lhsVersion = lhsIdsWithVersion.get(lhsId);
      makeDecision(lhsId, lhsVersion);
    }

    return this;
  }

  private void makeDecision(final long lhsId, final Long lhsVersion) {
    // LHS id missing on RHS => Create on RHS
    if (!rhsIdsWithVersion.containsKey(lhsId)) {
      if (shouldCreateData) {
        result.addToBeCreated(lhsId, lhsVersion);
      }
      return;
    }

    // RHS (id,version) = LHS (id,version) => nothing to do
    final Long rhsVersion = rhsIdsWithVersion.get(lhsId);
    final boolean versionEquals = Objects.equals(lhsVersion, rhsVersion);
    if (versionEquals) {
      return;
    }

    // LHS id existing on RHS, but with different version => Update on RHS
    if (shouldUpdateData) {
      result.addToBeUpdated(lhsId, lhsVersion);
    }
  }

  public CreateOrUpdateDecisionResults getDecisionResults() {
    return result;
  }

}
