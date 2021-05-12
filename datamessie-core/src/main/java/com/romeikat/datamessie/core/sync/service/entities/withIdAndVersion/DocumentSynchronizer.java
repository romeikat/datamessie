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
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class DocumentSynchronizer extends EntityWithIdAndVersionSynchronizer<Document> {

  // Input
  private final SourceSynchronizer sourceSynchronizer;

  // Output
  private final Set<Long> documentIds = Sets.newHashSet();
  private final Set<Long> crawlingIds = Sets.newHashSet();

  public DocumentSynchronizer(final SourceSynchronizer sourceSynchronizer,
      final ApplicationContext ctx) {
    super(Document.class, ctx);
    this.sourceSynchronizer = sourceSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Document> getLhsEntityFilter() {
    return document -> sourceSynchronizer.getSourceIds().contains(document.getSourceId());
  }

  @Override
  protected void copyProperties(final Document source, final Document target) {
    target.setTitle(source.getTitle());
    target.setStemmedTitle(getTargetStemmedTitle(source));
    target.setUrl(source.getUrl());
    target.setDescription(source.getDescription());
    target.setStemmedDescription(getTargetStemmedDescription(source));
    target.setPublished(source.getPublished());
    target.setDownloaded(source.getDownloaded());
    target.setState(getTargetState(source));
    target.setStatusCode(source.getStatusCode());
    target.setCrawlingId(source.getCrawlingId());
    target.setSourceId(source.getSourceId());

    // Output
    documentIds.add(source.getId());
    crawlingIds.add(source.getCrawlingId());
  }

  private String getTargetStemmedTitle(final Document source) {
    if (getSyncData().shouldUpdateProcessedData()) {
      return source.getStemmedTitle();
    } else {
      return null;
    }
  }

  private String getTargetStemmedDescription(final Document source) {
    if (getSyncData().shouldUpdateProcessedData()) {
      return source.getStemmedDescription();
    } else {
      return null;
    }
  }

  private DocumentProcessingState getTargetState(final Document source) {
    if (getSyncData().shouldUpdateProcessedData()) {
      return source.getState();
    } else {
      // CLEANED => back to REDIRECTED
      // STEMMED => back to REDIRECTED
      return source.getState() == DocumentProcessingState.CLEANED
          || source.getState() == DocumentProcessingState.STEMMED
              ? DocumentProcessingState.REDIRECTED
              : source.getState();
    }
  }

  @Override
  protected EntityWithIdAndVersionDao<Document> getDao(final ApplicationContext ctx) {
    return ctx.getBean("documentDao", DocumentDao.class);
  }

  public Set<Long> getDocumentIds() {
    return documentIds;
  }

  public Set<Long> getCrawlingIds() {
    return crawlingIds;
  }

}
