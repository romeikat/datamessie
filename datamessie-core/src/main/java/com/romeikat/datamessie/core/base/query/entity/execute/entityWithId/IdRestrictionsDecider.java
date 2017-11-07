package com.romeikat.datamessie.core.base.query.entity.execute.entityWithId;

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
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;

public class IdRestrictionsDecider {

  private static final int BREAK_EVEN = 100;

  private final EntityWithIdQuery<?> query;
  private final Set<Long> idsFromIdRestrictions;

  public IdRestrictionsDecider(final EntityWithIdQuery<?> query) {
    this.query = query;
    idsFromIdRestrictions = calculateIdsFromIdRestrictions();
  }

  public boolean shouldPassThroughIdRestrictions() {
    return idsFromIdRestrictions != null && idsFromIdRestrictions.size() <= BREAK_EVEN;
  }

  public boolean shouldPreprocessIdRestrictions() {
    return idsFromIdRestrictions != null && idsFromIdRestrictions.size() > BREAK_EVEN;
  }

  public Set<Long> getIdsFromIdRestrictions() {
    return idsFromIdRestrictions;
  }

  private Set<Long> calculateIdsFromIdRestrictions() {
    final Collection<Set<Long>> idRestrictions = query.getIdRestrictions();
    if (idRestrictions.isEmpty()) {
      return null;
    }

    Set<Long> result = null;

    for (final Collection<Long> idRestriction : idRestrictions) {
      if (idRestriction.isEmpty()) {
        idRestriction.add(-1l);
      }

      // First restriction
      if (result == null) {
        result = Sets.newHashSet(idRestriction);
      }
      // Further restrictions
      else {
        result.retainAll(idRestriction);
      }
    }

    return result;
  }

}
