package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class DownloadSynchronizer extends EntityWithIdAndVersionSynchronizer<Download> {

  // Input
  private final DocumentSynchronizer documentSynchronizer;

  public DownloadSynchronizer(final DocumentSynchronizer documentSynchronizer,
      final ApplicationContext ctx) {
    super(Download.class, ctx);
    this.documentSynchronizer = documentSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Download> getLhsEntityFilter() {
    return download -> documentSynchronizer.getDocumentIds().contains(download.getDocumentId());
  }

  @Override
  protected void copyProperties(final Download source, final Download target) {
    target.setUrl(source.getUrl());
    target.setSourceId(source.getSourceId());
    target.setDocumentId(source.getDocumentId());
    target.setSuccess(source.getSuccess());
  }

  @Override
  protected EntityWithIdAndVersionDao<Download> getDao(final ApplicationContext ctx) {
    return ctx.getBean(DownloadDao.class);
  }

}
