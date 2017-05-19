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

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.SharedSessionContract;

import com.romeikat.datamessie.core.base.query.entity.execute.AbstractEntityQueryExecutor;

import jersey.repackaged.com.google.common.collect.Sets;

public class IdRestrictionsIntegrator {

  private final AbstractEntityQueryExecutor<?, ?> executor;
  private final IdRestrictionsDecider idRestrictionsDecider;
  private final Criteria criteria;

  public IdRestrictionsIntegrator(final AbstractEntityQueryExecutor<?, ?> executor,
      final IdRestrictionsDecider idRestrictionsDecider, final Criteria criteria) {
    this.executor = executor;
    this.idRestrictionsDecider = idRestrictionsDecider;
    this.criteria = criteria;
  }

  public void integrateIdRestrictionsIntoCriteria(final SharedSessionContract ssc) {
    if (idRestrictionsDecider.shouldPassThroughIdRestrictions()) {
      executor.applyIdRestriction(idRestrictionsDecider.getIdsFromIdRestrictions(), criteria);
    } else if (idRestrictionsDecider.shouldPreprocessIdRestrictions()) {
      final List<Long> idsOfRestrictionsOnly = executor.getIdsOfRestrictionsOnly(ssc);
      final Set<Long> overallIds = Sets.newHashSet(idsOfRestrictionsOnly);
      overallIds.retainAll(idRestrictionsDecider.getIdsFromIdRestrictions());
      executor.applyIdRestriction(overallIds, criteria);
    }
  }

}
