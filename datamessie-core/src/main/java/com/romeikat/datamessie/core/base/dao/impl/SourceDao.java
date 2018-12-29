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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.query.document.DocumentFilterSettingsQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.dto.RedirectingRuleDto;
import com.romeikat.datamessie.core.domain.dto.SourceDto;
import com.romeikat.datamessie.core.domain.dto.SourceOverviewDto;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;
import com.romeikat.datamessie.core.domain.dto.TagSelectingRuleDto;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;

@Repository
public class SourceDao extends AbstractEntityWithIdAndVersionDao<Source> {

  @Autowired
  private ProjectDao projectDao;

  @Autowired
  private RedirectingRuleDao redirectingRuleDao;

  @Autowired
  private TagSelectingRuleDao tagSelectingRuleDao;

  @Autowired
  private SourceTypeDao sourceTypeDao;

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  public SourceDao() {
    super(Source.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "name";
  }

  public Map<Document, Source> getForDocuments(final SharedSessionContract ssc,
      final Collection<Document> documents) {
    final Set<Long> sourceIds =
        documents.stream().map(d -> d.getSourceId()).collect(Collectors.toSet());
    final Map<Long, Source> sourcesById = getIdsWithEntities(ssc, sourceIds);

    final Map<Document, Source> result = Maps.newHashMapWithExpectedSize(documents.size());
    for (final Document document : documents) {
      final Source source = sourcesById.get(document.getSourceId());
      result.put(document, source);
    }
    return result;
  }

  public List<Long> getIds(final SharedSessionContract ssc, final Long projectId,
      final Boolean visible) {
    if (projectId == null) {
      return Collections.emptyList();
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    if (visible != null) {
      sourceQuery.addRestriction(Restrictions.eq("visible", visible));
    }

    // Done
    final List<Long> sourceIds2 = sourceQuery.listIds(ssc);
    return sourceIds2;
  }

  public Long count(final SharedSessionContract ssc, final Long userId, final Long projectId) {
    if (projectId == null) {
      return 0l;
    }

    // Restrict to user
    final Collection<Long> projectIdsForUser = projectDao.getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return 0l;
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    final Long count = project2SourceQuery.count(ssc, "projectId");
    return count;
  }

  public SourceDto getAsDto(final SharedSessionContract ssc, final Long userId, final Long id) {
    if (id == null) {
      return null;
    }

    // Restrict to user
    final Collection<Long> projectIdsForUser = projectDao.getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return null;
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.in("projectId", projectIdsForUser));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return null;
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.idEq(id));
    sourceQuery.addRestriction(Restrictions.in("id", sourceIds));

    final Source source = sourceQuery.uniqueObject(ssc);
    return sourceToDto(ssc, source);
  }

  public List<SourceDto> getAsDtos(final SharedSessionContract ssc, final Long userId,
      final Long projectId, final Boolean visible) {
    if (projectId == null) {
      return Collections.emptyList();
    }

    // Restrict to user
    final Collection<Long> projectIdsForUser = projectDao.getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    if (visible != null) {
      sourceQuery.addRestriction(Restrictions.eq("visible", visible));
    }
    sourceQuery.addOrder(Order.asc("name"));

    // Done
    final List<Source> sources = sourceQuery.listObjects(ssc);

    // Transform
    final List<SourceDto> entitesAsDtos = Lists.transform(sources, s -> sourceToDto(ssc, s));
    return Lists.newArrayList(entitesAsDtos);
  }

  public List<SourceOverviewDto> getAsOverviewDtos(final SharedSessionContract ssc,
      final Long userId, final Long projectId, final Boolean visible, final Long first,
      final Long count) {
    if (projectId == null) {
      return Collections.emptyList();
    }

    // Restrict to user
    final Collection<Long> projectIdsForUser = projectDao.getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    project2SourceQuery.addRestriction(Restrictions.in("projectId", projectIdsForUser));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    if (visible != null) {
      sourceQuery.addRestriction(Restrictions.eq("visible", visible));
    }
    sourceQuery.setFirstResult(first == null ? null : first.intValue());
    sourceQuery.setMaxResults(count == null ? null : count.intValue());
    sourceQuery.addOrder(Order.asc("name"));

    // Done
    final List<Source> sources = sourceQuery.listObjects(ssc);

    // Transform
    final List<SourceOverviewDto> dtos = Lists.transform(sources, s -> sourceToOverviewDto(ssc, s));
    return Lists.newArrayList(dtos);
  }

