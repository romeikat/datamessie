package com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.EntityDao;
import com.romeikat.datamessie.core.base.dao.impl.Project2SourceDao;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.SourceSynchronizer;
import com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion.EntityWithoutIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class Project2SourceSynchronizer
    extends EntityWithoutIdAndVersionSynchronizer<Project2Source> {

  // Input
  private final SourceSynchronizer sourceSynchronizer;

  // Output
  private final Set<Long> projectIds = Sets.newHashSet();

  public Project2SourceSynchronizer(final SourceSynchronizer sourceSynchronizer,
      final ApplicationContext ctx) {
    super(Project2Source.class, ctx);
    this.sourceSynchronizer = sourceSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Project2Source> getLhsEntityFilter() {
    return project2Source -> sourceSynchronizer.getSourceIds()
        .contains(project2Source.getSourceId());
  }

  @Override
  protected void copyProperties(final Project2Source source, final Project2Source target) {
    target.setProjectId(source.getProjectId());
    target.setSourceId(source.getSourceId());

    // Output
    projectIds.add(source.getProjectId());
  }

  @Override
  protected EntityDao<Project2Source> getDao(final ApplicationContext ctx) {
    return ctx.getBean(Project2SourceDao.class);
  }

  public Set<Long> getProjectIds() {
    return projectIds;
  }

}
