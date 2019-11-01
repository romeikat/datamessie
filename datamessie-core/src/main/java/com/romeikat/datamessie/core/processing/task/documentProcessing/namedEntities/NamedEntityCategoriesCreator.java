package com.romeikat.datamessie.core.processing.task.documentProcessing.namedEntities;

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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetNamedEntityNamesWithoutCategoryCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetOrCreateNamedEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.ProvideNamedEntityCategoryTitlesCallback;
import com.romeikat.datamessie.model.core.NamedEntity;
import com.romeikat.datamessie.model.core.NamedEntityCategory;
import jersey.repackaged.com.google.common.collect.Lists;

public class NamedEntityCategoriesCreator {

  private static final Logger LOG = LoggerFactory.getLogger(NamedEntityCategoriesCreator.class);

  private final NamedEntityCategoryDao namedEntityCategoryDao;
  private final Double processingParallelismFactor;
  private final SessionFactory sessionFactory;

  private final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback;
  private final GetNamedEntityNamesWithoutCategoryCallback getNamedEntityNamesWithoutCategoryCallback;
  private final ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback;

  public NamedEntityCategoriesCreator(
      final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback,
      final GetNamedEntityNamesWithoutCategoryCallback getNamedEntityNamesWithoutCategoryCallback,
      final ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback,
      final ApplicationContext ctx) {
    namedEntityCategoryDao = ctx.getBean(NamedEntityCategoryDao.class);
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);

    this.getOrCreateNamedEntitiesCallback = getOrCreateNamedEntitiesCallback;
    this.getNamedEntityNamesWithoutCategoryCallback = getNamedEntityNamesWithoutCategoryCallback;
    this.provideNamedEntityCategoryTitlesCallback = provideNamedEntityCategoryTitlesCallback;
  }

  public List<NamedEntityCategory> createNamedEntityCategories(
      final Map<String, Long> namedEntityNames2NamedEntityId) {
    if (provideNamedEntityCategoryTitlesCallback == null) {
      return Collections.emptyList();
    }

    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Determine categories for named entities without one
    final Collection<String> namedEntityNames = namedEntityNames2NamedEntityId.keySet();
    final Collection<String> namedEntityNamesWithoutCategory =
        getNamedEntityNamesWithoutCategoryCallback
            .getWithoutCategories(sessionProvider.getStatelessSession(), namedEntityNames);
    final Multimap<String, String> namedEntityNames2NamedEntityCategoryNames =
        Multimaps.synchronizedSetMultimap(HashMultimap.create());
    new ParallelProcessing<String>(null, namedEntityNamesWithoutCategory,
        processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final String namedEntityName) {
        try {
          final Collection<String> namedEntityCategoryNames =
              getNamedEntityCategoryNames(namedEntityName);
          namedEntityNames2NamedEntityCategoryNames.putAll(namedEntityName,
              namedEntityCategoryNames);
        } catch (final Exception e) {
          LOG.error("Could not determine named entity categories for named entity {}",
              namedEntityName);
        }
      }
    };

    // Create category named entities
    final Collection<String> categoryNamedEntityNames =
        namedEntityNames2NamedEntityCategoryNames.values();
    final Map<String, Long> categoryNamedEntityNames2CategoryNamedEntityId =
        getOrCreateNamedEntitiesCallback.getOrCreate(sessionProvider.getStatelessSession(),
            categoryNamedEntityNames);

    sessionProvider.closeStatelessSession();

    // Create named entity categories
    final List<NamedEntityCategory> namedEntityCategories = Lists.newLinkedList();
    for (final Entry<String, String> entry : namedEntityNames2NamedEntityCategoryNames.entries()) {
      final String namedEntityName = entry.getKey();
      final String namedEntityCategoryName = entry.getValue();

      final long namedEntityId = namedEntityNames2NamedEntityId.get(namedEntityName);
      final long categoryNamedEntityId =
          categoryNamedEntityNames2CategoryNamedEntityId.get(namedEntityCategoryName);
      final NamedEntityCategory namedEntityCategory =
          createNamedEntityCategory(namedEntityId, categoryNamedEntityId);
      namedEntityCategories.add(namedEntityCategory);
    }
    return namedEntityCategories;
  }

  private NamedEntityCategory createNamedEntityCategory(final long namedEntityId,
      final long categoryNamedEntityId) {
    final NamedEntityCategory namedEntityCategory = namedEntityCategoryDao.create();
    namedEntityCategory.setNamedEntityId(namedEntityId);
    namedEntityCategory.setCategoryNamedEntityId(categoryNamedEntityId);
    return namedEntityCategory;
  }

  protected Set<String> getNamedEntityCategoryNames(final String namedEntityName) {
    // Determine categories
    final String nameAsSingleWord = NamedEntity.getAsSingleWord(namedEntityName);
    final List<String> namedEntityCategoryNames =
        provideNamedEntityCategoryTitlesCallback.provideCategoryTitles(nameAsSingleWord);

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
