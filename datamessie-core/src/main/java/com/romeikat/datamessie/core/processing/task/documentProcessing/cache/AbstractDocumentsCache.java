package com.romeikat.datamessie.core.processing.task.documentProcessing.cache;

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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.SharedSessionContract;

import com.romeikat.datamessie.core.base.dao.EntityWithIdDao;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;
import com.romeikat.datamessie.core.domain.entity.impl.Document;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

public abstract class AbstractDocumentsCache<E extends EntityWithId> {

  private final SharedSessionContract ssc;
  private final EntityWithIdDao<E> dao;

  private final Collection<Document> documents;
  private final Map<Long, E> documentsWithEntities;

  public AbstractDocumentsCache(final SharedSessionContract ssc, final EntityWithIdDao<E> dao,
      final Collection<Document> documents) {
    this.ssc = ssc;
    this.dao = dao;
    this.documents = documents;
    documentsWithEntities = Maps.newHashMapWithExpectedSize(documents.size());

    loadObjects();
  }

  private void loadObjects() {
    final Collection<Long> entityIds = getEntityIds();
    final Map<Long, E> entities = dao.getIdsWithEntities(ssc, entityIds);
    for (final Document document : documents) {
      final long entityId = getEntityId(document);
      final E entity = entities.get(entityId);
      documentsWithEntities.put(document.getId(), entity);
    }
  }

  private Set<Long> getEntityIds() {
    final Set<Long> entityIds = Sets.newHashSetWithExpectedSize(documents.size());
    for (final Document document : documents) {
      final Long entityId = getEntityId(document);
      if (entityId != null) {
        entityIds.add(entityId);
      }
    }
    return entityIds;
  }

  protected abstract Long getEntityId(Document document);

  public E get(final long documentId) {
    return documentsWithEntities.get(documentId);
  }

}
