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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Project2User;

@Repository
public class ProjectDao extends AbstractEntityWithIdAndVersionDao<Project> {

  @Autowired
  private CrawlingDao crawlingDao;

  public ProjectDao() {
    super(Project.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "name";
  }

  public Map<Document, Project> getForDocuments(final SharedSessionContract ssc,
      final Collection<Document> documents) {
    final Set<Long> crawlingIds =
        documents.stream().map(d -> d.getCrawlingId()).collect(Collectors.toSet());
    final Map<Long, Crawling> crawlingsById = crawlingDao.getIdsWithEntities(ssc, crawlingIds);

    final Set<Long> projectIds =
        crawlingsById.values().stream().map(c -> c.getProjectId()).collect(Collectors.toSet());
    final Map<Long, Project> projectsById = getIdsWithEntities(ssc, projectIds);

    final Map<Document, Project> result = Maps.newHashMapWithExpectedSize(documents.size());
    for (final Document document : documents) {
      final Crawling crawling = crawlingsById.get(document.getCrawlingId());
      if (crawling == null) {
        continue;
      }

      final Project project = projectsById.get(crawling.getProjectId());
      if (project == null) {
        continue;
      }

      result.put(document, project);
    }
    return result;
  }

  public List<Long> getIdsForUser(final SharedSessionContract ssc, final Long userId) {
    // Query: Project2User
    final EntityQuery<Project2User> project2UserQuery = new EntityQuery<>(Project2User.class);
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
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(Project.class);
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
    projectionList.add(Projections.property("cleaningMethod"), "cleaningMethod");
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
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(Project.class);
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
    projectionList.add(Projections.property("cleaningMethod"), "cleaningMethod");
    final ProjectDto dto = (ProjectDto) projectQuery.uniqueForProjection(ssc, projectionList);
    return dto;
  }

}