  public List<SourceOverviewDto> getAsOverviewDtos(final SharedSessionContract ssc,
      final Long userId, final Long projectId, final Long sourceId,
      final Collection<Long> sourceTypeIds) {
    if (projectId == null) {
      return Collections.emptyList();
    }

    // Restrict to user
    final Collection<Long> projectIdsForUser = projectDao.getIdsForUser(ssc, userId);
    if (projectIdsForUser.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Project2Source
    Collection<Long> sourceIds = null;
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    project2SourceQuery.addRestriction(Restrictions.in("projectId", projectIdsForUser));
    sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Source2SourceType
    Collection<Long> sourceIds2 = null;
    if (CollectionUtils.isNotEmpty(sourceTypeIds)) {
      final EntityQuery<Source2SourceType> source2SourceTypeQuery =
          new EntityQuery<>(Source2SourceType.class);
      source2SourceTypeQuery.addRestriction(Restrictions.in("sourceTypeId", sourceTypeIds));
      sourceIds2 = source2SourceTypeQuery.listIdsForProperty(ssc, "sourceId");
      if (sourceIds2.isEmpty()) {
        return Collections.emptyList();
      }
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    if (sourceId != null) {
      sourceQuery.addRestriction(Restrictions.idEq(sourceId));
    }
    if (sourceIds != null) {
      sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    }
    if (sourceIds2 != null) {
      sourceQuery.addRestriction(Restrictions.in("id", sourceIds2));
    }
    sourceQuery.addOrder(Order.asc("name"));

    // Done
    final List<Source> sources = sourceQuery.listObjects(ssc);

    // Transform
    final List<SourceOverviewDto> dtos = Lists.transform(sources, s -> sourceToOverviewDto(ssc, s));
    return Lists.newArrayList(dtos);
  }

  public List<Source> getOfProject(final SharedSessionContract ssc, final Long projectId) {
    if (projectId == null) {
      return Collections.emptyList();
    }

    // Query: Project2Source
    final EntityQuery<Project2Source> project2SourceQuery = new EntityQuery<>(Project2Source.class);
    project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    sourceQuery.addOrder(Order.asc("name"));

    // Done
    final List<Source> sources = sourceQuery.listObjects(ssc);
    return sources;
  }

  public List<Source> getOfSourceType(final SharedSessionContract ssc, final long sourceTypeId) {
    // Query: Source2SourceType
    final EntityQuery<Source2SourceType> project2SourceQuery =
        new EntityQuery<>(Source2SourceType.class);
    project2SourceQuery.addRestriction(Restrictions.eq("sourceTypeId", sourceTypeId));
    final List<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");

    final List<Source> sources = getEntities(ssc, sourceIds);
    return sources;
  }

  public List<Source> get(final SharedSessionContract ssc, final DocumentsFilterSettings dfs) {
    // Only use source-specific information
    final DocumentsFilterSettings dfsForQuery = new DocumentsFilterSettings();
    dfsForQuery.setProjectId(dfs.getProjectId());
    dfsForQuery.setSourceId(dfs.getSourceId());
    dfsForQuery.setSourceTypeIds(dfs.getSourceTypeIds());
    dfsForQuery.setSourceVisible(dfs.getSourceVisible());
    dfsForQuery.setProjectId(dfs.getProjectId());

    // Query for sources
    final DocumentFilterSettingsQuery<Source> query =
        new DocumentFilterSettingsQuery<Source>(dfs, Source.class, sharedBeanProvider);
    query.addOrder(Order.asc("name"));
    final List<Source> sources = query.listObjects(ssc);
    return sources;
  }

  private SourceDto sourceToDto(final SharedSessionContract ssc, final Source source) {
    if (source == null) {
      return null;
    }

    final SourceDto dto = new SourceDto();
    dto.setId(source.getId());
    dto.setName(source.getName());
    dto.setLanguage(source.getLanguage());
    final List<SourceTypeDto> sourceTypeDtos = sourceTypeDao.getAsDtos(ssc, source.getId());
    dto.setTypes(sourceTypeDtos);
    dto.setUrl(source.getUrl());
    final List<RedirectingRuleDto> redirectingRules =
        redirectingRuleDao.getAsDtos(ssc, source.getId());
    dto.setRedirectingRules(redirectingRules);
    final List<TagSelectingRuleDto> tagSelectingRules =
        tagSelectingRuleDao.getAsDtos(ssc, source.getId());
    dto.setTagSelectingRules(tagSelectingRules);
    dto.setNumberOfRedirectingRules(redirectingRules.size());
    dto.setNumberOfTagSelectingRules(tagSelectingRules.size());
    dto.setVisible(source.getVisible());
    dto.setStatisticsChecking(source.getStatisticsChecking());
    return dto;
  }

  private SourceOverviewDto sourceToOverviewDto(final SharedSessionContract ssc,
      final Source source) {
    if (source == null) {
      return null;
    }

    final SourceOverviewDto dto = new SourceOverviewDto();
    dto.setId(source.getId());
    dto.setName(source.getName());
    dto.setLanguage(source.getLanguage());
    final List<SourceTypeDto> sourceTypeDtos = sourceTypeDao.getAsDtos(ssc, source.getId());
    dto.setTypes(sourceTypeDtos);
    dto.setUrl(source.getUrl());
    final List<RedirectingRuleDto> redirectingRules =
        redirectingRuleDao.getAsDtos(ssc, source.getId());
    final List<TagSelectingRuleDto> tagSelectingRules =
        tagSelectingRuleDao.getAsDtos(ssc, source.getId());
    dto.setNumberOfRedirectingRules(redirectingRules.size());
    dto.setNumberOfTagSelectingRules(tagSelectingRules.size());
    dto.setVisible(source.getVisible());
    dto.setStatisticsChecking(source.getStatisticsChecking());
    return dto;
  }

}
