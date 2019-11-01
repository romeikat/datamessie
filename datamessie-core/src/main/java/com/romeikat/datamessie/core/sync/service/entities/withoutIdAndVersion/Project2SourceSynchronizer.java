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
import com.romeikat.datamessie.core.base.dao.impl.Project2SourceDao;
import com.romeikat.datamessie.core.domain.entity.impl.Project2SourceImpl;
import com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion.EntityWithoutIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;
import com.romeikat.datamessie.model.core.Project2Source;

public class Project2SourceSynchronizer
    extends EntityWithoutIdAndVersionSynchronizer<Project2Source> {

  public Project2SourceSynchronizer(final ApplicationContext ctx) {
    super(Project2SourceImpl.class, ctx);
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected void copyProperties(final Project2Source source, final Project2Source target) {
    target.setProjectId(source.getProjectId());
    target.setSourceId(source.getSourceId());
  }

  @Override
  protected EntityDao<Project2Source> getDao(final ApplicationContext ctx) {
    return ctx.getBean(Project2SourceDao.class);
  }

}
