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
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class NamedEntityOccurrenceSynchronizer
    extends EntityWithIdAndVersionSynchronizer<NamedEntityOccurrence> {

  // Input
  private final DocumentSynchronizer documentSynchronizer;

  // Output
  private final Set<Long> namedEntityIds = Sets.newHashSet();

  public NamedEntityOccurrenceSynchronizer(final DocumentSynchronizer documentSynchronizer,
      final ApplicationContext ctx) {
    super(NamedEntityOccurrence.class, ctx);
    this.documentSynchronizer = documentSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateProcessedData();
  }

  @Override
  protected Predicate<NamedEntityOccurrence> getLhsEntityFilter() {
    return namedEntityOccurrence -> documentSynchronizer.getDocumentIds()
        .contains(namedEntityOccurrence.getDocumentId());
  }

  @Override
  protected void copyProperties(final NamedEntityOccurrence source,
      final NamedEntityOccurrence target) {
    target.setNamedEntityId(source.getNamedEntityId());
    target.setParentNamedEntityId(source.getParentNamedEntityId());
    target.setType(source.getType());
    target.setQuantity(source.getQuantity());
    target.setDocumentId(source.getDocumentId());

    // Output
    namedEntityIds.add(source.getNamedEntityId());
  }

  @Override
  protected EntityWithIdAndVersionDao<NamedEntityOccurrence> getDao(final ApplicationContext ctx) {
    return ctx.getBean("namedEntityOccurrenceDao", NamedEntityOccurrenceDao.class);
  }

  public Set<Long> getNamedEntityIds() {
    return namedEntityIds;
  }

}
