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
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrenceImpl;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;

@Repository
public class NamedEntityOccurrenceDao
    extends AbstractEntityWithIdAndVersionDao<NamedEntityOccurrence> {

  public NamedEntityOccurrenceDao() {
    super(NamedEntityOccurrenceImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public NamedEntityOccurrence create(final long id, final long namedEntityId,
      final long parentNamedEntityId, final NamedEntityType type, final int quantity,
      final long documentId) {
    return new NamedEntityOccurrenceImpl(id, namedEntityId, parentNamedEntityId, type, quantity,
        documentId);
  }

  public List<NamedEntityOccurrence> getByDocument(final SharedSessionContract ssc,
      final long documentId) {
    return getEntitesByProperty(ssc, "documentId", documentId);
  }

  public NamedEntityOccurrence getByNamedEntityAndTypeAndDocument(final SharedSessionContract ssc,
      final long namedEntityId, final NamedEntityType type, final long documentId) {
    final EntityWithIdQuery<NamedEntityOccurrence> query =
        new EntityWithIdQuery<>(NamedEntityOccurrenceImpl.class);
    query.addRestriction(Restrictions.eq("namedEntityId", namedEntityId));
    query.addRestriction(Restrictions.eq("type", type));
    query.addRestriction(Restrictions.eq("documentId", documentId));
    return query.uniqueObject(ssc);
  }

  public void deleteForDocument(final StatelessSession statelessSession, final long documentId) {
    final String hql =
        "delete from " + getEntityClass().getSimpleName() + " where documentId = :_documentId";
    final Query<?> query = statelessSession.createQuery(hql);
    query.setParameter("_documentId", documentId);
    query.executeUpdate();
  }

}
