package com.romeikat.datamessie.core.base.dao.impl;

import java.util.Collection;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
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
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;

@Repository
public class StemmedContentDao extends AbstractEntityWithIdAndVersionDao<StemmedContent> {

  public StemmedContentDao() {
    super(StemmedContent.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "documentId";
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
