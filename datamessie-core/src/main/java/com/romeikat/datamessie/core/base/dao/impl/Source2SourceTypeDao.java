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
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;

@Repository
public class Source2SourceTypeDao extends AbstractEntityDao<Source2SourceType> {

  @Autowired
  private SessionFactory sessionFactory;

  public Source2SourceTypeDao() {
    super(Source2SourceType.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public List<Source2SourceType> getForSourceId(final SharedSessionContract ssc,
      final long sourceId) {
    // Query
    final Criteria criteria = ssc.createCriteria(Source2SourceType.class);
    criteria.add(Restrictions.eq("sourceId", sourceId));
    // Done
    @SuppressWarnings("unchecked")
    final List<Source2SourceType> entities = criteria.list();
    return entities;
  }

  public List<Source2SourceType> getForProjectId(final SharedSessionContract ssc,
      final long projectId) {
    // Query
    final Criteria criteria = ssc.createCriteria(Source2SourceType.class);
    criteria.add(Restrictions.eq("projectId", projectId));
    // Done
    @SuppressWarnings("unchecked")
    final List<Source2SourceType> entities = criteria.list();
    return entities;
  }

}
