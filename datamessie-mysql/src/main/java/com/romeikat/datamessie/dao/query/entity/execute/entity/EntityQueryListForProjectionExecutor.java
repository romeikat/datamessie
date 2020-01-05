package com.romeikat.datamessie.dao.query.entity.execute.entity;

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
import org.hibernate.Criteria;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Projection;
import com.romeikat.datamessie.dao.query.entity.EntityQuery;
import com.romeikat.datamessie.dao.query.entity.execute.AbstractEntityQueryExecutor;
import com.romeikat.datamessie.model.Entity;

public class EntityQueryListForProjectionExecutor<E extends Entity>
    extends AbstractEntityQueryExecutor<E, List<?>> {

  private final Projection projection;

  public EntityQueryListForProjectionExecutor(final SharedSessionContract ssc,
      final EntityQuery<E> query, final Projection projection) {
    super(ssc, query);

    this.projection = projection;
  }

  @Override
  protected Criteria buildCriteria(final SharedSessionContract ssc) {
    final EntityQuery<E> query = getQuery();

    final Criteria criteria = createCriteria(ssc, query.getTargetClass());

    applyRestrictions(query.getRestrictions(), criteria);
    applyOrders(query.getOrders(), criteria);
    applyProjection(criteria, projection);
    // ResultTransformer must be applied after Projection
    applyResultTransformer(query.getResultTransformer(), criteria);
    applyFirstResult(query.getFirstResult(), criteria);
    applyMaxResults(query.getMaxResults(), criteria);

    return criteria;
  }

  @Override
  protected List<?> executeCriteria(final Criteria criteria) {
    final List<?> objects = criteria.list();
    return objects;
  }

}
