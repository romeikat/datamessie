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
import java.util.Set;
import com.google.common.collect.Sets;

public class DeleteDecider {

  private final Set<Long> lhsIds;
  private final Set<Long> rhsIds;

  private final DeleteDecisionResults result;

  public DeleteDecider(final Collection<Long> lhsIds, final Collection<Long> rhsIds) {
    this.lhsIds = Sets.newHashSet(lhsIds);
    this.rhsIds = Sets.newHashSet(rhsIds);

    result = new DeleteDecisionResults();
  }

  public DeleteDecider makeDecisions() {
    rhsIds.removeAll(lhsIds);
    result.addToBeDeleted(rhsIds);

    return this;
  }

  public DeleteDecisionResults getDecisionResults() {
    return result;
  }

}
