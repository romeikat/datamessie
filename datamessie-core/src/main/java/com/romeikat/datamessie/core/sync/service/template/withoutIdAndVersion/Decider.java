package com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion;

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

import com.romeikat.datamessie.core.domain.entity.Entity;

public class Decider<E extends Entity> {

  private final Collection<E> lhsEntities;
  private final Collection<E> rhsEntities;

  private final DecisionResults<E> result;

  public Decider(final Collection<E> lhsEntities, final Collection<E> rhsEntities) {
    this.lhsEntities = lhsEntities;
    this.rhsEntities = rhsEntities;

    result = new DecisionResults<E>();
  }

  public Decider<E> makeDecisions() {
    for (final E rhsEntity : rhsEntities) {
      makeDecisionDelete(rhsEntity);
    }

    for (final E lhsEntity : lhsEntities) {
      makeDecisionCreate(lhsEntity);
    }

    return this;
  }

  private void makeDecisionDelete(final E rhsEntity) {
    if (!lhsEntities.contains(rhsEntity)) {
      result.addToBeDeleted(rhsEntity);
    }
  }

  private void makeDecisionCreate(final E lhsEntity) {
    if (!rhsEntities.contains(lhsEntity)) {
      result.addToBeCreated(lhsEntity);
    }
  }

  public DecisionResults<E> getDecisionResults() {
    return result;
  }

}
