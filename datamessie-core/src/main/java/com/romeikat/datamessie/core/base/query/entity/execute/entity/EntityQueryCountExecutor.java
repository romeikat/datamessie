package com.romeikat.datamessie.core.base.query.entity.execute.entity;

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

import org.hibernate.Criteria;
import org.hibernate.SharedSessionContract;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.execute.AbstractEntityQueryExecutor;
import com.romeikat.datamessie.core.domain.entity.Entity;

public class EntityQueryCountExecutor<E extends Entity>
    extends AbstractEntityQueryExecutor<E, Long> {

  private final String propertyName;

  public EntityQueryCountExecutor(final SharedSessionContract ssc,
      final EntityQuery<E> query, final String propertyName) {
    super(ssc, query);

    this.propertyName = propertyName;
  }

  @Override
  protected Criteria buildCriteria(final SharedSessionContract ssc) {
    final EntityQuery<E> query = getQuery();

    final Criteria criteria = createCriteria(ssc, query.getTargetClass());

    applyRestrictions(query.getRestrictions(), criteria);
    applyDistinctPropertyProjection(criteria, propertyName);
    applyCountProjection(criteria, propertyName);

    return criteria;
  }

  @Override
  protected Long executeCriteria(final Criteria criteria) {
    final Long count = (Long) criteria.uniqueResult();
    return count;
  }

}
