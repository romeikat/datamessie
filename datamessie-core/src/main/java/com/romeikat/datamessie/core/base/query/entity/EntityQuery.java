package com.romeikat.datamessie.core.base.query.entity;

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
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryCountExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryListForProjectionExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryListIdsOfPropertyExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryListObjectsExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryUniqueForProjectionExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryUniqueIdOfPropertyExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entity.EntityQueryUniqueObjectExecutor;
import com.romeikat.datamessie.core.domain.entity.Entity;
import jersey.repackaged.com.google.common.collect.Lists;


public class EntityQuery<E extends Entity> {

  public static enum ReturnMode {
    RETURN_NULL, RETURN_ALL;
  }

  private final Class<? extends E> targetClass;
  private final Set<Criterion> restrictions;
  private Integer firstResult;
  private Integer maxResults;
  private final List<Order> orders;
  private ResultTransformer resultTransformer;
  private ReturnMode returnModeForEmptyRestrictions;

  public EntityQuery(final Class<? extends E> targetClass) {
    this.targetClass = targetClass;
    restrictions = Sets.newHashSet();
    firstResult = null;
    maxResults = null;
    orders = Lists.newLinkedList();
    resultTransformer = null;
    returnModeForEmptyRestrictions = ReturnMode.RETURN_ALL;
  }

  public EntityQuery(final EntityQuery<E> other) {
    this.targetClass = other.targetClass;
    restrictions = Sets.newHashSet(other.restrictions);
    orders = Lists.newLinkedList(other.orders);
    returnModeForEmptyRestrictions = other.returnModeForEmptyRestrictions;
  }

  public EntityQuery<E> addRestriction(final Criterion restriction) {
    restrictions.add(restriction);

    return this;
  }

  public Integer getFirstResult() {
    return firstResult;
  }

  public EntityQuery<E> setFirstResult(final Integer firstResult) {
    this.firstResult = firstResult;

    return this;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public EntityQuery<E> setMaxResults(final Integer maxResults) {
    this.maxResults = maxResults;

    return this;
  }

  public EntityQuery<E> addOrder(final Order order) {
    orders.add(order);

    return this;
  }

  public ResultTransformer getResultTransformer() {
    return resultTransformer;
  }

  public EntityQuery<E> setResultTransformer(final ResultTransformer resultTransformer) {
    this.resultTransformer = resultTransformer;

    return this;
  }

  public void setReturnModeForEmptyRestrictions(final ReturnMode returnModeForEmptyRestrictions) {
    this.returnModeForEmptyRestrictions = returnModeForEmptyRestrictions;
  }

  public List<E> listObjects(final SharedSessionContract ssc) {
    return new EntityQueryListObjectsExecutor<>(ssc, this).execute();
  }

  /**
   * Loads the IDs contained in a property of the respective objects.
   *
   * @param ssc
   * @param propertyName
   * @return
   */
  public List<Long> listIdsForProperty(final SharedSessionContract ssc, final String propertyName) {
    return new EntityQueryListIdsOfPropertyExecutor<>(ssc, this, propertyName).execute();
  }

  /**
   * Projects the respective objects.
   *
   * @param ssc
   * @param projection
   * @return
   */
  public List<?> listForProjection(final SharedSessionContract ssc, final Projection projection) {
    return new EntityQueryListForProjectionExecutor<>(ssc, this, projection).execute();
  }

  /**
   * Loads the respective object.
   *
   * @param ssc
   * @return
   */
  public E uniqueObject(final SharedSessionContract ssc) {
    return new EntityQueryUniqueObjectExecutor<>(ssc, this).execute();
  }

  /**
   * Loads the ID contained in a property of the respective object.
   *
   * @param ssc
   * @param propertyName
   * @return
   */
  public Long uniqueIdForProperty(final SharedSessionContract ssc, final String propertyName) {
    return new EntityQueryUniqueIdOfPropertyExecutor<>(ssc, this, propertyName).execute();
  }

  /**
   * Loads the respective object.
   *
   * @param ssc
   * @param projection
   * @return
   */
  public Object uniqueForProjection(final SharedSessionContract ssc, final Projection projection) {
    return new EntityQueryUniqueForProjectionExecutor<>(ssc, this, projection).execute();
  }

  /**
   * Loads the number of respective objects.
   *
   * @param ssc
   * @param propertyName
   * @return
   */
  public Long count(final SharedSessionContract ssc, final String propertyName) {
    return new EntityQueryCountExecutor<>(ssc, this, propertyName).execute();
  }

  /**
   * Checks the return mode and the restrictions in order to determine whether the query should
   * return <code>null</code>.
   *
   * @return
   */
  public boolean shouldReturnNull() {
    return returnModeForEmptyRestrictions == ReturnMode.RETURN_NULL && !hasRestrictions();
  }

  protected boolean hasRestrictions() {
    return !restrictions.isEmpty();
  }

  public Class<? extends E> getTargetClass() {
    return targetClass;
  }

  public Set<Criterion> getRestrictions() {
    return restrictions;
  }

  public List<Order> getOrders() {
    return orders;
  }

}
