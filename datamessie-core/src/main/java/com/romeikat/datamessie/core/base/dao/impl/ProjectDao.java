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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.entity.impl.Project2UserImpl;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.model.core.Project;
import com.romeikat.datamessie.model.core.Project2User;

@Repository
public class ProjectDao extends AbstractEntityWithIdAndVersionDao<Project> {

  public ProjectDao() {
    super(ProjectImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "name";
  }

  public Project create(final long id, final String name, final boolean crawlingEnabled,
      final boolean preprocessingEnabled) {
    return new ProjectImpl(id, name, crawlingEnabled, preprocessingEnabled);
  }

  public List<Long> getIdsForUser(final SharedSessionContract ssc, final Long userId) {
    // Query: Project2User
    final EntityQuery<Project2User> project2UserQuery = new EntityQuery<>(Project2UserImpl.class);
    project2UserQuery.addRestriction(Restrictions.eq("userId", userId));
    final List<Long> projectIds = project2UserQuery.listIdsForProperty(ssc, "projectId");
    return projectIds;
  }

  public List<ProjectDto> getAllAsDtos(final SharedSessionContract ssc, final Long userId) {
    // Restrict to user
    final Collection<Long> projectIdsForUser = getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Project
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(ProjectImpl.class);
    projectQuery.addIdRestriction(projectIdsForUser);
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

  public ProjectDto getAsDto(final SharedSessionContract ssc, final long projectId,
      final Long userId) {
    // Restrict to user
    final Collection<Long> projectIdsForUser = getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return null;
    }

    // Query: Project
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(ProjectImpl.class);
    projectQuery.addRestriction(Restrictions.idEq(projectId));
    projectQuery.addIdRestriction(projectIdsForUser);
    projectQuery.addOrder(Order.desc("started"));
    projectQuery.setResultTransformer(new AliasToBeanResultTransformer(ProjectDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("name"), "name");
    projectionList.add(Projections.property("crawlingEnabled"), "crawlingEnabled");
    projectionList.add(Projections.property("crawlingInterval"), "crawlingInterval");
    projectionList.add(Projections.property("preprocessingEnabled"), "preprocessingEnabled");
    final ProjectDto dto = (ProjectDto) projectQuery.uniqueForProjection(ssc, projectionList);
    return dto;
  }

}
