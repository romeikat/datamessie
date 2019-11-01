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
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.domain.entity.impl.Project2SourceImpl;
import com.romeikat.datamessie.model.core.Project2Source;

@Repository
public class Project2SourceDao extends AbstractEntityDao<Project2Source> {

  public Project2SourceDao() {
    super(Project2SourceImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public Project2Source create(final long projectId, final long sourceId) {
    return new Project2SourceImpl(projectId, sourceId);
  }

}
