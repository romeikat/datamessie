package com.romeikat.datamessie.dao.query.entity.entities;

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

import org.hibernate.criterion.Restrictions;
import org.springframework.util.CollectionUtils;
import com.romeikat.datamessie.dao.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.model.core.SourceType;
import com.romeikat.datamessie.model.core.impl.SourceTypeImpl;
import com.romeikat.datamessie.model.util.DocumentsFilterSettings;

public class SourceTypeQuery extends EntityWithIdQuery<SourceType> {

  private final DocumentsFilterSettings dfs;

  public SourceTypeQuery(final DocumentsFilterSettings dfs) {
    super(SourceTypeImpl.class);

    this.dfs = dfs;

    addRestrictions();
  }

  private void addRestrictions() {
    if (!CollectionUtils.isEmpty(dfs.getSourceTypeIds())) {
      addRestriction(Restrictions.in("id", dfs.getSourceTypeIds()));
    }
  }

}
