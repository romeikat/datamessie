package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

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
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceTypeDao;
import com.romeikat.datamessie.core.domain.entity.impl.SourceType;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Source2SourceTypeSynchronizer;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class SourceTypeSynchronizer extends EntityWithIdAndVersionSynchronizer<SourceType> {

  // Input
  private final Source2SourceTypeSynchronizer source2SourceTypeSynchronizer;

  public SourceTypeSynchronizer(final Source2SourceTypeSynchronizer source2SourceTypeSynchronizer,
      final ApplicationContext ctx) {
    super(SourceType.class, ctx);
    this.source2SourceTypeSynchronizer = source2SourceTypeSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Pair<Long, Long>> getLhsIdFilter() {
    return idAndVersion -> source2SourceTypeSynchronizer.getSourceTypeIds()
        .contains(idAndVersion.getKey());
  }

  @Override
  protected void copyProperties(final SourceType source, final SourceType target) {
    target.setName(source.getName());
  }

  @Override
  protected EntityWithIdAndVersionDao<SourceType> getDao(final ApplicationContext ctx) {
    return ctx.getBean(SourceTypeDao.class);
  }

}
