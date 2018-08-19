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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Projection;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryCountExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryListForProjectionExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryListIdsExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryListIdsOfPropertyExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryListObjectsExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryUniqueForProjectionExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryUniqueIdExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryUniqueIdOfPropertyExecutor;
import com.romeikat.datamessie.core.base.query.entity.execute.entityWithId.EntityWithIdQueryUniqueObjectExecutor;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;
import jersey.repackaged.com.google.common.collect.Lists;

public class EntityWithIdQuery<E extends EntityWithId> extends EntityQuery<E> {

  private final List<Set<Long>> idRestrictions;

  public EntityWithIdQuery(final Class<E> targetClass) {
    super(targetClass);

    idRestrictions = Lists.newLinkedList();
  }

  public EntityWithIdQuery(final EntityWithIdQuery<E> other) {
    super(other);

    idRestrictions = Lists.newLinkedList(other.idRestrictions);
  }

  public EntityWithIdQuery<E> addIdRestriction(final Collection<Long> ids) {
    idRestrictions.add(Sets.newHashSet(ids));

    return this;
  }

  @Override
  public List<E> listObjects(final SharedSessionContract ssc) {
    return new EntityWithIdQueryListObjectsExecutor<>(ssc, this).execute();
  }

  public List<Long> listIds(final SharedSessionContract ssc) {
    return new EntityWithIdQueryListIdsExecutor<>(ssc, this).execute();
  }

  @Override
  public List<Long> listIdsForProperty(final SharedSessionContract ssc, final String propertyName) {
    return new EntityWithIdQueryListIdsOfPropertyExecutor<>(ssc, this, propertyName).execute();
  }

  @Override
  public List<?> listForProjection(final SharedSessionContract ssc, final Projection projection) {
    return new EntityWithIdQueryListForProjectionExecutor<>(ssc, this, projection).execute();
  }

  @Override
  public E uniqueObject(final SharedSessionContract ssc) {
    return new EntityWithIdQueryUniqueObjectExecutor<>(ssc, this).execute();
  }

  /**
   * Loads the ID of the respective object.
   *
   * @param ssc
   * @return
   */
  public Long uniqueId(final SharedSessionContract ssc) {
    return new EntityWithIdQueryUniqueIdExecutor<>(ssc, this).execute();
  }

  @Override
  public Long uniqueIdForProperty(final SharedSessionContract ssc, final String propertyName) {
    return new EntityWithIdQueryUniqueIdOfPropertyExecutor<>(ssc, this, propertyName).execute();
  }

  @Override
  public Object uniqueForProjection(final SharedSessionContract ssc, final Projection projection) {
    return new EntityWithIdQueryUniqueForProjectionExecutor<>(ssc, this, projection).execute();
  }

  public Long count(final SharedSessionContract ssc) {
    return new EntityWithIdQueryCountExecutor<>(ssc, this).execute();
  }

  @Override
  protected boolean hasRestrictions() {
    return super.hasRestrictions() || !idRestrictions.isEmpty();
  }

  public List<Set<Long>> getIdRestrictions() {
    return idRestrictions;
  }

}
