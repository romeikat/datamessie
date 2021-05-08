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
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class SourceSynchronizer extends EntityWithIdAndVersionSynchronizer<Source> {

  // Input
  private static final Set<Long> SOURCE_IDS =
      Sets.newHashSet(200l, 199l, 196l, 5l, 8l, 19l, 1l, 24l, 90l, 30l, 660l, 226l, 227l);

  // Output
  private final Set<Long> sourceIds = Sets.newHashSet();

  public SourceSynchronizer(final ApplicationContext ctx) {
    super(Source.class, ctx);
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Long> getLhsIdFilter() {
    return sourceId -> SOURCE_IDS == null ? true : SOURCE_IDS.contains(sourceId);
  }

  @Override
  protected void copyProperties(final Source source, final Source target) {
    target.setName(source.getName());
    target.setLanguage(source.getLanguage());
    target.setUrl(source.getUrl());
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
