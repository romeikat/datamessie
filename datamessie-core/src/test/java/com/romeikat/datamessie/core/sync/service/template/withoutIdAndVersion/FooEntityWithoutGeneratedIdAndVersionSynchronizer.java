package com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.impl.FooEntityWithoutGeneratedIdAndVersionDao;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithoutIdAndVersion;
import com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion.EntityWithoutIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class FooEntityWithoutGeneratedIdAndVersionSynchronizer
    extends EntityWithoutIdAndVersionSynchronizer<FooEntityWithoutIdAndVersion> {

  public FooEntityWithoutGeneratedIdAndVersionSynchronizer(final ApplicationContext ctx) {
    super(FooEntityWithoutIdAndVersion.class, ctx);
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return true;
  }

  @Override
  protected void copyProperties(final FooEntityWithoutIdAndVersion source,
      final FooEntityWithoutIdAndVersion target) {
    target.setName(source.getName());
  }

  @Override
  protected EntityDao<FooEntityWithoutIdAndVersion> getDao(final ApplicationContext ctx) {
    return ctx.getBean(FooEntityWithoutGeneratedIdAndVersionDao.class);
  }

}
