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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.entity.impl.Project;

@Repository
public class ProjectDao extends AbstractEntityWithIdAndVersionDao<Project> {

  public ProjectDao() {
    super(Project.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "name";
  }

  public List<ProjectDto> getAllAsDtos(final SharedSessionContract ssc) {
    // Query: Project
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(Project.class);
    projectQuery.addOrder(Order.asc("name"));
    projectQuery.setResultTransformer(new AliasToBeanResultTransformer(ProjectDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("name"), "name");
    projectionList.add(Projections.property("crawlingEnabled"), "crawlingEnabled");
    projectionList.add(Projections.property("crawlingInterval"), "crawlingInterval");
    projectionList.add(Projections.property("preprocessingEnabled"), "preprocessingEnabled");
    @SuppressWarnings("unchecked")
    final List<ProjectDto> dtos =
        (List<ProjectDto>) projectQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

  public ProjectDto getAsDto(final SharedSessionContract sharedSessionContract,
      final long projectId) {
    // Query: Project
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(Project.class);
    projectQuery.addRestriction(Restrictions.idEq(projectId));
    projectQuery.addOrder(Order.desc("started"));
    projectQuery.setResultTransformer(new AliasToBeanResultTransformer(ProjectDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("name"), "name");
    projectionList.add(Projections.property("crawlingEnabled"), "crawlingEnabled");
    projectionList.add(Projections.property("crawlingInterval"), "crawlingInterval");
    projectionList.add(Projections.property("preprocessingEnabled"), "preprocessingEnabled");
    final ProjectDto dto =
        (ProjectDto) projectQuery.uniqueForProjection(sharedSessionContract, projectionList);
    return dto;
  }

}
