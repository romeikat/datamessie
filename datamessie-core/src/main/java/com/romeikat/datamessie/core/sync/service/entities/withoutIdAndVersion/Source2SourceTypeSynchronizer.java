package com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion;

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

import com.romeikat.datamessie.core.base.dao.EntityDao;
import com.romeikat.datamessie.core.base.dao.impl.Source2SourceTypeDao;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;
import com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion.EntityWithoutIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class Source2SourceTypeSynchronizer extends EntityWithoutIdAndVersionSynchronizer<Source2SourceType> {

  public Source2SourceTypeSynchronizer(final ApplicationContext ctx) {
    super(Source2SourceType.class, ctx);
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected void copyProperties(final Source2SourceType source, final Source2SourceType target) {
    target.setSourceId(source.getSourceId());
    target.setSourceTypeId(source.getSourceTypeId());
  }

  @Override
  protected EntityDao<Source2SourceType> getDao(final ApplicationContext ctx) {
    return ctx.getBean(Source2SourceTypeDao.class);
  }

}
