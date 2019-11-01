package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;
import com.romeikat.datamessie.model.core.Document;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public class DocumentSynchronizer extends EntityWithIdAndVersionSynchronizer<Document> {

  private final SyncData syncData;

  public DocumentSynchronizer(final ApplicationContext ctx) {
    super(DocumentImpl.class, ctx);
    syncData = SyncData.valueOf(SpringUtil.getPropertyValue(ctx, "sync.data"));
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected void copyProperties(final Document source, final Document target) {
    target.setTitle(source.getTitle());
    target.setStemmedTitle(source.getStemmedTitle());
    target.setUrl(source.getUrl());
    target.setDescription(source.getDescription());
    target.setStemmedDescription(source.getStemmedDescription());
    target.setPublished(source.getPublished());
    target.setDownloaded(source.getDownloaded());

    final DocumentProcessingState sourceState = source.getState();
    if (syncData.shouldUpdateOriginalData()) {
      DocumentProcessingState targetState = sourceState;
      if (targetState == DocumentProcessingState.CLEANED
          || targetState == DocumentProcessingState.STEMMED) {
        targetState = DocumentProcessingState.REDIRECTED;
      }
      target.setState(targetState);
    } else if (syncData.shouldUpdateProcessedData()) {
      target.setState(sourceState);
    }

    target.setStatusCode(source.getStatusCode());
    target.setCrawlingId(source.getCrawlingId());
    target.setSourceId(source.getSourceId());
  }

  @Override
  protected EntityWithIdAndVersionDao<Document> getDao(final ApplicationContext ctx) {
    return ctx.getBean("documentDao", DocumentDao.class);
  }

}
