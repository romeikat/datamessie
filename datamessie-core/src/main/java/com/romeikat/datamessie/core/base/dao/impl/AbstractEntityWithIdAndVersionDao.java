package com.romeikat.datamessie.core.base.dao.impl;

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
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;
import com.romeikat.datamessie.core.domain.util.IdAndVersion;

public abstract class AbstractEntityWithIdAndVersionDao<E extends EntityWithIdAndVersion>
    extends AbstractEntityWithIdDao<E> implements EntityWithIdAndVersionDao<E> {

  public AbstractEntityWithIdAndVersionDao(final Class<E> entityClass) {
    super(entityClass);
  }

  @Override
  public Map<Long, Long> getIdsWithVersion(final SharedSessionContract ssc,
      final Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }

    // Query
    final EntityQuery<E> query = new EntityQuery<>(getEntityClass());
    query.addRestriction(Restrictions.in("id", ids));
    query.setResultTransformer(new AliasToBeanResultTransformer(IdAndVersion.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("version"), "version");
    @SuppressWarnings("unchecked")
    final List<IdAndVersion> idsAndVersions =
        (List<IdAndVersion>) query.listForProjection(ssc, projectionList);

    // Transform into map
    final Map<Long, Long> result = transformIntoMap(idsAndVersions);
    return result;
  }

  private Map<Long, Long> transformIntoMap(final List<IdAndVersion> idsAndVersions) {
    final Map<Long, Long> result = Maps.newHashMapWithExpectedSize(idsAndVersions.size());
    for (final IdAndVersion idAndVersion : idsAndVersions) {
      result.put(idAndVersion.getId(), idAndVersion.getVersion());
    }
    return result;
  }

  @Override
  public Map<Long, Long> getIdsWithVersion(final SharedSessionContract ssc,
      final Integer firstResult, final Integer maxResults) {
    // Query
    final EntityQuery<E> query = new EntityQuery<>(getEntityClass());
    query.setFirstResult(firstResult);
    query.setMaxResults(maxResults);
    query.setResultTransformer(new AliasToBeanResultTransformer(IdAndVersion.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("version"), "version");
    @SuppressWarnings("unchecked")
    final List<IdAndVersion> idsAndVersions =
        (List<IdAndVersion>) query.listForProjection(ssc, projectionList);

    // Transform into map
    final Map<Long, Long> result = transformIntoMap(idsAndVersions);
    return result;
  }

}
