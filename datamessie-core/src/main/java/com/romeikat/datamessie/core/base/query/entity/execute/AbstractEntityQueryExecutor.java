package com.romeikat.datamessie.core.base.query.entity.execute;

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
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.domain.entity.Entity;
import jersey.repackaged.com.google.common.base.Objects;

public abstract class AbstractEntityQueryExecutor<E extends Entity, R> {

  private final SharedSessionContract ssc;
  private final EntityQuery<E> query;

  public AbstractEntityQueryExecutor(final SharedSessionContract ssc, final EntityQuery<E> query) {
    this.ssc = ssc;
    this.query = query;
  }

  public R execute() {
    if (getQuery().shouldReturnNull()) {
      return null;
    }

    final Criteria criteria = buildCriteria(ssc);
    final R result = executeCriteria(criteria);
    return result;
  }

  protected abstract Criteria buildCriteria(SharedSessionContract ssc);

  protected abstract R executeCriteria(Criteria criteria);

  public Criteria createCriteria(final SharedSessionContract ssc,
      final Class<? extends E> targetClass) {
    final Criteria criteria = ssc.createCriteria(targetClass);
    return criteria;
  }

  public void applyRestrictions(final Collection<Criterion> restrictions, final Criteria criteria) {
    for (final Criterion restriction : restrictions) {
      criteria.add(restriction);
    }
  }

  public void applyIdRestriction(final Collection<Long> ids, final Criteria criteria) {
    if (ids == null) {
      return;
    }

    if (ids.isEmpty()) {
      criteria.add(Restrictions.idEq(-1l));
    } else {
      criteria.add(Restrictions.in("id", ids));
    }
  }

  public void applyOrders(final Collection<Order> orders, final Criteria criteria) {
    for (final Order order : orders) {
      criteria.addOrder(order);
    }
  }

  public void applyOrderForProperty(final Collection<Order> orders, final String property,
      final Criteria criteria) {
    for (final Order order : orders) {
      // Ordering is only supported for the projected property
      if (Objects.equal(order.getPropertyName(), property)) {
        criteria.addOrder(order);
      }
    }
  }

  public void applyFirstResult(final Integer firstResult, final Criteria criteria) {
    if (firstResult != null) {
      criteria.setFirstResult(firstResult);
    }
  }

  public void applyMaxResults(final Integer maxResults, final Criteria criteria) {
    if (maxResults != null) {
      criteria.setMaxResults(maxResults);
    }
  }

  public void applyIdProjection(final Criteria criteria) {
    criteria.setProjection(Projections.id());
  }

  public void applyPropertyProjection(final Criteria criteria, final String propertyName) {
    criteria.setProjection(Projections.property(propertyName));
  }

  public void applyProjection(final Criteria criteria, final Projection projection) {
    criteria.setProjection(projection);
  }

  public void applyDistinctPropertyProjection(final Criteria criteria, final String propertyName) {
    criteria.setProjection(Projections.distinct(Projections.property(propertyName)));
  }

  public void applyCountProjection(final Criteria criteria, final String propertyName) {
    criteria.setProjection(Projections.count(propertyName));
  }

  public void applyResultTransformer(final ResultTransformer resultTransformer,
      final Criteria criteria) {
    if (resultTransformer != null) {
      criteria.setResultTransformer(resultTransformer);
    }
  }

  public List<Long> getIdsOfRestrictionsOnly(final SharedSessionContract ssc) {
    final Criteria criteria = createCriteria(ssc, query.getTargetClass());
    applyRestrictions(query.getRestrictions(), criteria);
    applyIdProjection(criteria);
    @SuppressWarnings("unchecked")
    final List<Long> ids = criteria.list();
    return ids;
  }

  public EntityQuery<E> getQuery() {
    return query;
  }

}
