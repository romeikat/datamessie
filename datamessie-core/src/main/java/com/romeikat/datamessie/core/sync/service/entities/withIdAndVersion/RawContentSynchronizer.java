package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.hibernate.StatelessSession;

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
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class RawContentSynchronizer extends EntityWithIdAndVersionSynchronizer<RawContent> {

  // Input
  private final DocumentSynchronizer documentSynchronizer;

  public RawContentSynchronizer(final DocumentSynchronizer documentSynchronizer,
      final ApplicationContext ctx) {
    super(RawContent.class, ctx);
    this.documentSynchronizer = documentSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Long> getLhsIdFilter() {
    return rawContentId -> documentSynchronizer.getDocumentIds().contains(rawContentId);
  }

  @Override
  protected List<RawContent> loadLhsEntities(final Map<Long, Long> lhsIdsWithVersion,
      final StatelessSession lhsStatelessSession) {
    // TODO mock empty contents, then post-update them
    return super.loadLhsEntities(lhsIdsWithVersion, lhsStatelessSession);
  }

  @Override
  protected void copyProperties(final RawContent source, final RawContent target) {
    target.setContent(source.getContent());
    target.setDocumentId(source.getDocumentId());
  }

  @Override
  protected EntityWithIdAndVersionDao<RawContent> getDao(final ApplicationContext ctx) {
    return ctx.getBean(RawContentDao.class);
  }

}
