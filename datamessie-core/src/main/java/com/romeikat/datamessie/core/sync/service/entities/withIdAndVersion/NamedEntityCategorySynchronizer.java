package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

import java.util.Set;
import java.util.function.Predicate;

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

import org.springframework.context.ApplicationContext;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class NamedEntityCategorySynchronizer
    extends EntityWithIdAndVersionSynchronizer<NamedEntityCategory> {

  // Input
  private final NamedEntityOccurrenceSynchronizer namedEntityOccurrenceSynchronizer;

  // Output
  private final Set<Long> categoryNamedEntityIds = Sets.newHashSet();

  public NamedEntityCategorySynchronizer(
      final NamedEntityOccurrenceSynchronizer namedEntityOccurrenceSynchronizer,
      final ApplicationContext ctx) {
    super(NamedEntityCategory.class, ctx);
    this.namedEntityOccurrenceSynchronizer = namedEntityOccurrenceSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateProcessedData();
  }

  @Override
  protected Predicate<NamedEntityCategory> getLhsEntityFilter() {
    return namedEntityCategory -> namedEntityOccurrenceSynchronizer.getNamedEntityIds()
        .contains(namedEntityCategory.getNamedEntityId());
  }

  @Override
  protected void copyProperties(final NamedEntityCategory source,
      final NamedEntityCategory target) {
    target.setNamedEntityId(source.getNamedEntityId());
    target.setCategoryNamedEntityId(source.getCategoryNamedEntityId());

    // Output
    categoryNamedEntityIds.add(source.getCategoryNamedEntityId());
  }

  @Override
  protected EntityWithIdAndVersionDao<NamedEntityCategory> getDao(final ApplicationContext ctx) {
    return ctx.getBean(NamedEntityCategoryDao.class);
  }

  public Set<Long> getCategoryNamedEntityIds() {
    return categoryNamedEntityIds;
  }

}
