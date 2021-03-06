package com.romeikat.datamessie.core.base.dao.impl;

import java.util.Collection;
import java.util.Collections;
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
import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.impl.Download;

@Repository
public class DownloadDao extends AbstractEntityWithIdAndVersionDao<Download> {

  public DownloadDao() {
    super(Download.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "url";
  }

  public List<Download> getForDocument(final SharedSessionContract ssc, final long documentId) {
    // Query: Download
    final EntityWithIdQuery<Download> downloadQuery = new EntityWithIdQuery<>(Download.class);
    downloadQuery.addRestriction(Restrictions.eq("documentId", documentId));

    // Done
    final List<Download> downloads = downloadQuery.listObjects(ssc);
    return downloads;
  }

  public Multimap<Long, Download> getForDocuments(final SharedSessionContract ssc,
      final Collection<Long> documentIds) {
    // Query: Download
    final EntityWithIdQuery<Download> downloadQuery = new EntityWithIdQuery<>(Download.class);
    downloadQuery.addRestriction(Restrictions.in("documentId", documentIds));

    // Done
    final List<Download> downloads = downloadQuery.listObjects(ssc);
    final Multimap<Long, Download> result = Multimaps.index(downloads, d -> d.getDocumentId());
    return result;
  }

  public Download getForUrlAndSource(final SharedSessionContract ssc, final String url,
      final long sourceId) {
    if (url == null) {
      return null;
    }

    // Query: Download
    final EntityWithIdQuery<Download> downloadQuery = new EntityWithIdQuery<>(Download.class);
    downloadQuery.addRestriction(Restrictions.eq("url", url));
    downloadQuery.addRestriction(Restrictions.eq("sourceId", sourceId));

    // Done
    final Download download = downloadQuery.uniqueObject(ssc);
    return download;
  }

  public List<Long> getDocumentIdsForUrlsAndSource(final SharedSessionContract ssc,
      final Collection<String> urls, final long sourceId) {
    if (urls.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Download
    final EntityWithIdQuery<Download> downloadQuery = new EntityWithIdQuery<>(Download.class);
    downloadQuery.addRestriction(Restrictions.in("url", urls));
    downloadQuery.addRestriction(Restrictions.eq("sourceId", sourceId));

    // Done
    final List<Long> documentIds = downloadQuery.listIdsForProperty(ssc, "documentId");
    return documentIds;
  }

  public void deleteForDocuments(final StatelessSession statelessSession,
      final Collection<Long> documentIds) {
    if (documentIds.isEmpty()) {
      return;
    }

    final String hql =
        "delete from " + getEntityClass().getSimpleName() + " where documentId IN :_documentIds";
    final Query<?> query = statelessSession.createQuery(hql);
    query.setParameter("_documentIds", documentIds);
    query.executeUpdate();
  }

}
