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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategoryImpl;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityImpl;
import jersey.repackaged.com.google.common.collect.Maps;

public class NamedEntityName2CategoriesCache
    extends AbstractLazyCache<String, Set<String>, SharedSessionContract> {

  @Override
  protected Map<String, Set<String>> loadValues(final SharedSessionContract ssc,
      final Collection<String> namedEntityNames) {
    // NamedEntity name -> NamedEntity ID
    final BiMap<String, Long> namedEntityNames2NamedEntityId =
        getNamedEntityNames2NamedEntityId(ssc, namedEntityNames);
    if (namedEntityNames2NamedEntityId.isEmpty()) {
      return Collections.emptyMap();
    }

    // NamedEntity name -> category NamedEntity IDs
    final Map<String, Set<Long>> namedEntityNames2CategoryNamedEntityIds =
        getNamedEntityNames2CategoryNamedEntityIds(ssc, namedEntityNames2NamedEntityId);
    if (namedEntityNames2CategoryNamedEntityIds.isEmpty()) {
      return Collections.emptyMap();
    }

    // NamedEntity name -> category NamedEntity names
    final Map<String, Set<String>> namedEntityNames2CategoryNamedEntityNames =
        getNamedEntityNames2CategoryNamedEntityNames(ssc, namedEntityNames2CategoryNamedEntityIds);
    return namedEntityNames2CategoryNamedEntityNames;
  }

  private BiMap<String, Long> getNamedEntityNames2NamedEntityId(final SharedSessionContract ssc,
      final Collection<String> namedEntityNames) {
    // Query named entities for the given names
    final EntityWithIdQuery<NamedEntity> namedEntityQuery =
        new EntityWithIdQuery<>(NamedEntityImpl.class);
    namedEntityQuery.addRestriction(Restrictions.in("name", namedEntityNames));
    final ProjectionList projectionList = Projections.projectionList()
        .add(Projections.property("name")).add(Projections.property("id"));
    final List<Object[]> rows =
        (List<Object[]>) namedEntityQuery.listForProjection(ssc, projectionList);

    // Map named entity names to named entity IDs
    final BiMap<String, Long> result = rows.stream().collect(Collectors
        .toMap(row -> (String) row[0], row -> (Long) row[1], (a, b) -> b, HashBiMap::create));
    return result;
  }

  private Map<String, Set<Long>> getNamedEntityNames2CategoryNamedEntityIds(
      final SharedSessionContract ssc, final BiMap<String, Long> namedEntityNames2NamedEntityId) {
    // Query named entity categories for the the given named entity IDs
    final EntityWithIdQuery<NamedEntityCategory> namedEntityCategoryQuery =
        new EntityWithIdQuery<>(NamedEntityCategoryImpl.class);
    namedEntityCategoryQuery
        .addRestriction(Restrictions.in("namedEntityId", namedEntityNames2NamedEntityId.values()));
    final Collection<NamedEntityCategory> namedEntityCategories =
        namedEntityCategoryQuery.listObjects(ssc);

    // Map named entity name with named entity category IDs (for registering dependencies)
    final HashMultimap<String, Long> namedEntityNames2NamedEntityCategoryIds =
        HashMultimap.create();
    for (final NamedEntityCategory namedEntityCategory : namedEntityCategories) {
      final long namedEntityId = namedEntityCategory.getNamedEntityId();
      final String namedEntityName = namedEntityNames2NamedEntityId.inverse().get(namedEntityId);
      namedEntityNames2NamedEntityCategoryIds.put(namedEntityName, namedEntityId);
    }
    // Register dependencies
    for (final String namedEntityName : namedEntityNames2NamedEntityCategoryIds.keySet()) {
      final Collection<Long> namedEntityCategoryIds =
          namedEntityNames2NamedEntityCategoryIds.get(namedEntityName);
      registerDependencies(namedEntityCategoryIds, namedEntityName);
    }

    // Map named entity IDs with category named entity IDs
    final HashMultimap<Long, Long> namedEntityIds2CategoryNamedEntityIds = HashMultimap.create();
    for (final NamedEntityCategory namedEntityCategory : namedEntityCategories) {
      namedEntityIds2CategoryNamedEntityIds.put(namedEntityCategory.getNamedEntityId(),
          namedEntityCategory.getCategoryNamedEntityId());
    }

    // Map named entity names with category named entity IDs
    final Map<String, Set<Long>> result =
        namedEntityNames2NamedEntityId.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
            e -> namedEntityIds2CategoryNamedEntityIds.get(e.getValue())));
    return result;
  }

  private Map<String, Set<String>> getNamedEntityNames2CategoryNamedEntityNames(
      final SharedSessionContract ssc,
      final Map<String, Set<Long>> namedEntityNames2CategoryNamedEntityIds) {
    // Determine all category named entity IDs
    final Set<Long> allNamedEntityIds = Sets.newHashSet();
    for (final Set<Long> namedEntityIds : namedEntityNames2CategoryNamedEntityIds.values()) {
      allNamedEntityIds.addAll(namedEntityIds);
    }

    // Query named entities for the given category named entity IDs
    final EntityWithIdQuery<NamedEntity> namedEntityQuery =
        new EntityWithIdQuery<>(NamedEntityImpl.class);
    namedEntityQuery.addRestriction(Restrictions.in("id", allNamedEntityIds));
    final List<NamedEntity> namedEntities = namedEntityQuery.listObjects(ssc);

    // Map category named entity IDs to category named entity name
    final Map<Long, String> categoryNamedEntityIds2CategoryNamedEntityName =
        namedEntities.stream().collect(Collectors.toMap(ne -> ne.getId(), ne -> ne.getName()));

    // Map named entity names with category named entity names
    final Map<String, Set<String>> result =
        Maps.newHashMapWithExpectedSize(namedEntityNames2CategoryNamedEntityIds.size());
    for (final Entry<String, Set<Long>> entry : namedEntityNames2CategoryNamedEntityIds
        .entrySet()) {
      final String namedEntityName = entry.getKey();
      final Set<Long> categoryNamedEntityIds = entry.getValue();
      final Set<String> categoryNamedEntityNames = categoryNamedEntityIds.stream()
          .map(s -> categoryNamedEntityIds2CategoryNamedEntityName.get(s))
          .collect(Collectors.toSet());
      result.put(namedEntityName, categoryNamedEntityNames);
    }
    return result;
  }

}
