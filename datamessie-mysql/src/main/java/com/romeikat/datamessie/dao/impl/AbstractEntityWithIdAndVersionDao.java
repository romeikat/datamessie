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
import java.util.List;
import java.util.TreeMap;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.dao.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.model.EntityWithIdAndVersion;
import com.romeikat.datamessie.model.util.IdAndVersion;

public abstract class AbstractEntityWithIdAndVersionDao<E extends EntityWithIdAndVersion>
    extends AbstractEntityWithIdDao<E> implements EntityWithIdAndVersionDao<E> {

  public AbstractEntityWithIdAndVersionDao(final Class<? extends E> entityClass) {
    super(entityClass);
  }

  @Override
  public TreeMap<Long, Long> getIdsWithVersion(final Long firstId, final Integer maxResults) {
    // Query
    final EntityWithIdQuery<E> query = new EntityWithIdQuery<>(getEntityClass());
    if (firstId != null) {
      query.addRestriction(Restrictions.ge("id", firstId));
    }
    query.setMaxResults(maxResults);
    query.addOrder(Order.asc("id"));
    query.setResultTransformer(new AliasToBeanResultTransformer(IdAndVersion.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("version"), "version");
    @SuppressWarnings("unchecked")
    final List<IdAndVersion> idsAndVersions =
        (List<IdAndVersion>) query.listForProjection(projectionList);

    // Transform into map
    final TreeMap<Long, Long> result = transformIntoMap(idsAndVersions);
    return result;
  }

  @Override
  public TreeMap<Long, Long> getIdsWithVersion(final Collection<Long> ids) {
    if (ids.isEmpty()) {
      return new TreeMap<Long, Long>();
    }

    // Query
    final EntityWithIdQuery<E> query = new EntityWithIdQuery<>(getEntityClass());
    query.addRestriction(Restrictions.in("id", ids));
    query.setResultTransformer(new AliasToBeanResultTransformer(IdAndVersion.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("version"), "version");
    @SuppressWarnings("unchecked")
    final List<IdAndVersion> idsAndVersions =
        (List<IdAndVersion>) query.listForProjection(projectionList);

    // Transform into map
    final TreeMap<Long, Long> result = transformIntoMap(idsAndVersions);
    return result;
  }

  private TreeMap<Long, Long> transformIntoMap(final List<IdAndVersion> idsAndVersions) {
    final TreeMap<Long, Long> result = Maps.newTreeMap();
    for (final IdAndVersion idAndVersion : idsAndVersions) {
      result.put(idAndVersion.getId(), idAndVersion.getVersion());
    }
    return result;
  }

}
