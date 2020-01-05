package com.romeikat.datamessie.dao.impl;

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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.dao.EntityWithIdDao;
import com.romeikat.datamessie.dao.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.dao.util.SessionManager;
import com.romeikat.datamessie.model.EntityWithId;

public abstract class AbstractEntityWithIdDao<E extends EntityWithId> extends AbstractEntityDao<E>
    implements EntityWithIdDao<E> {

  public AbstractEntityWithIdDao(final Class<? extends E> entityClass) {
    super(entityClass);
  }

  @Override
  public E getEntity(final long id) {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(getEntityClass());
    criteria.add(Restrictions.idEq(id));
    // Done
    @SuppressWarnings("unchecked")
    final E result = (E) criteria.uniqueResult();
    SessionManager.closeSession();
    return result;
  }

  @Override
  public List<E> getEntities(final Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(getEntityClass());
    criteria.add(Restrictions.in("id", ids));
    // Done
    @SuppressWarnings("unchecked")
    final List<E> result = criteria.list();
    SessionManager.closeSession();
    return result;
  }

  @Override
  public Map<Long, E> getIdsWithEntities() {
    final List<E> objects = getAllEntites();
    final Map<Long, E> result = Maps.uniqueIndex(objects, s -> s.getId());
    return result;
  }

  @Override
  public Map<Long, E> getIdsWithEntities(final Collection<Long> ids) {
    final List<E> objects = getEntities(ids);
    final Map<Long, E> result = Maps.uniqueIndex(objects, s -> s.getId());
    return result;
  }

  @Override
  public List<Long> getIds() {
    return getIds(null, null);
  }

  @Override
  public List<Long> getIds(final Long firstId, final Integer maxResults) {
    // Query
    final EntityWithIdQuery<E> query = new EntityWithIdQuery<>(getEntityClass());
    if (firstId != null) {
      query.addRestriction(Restrictions.ge("id", firstId));
    }
    query.setMaxResults(maxResults);
    query.addOrder(Order.asc("id"));
    // Done
    final List<Long> result = query.listIds(SessionManager.getSession());
    SessionManager.closeSession();
    return result;
  }

  @Override
  public List<Long> getIds(final Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    // Query
    final EntityWithIdQuery<E> query = new EntityWithIdQuery<>(getEntityClass());
    query.addRestriction(Restrictions.in("id", ids));
    final String defaultSortingProperty = defaultSortingProperty();
    if (defaultSortingProperty != null) {
      query.addOrder(Order.asc(defaultSortingProperty));
    }
    // Done
    final List<Long> result = query.listIds(SessionManager.getSession());
    SessionManager.closeSession();
    return result;
  }

  @Override
  public List<Long> getIds(final Collection<Long> ids, final Long firstId,
      final Integer maxResults) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    // Query
    final EntityWithIdQuery<E> query = new EntityWithIdQuery<>(getEntityClass());
    query.addRestriction(Restrictions.in("id", ids));
    if (firstId != null) {
      query.addRestriction(Restrictions.ge("id", firstId));
    }
    query.setMaxResults(maxResults);
    query.addOrder(Order.asc("id"));
    // Done
    final List<Long> result = query.listIds(SessionManager.getSession());
    SessionManager.closeSession();
    return result;
  }

  @Override
  public Long getMaxId() {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(getEntityClass());
    // Projection
    criteria.setProjection(Projections.max("id"));
    // Done
    final Long result = (Long) criteria.uniqueResult();
    SessionManager.closeSession();
    return result;
  }

  @Override
  public void insertOrUpdate(final E entity) {
    SessionManager.beginTransaction();
    final boolean exists = getEntity(entity.getId()) != null;

    // Insert
    if (!exists) {
      insert(entity);
    }
    // Update
    else {
      update(entity);
    }
    SessionManager.endTransaction();
  }

}
