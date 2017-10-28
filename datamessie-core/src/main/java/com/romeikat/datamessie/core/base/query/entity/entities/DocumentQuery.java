package com.romeikat.datamessie.core.base.query.entity.entities;

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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;

import org.hibernate.criterion.Restrictions;
import org.springframework.util.CollectionUtils;

import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.entity.impl.Document;

public class DocumentQuery extends EntityWithIdQuery<Document> {

  private final DocumentsFilterSettings dfs;
  private final Collection<? extends Collection<Long>> idRestrictions;

  public DocumentQuery(final DocumentsFilterSettings dfs, final Collection<? extends Collection<Long>> idRestrictions) {
    super(Document.class);

    this.dfs = dfs;
    this.idRestrictions = idRestrictions;

    addRestrictions();
  }

  private void addRestrictions() {
    // Published
    if (dfs.getFromDate() != null) {
      addRestriction(Restrictions.ge("published", LocalDateTime.of(dfs.getFromDate(), LocalTime.MIDNIGHT)));
    }
    if (dfs.getToDate() != null) {
      addRestriction(Restrictions.lt("published", LocalDateTime.of(dfs.getToDate(), LocalTime.MIDNIGHT).plusDays(1)));
    }

    // States
    if (!CollectionUtils.isEmpty(dfs.getStates())) {
      addRestriction(Restrictions.in("state", dfs.getStates()));
    }

    // Document IDs from DFS
    final Collection<Long> documentIds = dfs.getDocumentIds();
    final boolean documentIdsApply = documentIds != null;
    if (documentIdsApply) {
      addIdRestriction(documentIds);
    }

    // Additional document IDs
    for (final Collection<Long> idRestriction : idRestrictions) {
      addIdRestriction(idRestriction);
    }
  }

}
