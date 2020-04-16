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
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.cache.ILazyCache;
import com.romeikat.datamessie.core.base.cache.NamedEntityName2CategoriesCache;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import jersey.repackaged.com.google.common.collect.Sets;

@Repository
public class NamedEntityCategoryDao
    extends AbstractEntityWithIdAndVersionCachingDao<NamedEntityCategory> {

  // Cache: NamedEntity name -> category NamedEntity names
  private final NamedEntityName2CategoriesCache namedEntityName2CategoriesCache;

  @Autowired
  private NamedEntityDao namedEntityDao;

  public NamedEntityCategoryDao() {
    super(NamedEntityCategory.class);

    namedEntityName2CategoriesCache = new NamedEntityName2CategoriesCache();
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  @Override
  protected Collection<ILazyCache<?, ?, ?>> getLazyCaches() {
    return Sets.newHashSet(namedEntityName2CategoriesCache);
  }

  @Override
  public void insert(final StatelessSession statelessSession,
      final NamedEntityCategory namedEntityCategory) {
    super.insert(statelessSession, namedEntityCategory);

    invalidateNamedEntityName(statelessSession, namedEntityCategory);
  }

  private void invalidateNamedEntityName(final SharedSessionContract ssc,
      final NamedEntityCategory namedEntityCategory) {
    final long namedEntityId = namedEntityCategory.getNamedEntityId();
    final NamedEntity namedEntity = namedEntityDao.getEntity(ssc, namedEntityId);
    final String namedEntityName = namedEntity.getName();
    namedEntityName2CategoriesCache.invalidateKey(namedEntityName);
  }

  public Set<String> getWithoutCategories(final SharedSessionContract ssc,
      final Collection<String> namedEntityNames) {
    final Map<String, Set<String>> namedEntityNames2Categories =
        namedEntityName2CategoriesCache.getValues(ssc, namedEntityNames);
    final Set<String> withCategories = namedEntityNames2Categories.entrySet().stream()
        .filter(e -> !e.getValue().isEmpty()).map(e -> e.getKey()).collect(Collectors.toSet());

    final Set<String> withoutCategories = Sets.newHashSet(namedEntityNames);
    withoutCategories.removeAll(withCategories);
    return withoutCategories;
  }

  public Set<String> getNamedEntityCategoryNames(final SharedSessionContract ssc,
      final String namedEntityName) {
    final Set<String> namedEntityCategoryNames =
        namedEntityName2CategoriesCache.getValue(ssc, namedEntityName);
    return namedEntityCategoryNames == null ? Collections.emptySet() : namedEntityCategoryNames;
  }

  public List<NamedEntityCategory> getByNamedEntity(final SharedSessionContract ssc,
      final NamedEntity namedEntity) {
    return getEntitesByProperty(ssc, "namedEntityId", namedEntity.getId());
  }

}
