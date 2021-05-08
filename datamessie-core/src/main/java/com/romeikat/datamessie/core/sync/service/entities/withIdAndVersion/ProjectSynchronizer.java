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
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Project2SourceSynchronizer;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class ProjectSynchronizer extends EntityWithIdAndVersionSynchronizer<Project> {

  // Input
  private final Project2SourceSynchronizer project2SourceSynchronizer;

  // Output
  private final Set<Long> projectIds = Sets.newHashSet();

  public ProjectSynchronizer(final Project2SourceSynchronizer project2SourceSynchronizer,
      final ApplicationContext ctx) {
    super(Project.class, ctx);
    this.project2SourceSynchronizer = project2SourceSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Long> getLhsIdFilter() {
    return projectId -> project2SourceSynchronizer.getProjectIds().contains(projectId);
  }

  @Override
  protected void copyProperties(final Project source, final Project target) {
    target.setName(source.getName());
    target.setCrawlingEnabled(source.getCrawlingEnabled());
    target.setCrawlingInterval(source.getCrawlingInterval());
    target.setPreprocessingEnabled(source.getPreprocessingEnabled());
    target.setCleaningMethod(source.getCleaningMethod());

    // Output
    projectIds.add(source.getId());
  }

  @Override
  protected EntityWithIdAndVersionDao<Project> getDao(final ApplicationContext ctx) {
    return ctx.getBean(ProjectDao.class);
  }

  public Set<Long> getProjectIds() {
    return projectIds;
  }

}
