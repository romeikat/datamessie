package com.romeikat.datamessie.core.sync.service.template.withIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.impl.FooEntityWithGeneratedIdAndVersionDao;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class FooEntityWithGeneratedIdAndVersionSynchronizer
    extends EntityWithIdAndVersionSynchronizer<FooEntityWithGeneratedIdAndVersion> {

  public FooEntityWithGeneratedIdAndVersionSynchronizer(final ApplicationContext ctx) {
    super(FooEntityWithGeneratedIdAndVersion.class, ctx);
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return true;
  }

  @Override
  protected void copyProperties(final FooEntityWithGeneratedIdAndVersion source,
      final FooEntityWithGeneratedIdAndVersion target) {
    target.setName(source.getName());
    target.setActive(source.getActive());
  }

  @Override
  protected EntityWithIdAndVersionDao<FooEntityWithGeneratedIdAndVersion> getDao(
      final ApplicationContext ctx) {
    return ctx.getBean(FooEntityWithGeneratedIdAndVersionDao.class);
  }

}
