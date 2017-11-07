package com.romeikat.datamessie.core.base.cache;

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
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;

public class NamedEntityName2CategoriesCache
    extends AbstractLazyCache<String, Set<String>, SharedSessionContract> {

  @Override
  protected Set<String> loadValue(final SharedSessionContract ssc, final String namedEntityName) {
    // NamedEntity name -> ID
    final Long namedEntityId = getNamedEntityId(ssc, namedEntityName);
    if (namedEntityId == null) {
      return Collections.emptySet();
    }

    // NamedEntity ID -> NamedEntityCategories
    final Collection<NamedEntityCategory> namedEntityCategories =
        getNamedEntityCategories(ssc, namedEntityId);
    registerDependencies(namedEntityCategories, namedEntityName);

    // NamedEntityCategories -> category NamedEntity IDs
    final Collection<Long> categoryNamedEntityIds = getNamedEntityIds(ssc, namedEntityCategories);
    if (categoryNamedEntityIds.isEmpty()) {
      return Collections.emptySet();
    }

    // Category NamedEntity IDs -> Category NamedEntities
    final Collection<NamedEntity> namedEntities = getNamedEntites(ssc, categoryNamedEntityIds);
    if (namedEntities.isEmpty()) {
      return Collections.emptySet();
    }

    // Category NamedEntities -> category NamedEntity names
    final Set<String> categoryNamedEntityNames = getCategoryNamedEntityNames(namedEntities);
    return categoryNamedEntityNames;
  }

  private Long getNamedEntityId(final SharedSessionContract ssc, final String namedEntityName) {
    final EntityWithIdQuery<NamedEntity> namedEntityQuery =
        new EntityWithIdQuery<>(NamedEntity.class);
    namedEntityQuery.addRestriction(Restrictions.eq("name", namedEntityName));
    final Long namedEntityId = namedEntityQuery.uniqueId(ssc);
    return namedEntityId;
  }

  private Collection<NamedEntityCategory> getNamedEntityCategories(final SharedSessionContract ssc,
      final Long namedEntityId) {
    final EntityQuery<NamedEntityCategory> namedEntityCategoryQuery =
        new EntityQuery<>(NamedEntityCategory.class);
    namedEntityCategoryQuery.addRestriction(Restrictions.eq("namedEntityId", namedEntityId));
    final Collection<NamedEntityCategory> namedEntityCategories =
        namedEntityCategoryQuery.listObjects(ssc);
    return namedEntityCategories;
  }

  private void registerDependencies(final Collection<NamedEntityCategory> namedEntityCategories,
      final String namedEntityName) {
    final Collection<Long> namedEntityCategoryIds =
        namedEntityCategories.stream().map(nec -> nec.getId()).collect(Collectors.toSet());
    registerDependencies(namedEntityCategoryIds, namedEntityName);
  }

  private Collection<Long> getNamedEntityIds(final SharedSessionContract ssc,
      final Collection<NamedEntityCategory> namedEntityCategories) {
    final Collection<Long> categoryNamedEntityIds = namedEntityCategories.stream()
        .map(nec -> nec.getCategoryNamedEntityId()).collect(Collectors.toSet());
    return categoryNamedEntityIds;
  }

  private Collection<NamedEntity> getNamedEntites(final SharedSessionContract ssc,
      final Collection<Long> categoryNamedEntityIds) {
    final EntityQuery<NamedEntity> namedEntityQuery = new EntityQuery<>(NamedEntity.class);
    namedEntityQuery.addRestriction(Restrictions.in("id", categoryNamedEntityIds));
    final Collection<NamedEntity> namedEntities = namedEntityQuery.listObjects(ssc);
    return namedEntities;
  }

  private Set<String> getCategoryNamedEntityNames(final Collection<NamedEntity> namedEntities) {
    final Set<String> categoryNamedEntityNames =
        namedEntities.stream().map(ne -> ne.getName()).collect(Collectors.toSet());
    return categoryNamedEntityNames;
  }

}
