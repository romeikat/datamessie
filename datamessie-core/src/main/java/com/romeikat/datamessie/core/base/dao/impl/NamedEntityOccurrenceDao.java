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
import java.util.Collection;
import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import jersey.repackaged.com.google.common.collect.Lists;

@Repository
public class NamedEntityOccurrenceDao
    extends AbstractEntityWithIdAndVersionDao<NamedEntityOccurrence> {

  public NamedEntityOccurrenceDao() {
    super(NamedEntityOccurrence.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public List<NamedEntityOccurrence> getByDocument(final SharedSessionContract ssc,
      final long documentId) {
    return getEntitesByProperty(ssc, "documentId", documentId);
  }

  public NamedEntityOccurrence getByNamedEntityAndTypeAndDocument(final SharedSessionContract ssc,
      final long namedEntityId, final NamedEntityType type, final long documentId) {
    final EntityWithIdQuery<NamedEntityOccurrence> query =
        new EntityWithIdQuery<>(NamedEntityOccurrence.class);
    query.addRestriction(Restrictions.eq("namedEntityId", namedEntityId));
    query.addRestriction(Restrictions.eq("type", type));
    query.addRestriction(Restrictions.eq("documentId", documentId));
    return query.uniqueObject(ssc);
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

  public void deleteForDocument(final StatelessSession statelessSession, final long documentId) {
    deleteForDocuments(statelessSession, Lists.newArrayList(documentId));
  }

}
