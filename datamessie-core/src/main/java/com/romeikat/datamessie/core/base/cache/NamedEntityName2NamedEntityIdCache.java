package com.romeikat.datamessie.core.base.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityImpl;

public class NamedEntityName2NamedEntityIdCache
    extends AbstractLazyCache<String, Long, SharedSessionContract> {

  /**
   * We assume an average named entity name of 25 characters. Thus, each cache entry requires 25*2 +
   * 8 = 58 bytes. One million entries require about 60 MB.
   */
  private static final int MAX_SIZE = 1000000;

  public NamedEntityName2NamedEntityIdCache() {
    super(MAX_SIZE);
  }

  @Override
  protected Map<String, Long> loadValues(final SharedSessionContract ssc,
      final Collection<String> namedEntityNames) {
    // NamedEntity name -> NamedEntity ID
    final Map<String, Long> namedEntityIds = getNamedEntityIds(ssc, namedEntityNames);
    return namedEntityIds;
  }

  private Map<String, Long> getNamedEntityIds(final SharedSessionContract ssc,
      final Collection<String> namedEntityNames) {
    final EntityWithIdQuery<NamedEntity> namedEntityQuery =
        new EntityWithIdQuery<>(NamedEntityImpl.class);
    namedEntityQuery.addRestriction(Restrictions.in("name", namedEntityNames));
    final ProjectionList projectionList = Projections.projectionList()
        .add(Projections.property("name")).add(Projections.property("id"));
    final List<Object[]> rows =
        (List<Object[]>) namedEntityQuery.listForProjection(ssc, projectionList);

    final Map<String, Long> result =
        rows.stream().collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
    return result;
  }

}
