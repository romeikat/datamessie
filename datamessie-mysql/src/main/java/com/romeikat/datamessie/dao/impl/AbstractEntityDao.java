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
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.dao.EntityDao;
import com.romeikat.datamessie.dao.util.SessionManager;
import com.romeikat.datamessie.model.Entity;

public abstract class AbstractEntityDao<E extends Entity> implements EntityDao<E> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityDao.class);

  private final Class<? extends E> entityClass;

  public AbstractEntityDao(final Class<? extends E> entityClass) {
    this.entityClass = entityClass;
  }

  protected abstract String defaultSortingProperty();

  @Override
  public E create() {
    try {
      return entityClass.newInstance();
    } catch (final Exception e) {
      final String msg = String.format("Could not instantiate %s", entityClass.getName());
      LOG.error(msg, e);
      return null;
    }
  }

  /**
   * Returns the class of the entity.
   *
   * @return
   */
  protected Class<? extends E> getEntityClass() {
    return entityClass;
  }

  /**
   * Provides all entities.
   *
   * @return
   */
  @Override
  public List<E> getAllEntites() {
    return getEntites(null, null);
  }

  /**
   * Provides all entities.
   *
   * @param firstResult
   * @param maxResults
   * @return
   */
  @Override
  public List<E> getEntites(final Integer firstResult, final Integer maxResults) {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(entityClass);
    final String defaultSortingProperty = defaultSortingProperty();
    if (defaultSortingProperty != null) {
      criteria.addOrder(Order.asc(defaultSortingProperty));
    }
    if (firstResult != null) {
      criteria.setFirstResult(firstResult);
    }
    if (maxResults != null) {
      criteria.setMaxResults(maxResults);
    }
    // Done
    @SuppressWarnings("unchecked")
    final List<E> entities = criteria.list();
    SessionManager.closeSession();
    return entities;
  }

  /**
   * Provides all entities by a given property.
   *
   * @param property
   * @param value
   * @return
   */
  @Override
  public List<E> getEntitesByProperty(final String property, final Object value) {
    return getEntitesByProperty(property, value, null, null);
  }

  /**
   * Provides all entities by a given property.
   *
   * @param property
   * @param value
   * @param firstResult
   * @param maxResults
   * @return
   */
  @Override
  public List<E> getEntitesByProperty(final String property, final Object value,
      final Integer firstResult, final Integer maxResults) {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(entityClass);
    if (property != null && value != null) {
      criteria.add(Restrictions.eq(property, value));
    }
    if (firstResult != null) {
      criteria.setFirstResult(firstResult);
    }
    if (maxResults != null) {
      criteria.setMaxResults(maxResults);
    }
    final String defaultSortingProperty = defaultSortingProperty();
    if (defaultSortingProperty != null) {
      criteria.addOrder(Order.asc(defaultSortingProperty));
    }
    // Done
    @SuppressWarnings("unchecked")
    final List<E> entities = criteria.list();
    SessionManager.closeSession();
    return entities;
  }

  /**
   * Provides a unique entity by a given property value.
   *
   * @param property
   * @param value
   * @return
   */
  @Override
  public E getUniqueEntityByProperty(final String property, final Object value) {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(entityClass);
    if (property != null && value != null) {
      criteria.add(Restrictions.eq(property, value));
    }
    // Done
    @SuppressWarnings("unchecked")
    final E entity = (E) criteria.uniqueResult();
    SessionManager.closeSession();
    return entity;
  }

  /**
   * Counts all entities.
   *
   * @return
   */
  @Override
  public long countAll() {
    // Query
    final Criteria criteria = SessionManager.getSession().createCriteria(entityClass);
    // Projection
    criteria.setProjection(Projections.count("id"));
    // Done
    Long count = (Long) criteria.uniqueResult();
    SessionManager.closeSession();
    if (count == null) {
      count = 0l;
    }
    return count;
  }

  /**
   * Inserts a new entity. If the entity already exists in the database, a copy of it is saved with
   * a new id.
   *
   * @param entity
   */
  @Override
  public void insert(final E entity) {
    SessionManager.getSession().insert(entity);
    SessionManager.closeSession();
  }

  /**
   * Updates an existing entity. If the entity does not exist in the database, an exception is
   * thrown.
   *
   * @param entity
   */
  @Override
  public void update(final E entity) {
    SessionManager.getSession().update(entity);
    SessionManager.closeSession();
  }

  /**
   * Deletes an existing entity. If the entity does not exist in the database, an exception is
   * thrown.
   *
   * @param entity
   */
  @Override
  public void delete(final E entity) {
    SessionManager.getSession().delete(entity);
    SessionManager.closeSession();
  }

}
