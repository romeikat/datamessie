package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.CollectionUtils;
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
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class SourceSynchronizer extends EntityWithIdAndVersionSynchronizer<Source> {

  // Input
  private final List<Long> sourceIdsForSync;

  // Output
  private final Set<Long> sourceIds = Sets.newHashSet();

  public SourceSynchronizer(final List<Long> sourceIdsForSync, final ApplicationContext ctx) {
    super(Source.class, ctx);
    this.sourceIdsForSync = sourceIdsForSync;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Pair<Long, Long>> getLhsIdFilter() {
    return idAndVersion -> CollectionUtils.isEmpty(sourceIdsForSync) ? true
        : sourceIdsForSync.contains(idAndVersion.getKey());
  }

  @Override
  protected void copyProperties(final Source source, final Source target) {
    target.setName(source.getName());
    target.setLanguage(source.getLanguage());
    target.setUrl(source.getUrl());
    target.setUserAgent(source.getUserAgent());
    target.setCookie(source.getCookie());
    target.setCrawlingEnabled(source.getCrawlingEnabled());
    target.setVisible(source.getVisible());
    target.setStatisticsChecking(source.getStatisticsChecking());
    target.setNotes(source.getNotes());

    // Output
    sourceIds.add(source.getId());
  }

  @Override
  protected EntityWithIdAndVersionDao<Source> getDao(final ApplicationContext ctx) {
    return ctx.getBean(SourceDao.class);
  }

  public Set<Long> getSourceIds() {
    return sourceIds;
  }

}
