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
import java.util.Collections;
import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;
import com.romeikat.datamessie.core.domain.entity.Source2SourceType;
import com.romeikat.datamessie.core.domain.entity.SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceTypeImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceTypeImpl;

@Repository
public class SourceTypeDao extends AbstractEntityWithIdAndVersionDao<SourceType> {

  public SourceTypeDao() {
    super(SourceTypeImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "name";
  }

  public List<SourceTypeDto> getAsDtos(final SharedSessionContract ssc) {
    // Query: SourceType
    final EntityWithIdQuery<SourceType> sourceQuery = new EntityWithIdQuery<>(SourceTypeImpl.class);
    sourceQuery.addOrder(Order.asc("name"));
    sourceQuery.setResultTransformer(new AliasToBeanResultTransformer(SourceTypeDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("name"), "name");
    @SuppressWarnings("unchecked")
    final List<SourceTypeDto> dtos =
        (List<SourceTypeDto>) sourceQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

  public List<SourceTypeDto> getAsDtos(final SharedSessionContract ssc, final long sourceId) {
    // Query: Source2SourceType
    final EntityQuery<Source2SourceType> project2SourceQuery =
        new EntityQuery<>(Source2SourceTypeImpl.class);
    project2SourceQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    final List<Long> sourceTypeIds = project2SourceQuery.listIdsForProperty(ssc, "sourceTypeId");
    if (sourceTypeIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: SourceType
    final EntityQuery<SourceType> sourceTypeQuery = new EntityQuery<>(SourceTypeImpl.class);
    sourceTypeQuery.addRestriction(Restrictions.in("id", sourceTypeIds));
    sourceTypeQuery.addOrder(Order.asc("name"));
    sourceTypeQuery.setResultTransformer(new AliasToBeanResultTransformer(SourceTypeDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("name"), "name");
    @SuppressWarnings("unchecked")
    final List<SourceTypeDto> dtos =
        (List<SourceTypeDto>) sourceTypeQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

  public List<SourceType> getOfSource(final SharedSessionContract ssc, final long sourceId) {
    // Query: Source2SourceType
    final EntityQuery<Source2SourceType> project2SourceQuery =
        new EntityQuery<>(Source2SourceTypeImpl.class);
    project2SourceQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    final List<Long> sourceTypeIds = project2SourceQuery.listIdsForProperty(ssc, "sourceTypeId");

    final List<SourceType> sourceTypes = getEntities(ssc, sourceTypeIds);
    return sourceTypes;
  }

}
