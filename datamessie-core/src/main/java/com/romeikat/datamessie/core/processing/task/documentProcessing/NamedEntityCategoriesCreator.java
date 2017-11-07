package com.romeikat.datamessie.core.processing.task.documentProcessing;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.INamedEntityCategoryProider;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import jersey.repackaged.com.google.common.collect.Sets;

@Service
public class NamedEntityCategoriesCreator {

  @Autowired
  @Qualifier("namedEntityDao")
  private NamedEntityDao namedEntityDao;

  @Autowired
  @Qualifier("namedEntityCategoryDao")
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Autowired
  private ApplicationContext ctx;

  public synchronized void createNamedEntityCategories(final StatelessSession statelessSession,
      final Collection<NamedEntityOccurrence> namedEntityOccurrences) {
    if (namedEntityOccurrences == null) {
      return;
    }

    // Get named entities
    final Collection<NamedEntity> namedEntities =
        getNamedEntitiesForCategoriesCreation(statelessSession, namedEntityOccurrences);

    // Create categories for each named entity
    for (final NamedEntity namedEntity : namedEntities) {
      // Skip if categories for that named entity have already been created
      final boolean categoriesAlreadyCreated =
          namedEntityCategoryDao.hasNamedEntityCategories(statelessSession, namedEntity.getName());
      if (categoriesAlreadyCreated) {
        continue;
      }

      // Create categories for that named entity
      createNamedEntityCategories(statelessSession, namedEntity);
    }
  }

  private Collection<NamedEntity> getNamedEntitiesForCategoriesCreation(
      final StatelessSession statelessSession,
      final Collection<NamedEntityOccurrence> namedEntityOccurrences) {
    final Set<Long> namedEntityIds = Sets.newHashSet();
    // Collect named entities for which categories should be created
    for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
      final Long namedEntityId = namedEntityOccurrence.getNamedEntityId();
      namedEntityIds.add(namedEntityId);
    }
    // Load named entities
    final Collection<NamedEntity> namedEntities =
        namedEntityDao.getEntities(statelessSession, namedEntityIds);
    // Done
    return namedEntities;
  }

  private void createNamedEntityCategories(final StatelessSession statelessSession,
      final NamedEntity namedEntity) {
    final Set<String> namedEntityCategoryNames = getNamedEntityCategoryNames(namedEntity);
    for (final String namedEntityCategoryName : namedEntityCategoryNames) {
      createNamedEntityCategory(statelessSession, namedEntity, namedEntityCategoryName);
    }
  }

  private void createNamedEntityCategory(final StatelessSession statelessSession,
      final NamedEntity namedEntity, final String namedEntityCategoryName) {
    final long categoryNamedEntityId =
        namedEntityDao.getOrCreate(statelessSession, namedEntityCategoryName);
    final NamedEntityCategory namedEntityCategory = new NamedEntityCategory();
    namedEntityCategory.setNamedEntityId(namedEntity.getId());
    namedEntityCategory.setCategoryNamedEntityId(categoryNamedEntityId);
    namedEntityCategoryDao.insert(statelessSession, namedEntityCategory);
  }

  protected Set<String> getNamedEntityCategoryNames(final NamedEntity namedEntity) {
    final INamedEntityCategoryProider plugin =
        DateMessiePlugins.getInstance(ctx).getOrLoadPlugin(INamedEntityCategoryProider.class);
    if (plugin == null) {
      return Collections.emptySet();
    }

    // Determine categories
    final String nameAsSingleWord = NamedEntity.getAsSingleWord(namedEntity.getName());
    final List<String> namedEntityCategoryNames = plugin.provideCategoryTitles(nameAsSingleWord);

    // Translate categories into names for named entities
    final Set<String> categoriesAsNamedEntityNames = new HashSet<String>();
    for (final String namedEntityCategoryName : namedEntityCategoryNames) {
      final String namedEntityCategoryNameAsMultipleWords =
          NamedEntity.getAsMultipleWords(namedEntityCategoryName);
      categoriesAsNamedEntityNames.add(namedEntityCategoryNameAsMultipleWords);
    }

    // Done
    return categoriesAsNamedEntityNames;
  }

}
