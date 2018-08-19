package com.romeikat.datamessie.core.base.dao.impl;

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
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
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

}
