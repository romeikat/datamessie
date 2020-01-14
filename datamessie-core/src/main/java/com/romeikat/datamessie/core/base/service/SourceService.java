package com.romeikat.datamessie.core.base.service;

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
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.DeletingRuleDao;
import com.romeikat.datamessie.core.base.dao.impl.Project2SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.dao.impl.RedirectingRuleDao;
import com.romeikat.datamessie.core.base.dao.impl.Source2SourceTypeDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceTypeDao;
import com.romeikat.datamessie.core.base.dao.impl.TagSelectingRuleDao;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.task.DocumentsDeprocessingTask;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.util.EntitiesById;
import com.romeikat.datamessie.core.base.util.EntitiesWithIdById;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.UpdateTracker;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransactionAndResult;
import com.romeikat.datamessie.core.domain.dto.DeletingRuleDto;
import com.romeikat.datamessie.core.domain.dto.RedirectingRuleDto;
import com.romeikat.datamessie.core.domain.dto.SourceDto;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;
import com.romeikat.datamessie.core.domain.dto.TagSelectingRuleDto;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Service
public class SourceService {

  private static final Logger LOG = LoggerFactory.getLogger(SourceService.class);

  @Autowired
  private TaskManager taskManager;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  @Qualifier("sourceTypeDao")
  private SourceTypeDao sourceTypeDao;

  @Autowired
  private ProjectDao projectDao;

  @Autowired
  private RedirectingRuleDao redirectingRuleDao;

  @Autowired
  private DeletingRuleDao deletingRuleDao;

  @Autowired
  private TagSelectingRuleDao tagSelectingRuleDao;

  @Autowired
  private Source2SourceTypeDao source2SourceTypeDao;

  @Autowired
  private Project2SourceDao project2SourceDao;

  @Autowired
  private StringUtil stringUtil;

  @Autowired
  private ApplicationContext ctx;

  public SourceDto createSource(final StatelessSession statelessSession, final Long userId,
      final Long projectId) {
    final SourceDto source = new ExecuteWithTransactionAndResult<SourceDto>(statelessSession) {
      @Override
      protected SourceDto executeWithResult(final StatelessSession statelessSession) {
        // Restrict to user
        final Collection<Long> projectIdsForUser =
            projectDao.getIdsForUser(statelessSession, userId);
        if (projectIdsForUser.isEmpty()) {
          return null;
        }


        // Create
        final String name = getNewName(statelessSession);
        final Source source = new Source(0, name, "", true, false);
        sourceDao.insert(statelessSession, source);

        // Assign
        if (projectId != null) {
          final Project2Source project2Source = new Project2Source(projectId, source.getId());
          project2SourceDao.insert(statelessSession, project2Source);
        }

        // Get
        return sourceDao.getAsDto(statelessSession, userId, source.getId());
      }

      @Override
      protected void onException(final Exception e) {
        final StringBuilder msg = new StringBuilder();
        msg.append("Could not create source");
        if (projectId != null) {
          msg.append(" for project ");
          msg.append(projectId);
        }
        LOG.error(msg.toString(), e);
      }
    }.execute();

    // Get
    return source;
  }

  public String getNewName(final SharedSessionContract ssc) {
    // Get all names
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("name"), "name");
    final List<String> names = (List<String>) sourceQuery.listForProjection(ssc, projectionList);

    // Determine new name
    int counter = 1;
    while (true) {
      final String candidateName = "New source #" + counter;
      if (!stringUtil.containsIgnoreCase(names, candidateName)) {
        return candidateName;
      } else {
        counter++;
      }
    }
  }

  public void updateSource(final StatelessSession statelessSession, final SourceDto sourceDto) {
    // Get
    final Source source = sourceDao.getEntity(statelessSession, sourceDto.getId());
    if (source == null) {
      return;
    }

    // Set simple fields
    source.setName(sourceDto.getName());
    source.setLanguage(sourceDto.getLanguage());
    source.setUrl(sourceDto.getUrl());
    source.setVisible(sourceDto.getVisible());
    source.setStatisticsChecking(sourceDto.getStatisticsChecking());

    // Set new types
    setSourceTypes(statelessSession, source.getId(), sourceDto.getTypes());

    // Set new rules
    final boolean wereRedirectingRulesUpdated =
        updateRedirectingRules(statelessSession, sourceDto.getRedirectingRules(), source.getId());
    final boolean wereDeletingRulesUpdated =
        updateDeletingRules(statelessSession, sourceDto.getDeletingRules(), source.getId());
    final boolean wereTagSelectingRulesUpdated =
        updateTagSelectingRules(statelessSession, sourceDto.getTagSelectingRules(), source.getId());

    // Update
    sourceDao.update(statelessSession, source);

    // If the rules have changed, trigger deprocessing of respective documents
    if (wereRedirectingRulesUpdated) {
      // Only trigger if no task targeting the same source is being active
      final boolean taskAlreadyActive = isTaskActive(DocumentsDeprocessingTask.NAME, source.getId(),
          DocumentProcessingState.DOWNLOADED);
      if (!taskAlreadyActive) {
        final DocumentsDeprocessingTask task =
            (DocumentsDeprocessingTask) ctx.getBean(DocumentsDeprocessingTask.BEAN_NAME,
                source.getId(), DocumentProcessingState.DOWNLOADED);
        taskManager.addTask(task);
      }
    } else if (wereDeletingRulesUpdated || wereTagSelectingRulesUpdated) {
      // Only trigger if no task targeting the same source is being active
      final boolean taskAlreadyActive = isTaskActive(DocumentsDeprocessingTask.NAME, source.getId(),
          DocumentProcessingState.REDIRECTED);
      if (!taskAlreadyActive) {
        final DocumentsDeprocessingTask task =
            (DocumentsDeprocessingTask) ctx.getBean(DocumentsDeprocessingTask.BEAN_NAME,
                source.getId(), DocumentProcessingState.REDIRECTED);
        taskManager.addTask(task);
      }
    }
  }

  private boolean updateRedirectingRules(final StatelessSession statelessSession,
      final List<RedirectingRuleDto> redirectingRuleDtos, final long sourceId) {
    boolean updated = false;

    final Collection<RedirectingRule> redirectingRules =
        redirectingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<RedirectingRule> redirectingRulesById =
        new EntitiesWithIdById<>(redirectingRules);
    int position = 0;
    for (final RedirectingRuleDto redirectingRuleDto : redirectingRuleDtos) {
      RedirectingRule redirectingRule = redirectingRulesById.poll(redirectingRuleDto.getId());

      // Create rule (DTO without ID or with unknown ID)
      if (redirectingRule == null) {
        redirectingRule = new RedirectingRule();
        redirectingRuleDao.insert(statelessSession, redirectingRule);
        updated = true;
      }

      // Update rule
      final UpdateTracker<RedirectingRule> updateTracker =
          new UpdateTracker<>(redirectingRule).beginUpdate();
      redirectingRule.setRegex(redirectingRuleDto.getRegex());
      redirectingRule.setRegexGroup(redirectingRuleDto.getRegexGroup());
      redirectingRule.setActiveFrom(redirectingRuleDto.getActiveFrom());
      redirectingRule.setActiveTo(redirectingRuleDto.getActiveTo());
      redirectingRule.setPosition(position);
      redirectingRule.setSourceId(sourceId);
      updateTracker.endUpdate();
      if (updateTracker.wasObjectUpdated()) {
        redirectingRuleDao.update(statelessSession, redirectingRule);
        updated = true;
      }

      position++;
    }

    // Delete rules
    for (final RedirectingRule redirectingRule : redirectingRulesById.getObjects()) {
      redirectingRuleDao.delete(statelessSession, redirectingRule);
      updated = true;
    }

    return updated;
  }

  private boolean updateDeletingRules(final StatelessSession statelessSession,
      final List<DeletingRuleDto> deletingRuleDtos, final long sourceId) {
    boolean updated = false;

    final Collection<DeletingRule> deletingRules =
        deletingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<DeletingRule> deletingRulesById = new EntitiesWithIdById<>(deletingRules);
    int position = 0;
    for (final DeletingRuleDto deletingRuleDto : deletingRuleDtos) {
      DeletingRule deletingRule = deletingRulesById.poll(deletingRuleDto.getId());

      // Create rule (DTO without ID or with unknown ID)
      if (deletingRule == null) {
        deletingRule = new DeletingRule();
        deletingRuleDao.insert(statelessSession, deletingRule);
        updated = true;
      }

      // Update rule
      final UpdateTracker<DeletingRule> updateTracker =
          new UpdateTracker<>(deletingRule).beginUpdate();
      deletingRule.setRegex(deletingRuleDto.getRegex());
      deletingRule.setActiveFrom(deletingRuleDto.getActiveFrom());
      deletingRule.setActiveTo(deletingRuleDto.getActiveTo());
      deletingRule.setPosition(position);
      deletingRule.setSourceId(sourceId);
      updateTracker.endUpdate();
      if (updateTracker.wasObjectUpdated()) {
        deletingRuleDao.update(statelessSession, deletingRule);
        updated = true;
      }

      position++;
    }

    // Delete rules
    for (final DeletingRule entity : deletingRulesById.getObjects()) {
      deletingRuleDao.delete(statelessSession, entity);
      updated = true;
    }

    return updated;
  }

  private boolean updateTagSelectingRules(final StatelessSession statelessSession,
      final List<TagSelectingRuleDto> tagSelectingRuleDtos, final long sourceId) {
    boolean updated = false;

    final Collection<TagSelectingRule> tagSelectingRules =
        tagSelectingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<TagSelectingRule> tagSelectingRulesById =
        new EntitiesWithIdById<>(tagSelectingRules);
    int position = 0;
    for (final TagSelectingRuleDto tagSelectingRuleDto : tagSelectingRuleDtos) {
      TagSelectingRule tagSelectingRule = tagSelectingRulesById.poll(tagSelectingRuleDto.getId());

      // Create rule (DTO without ID or with unknown ID)
      if (tagSelectingRule == null) {
        tagSelectingRule = new TagSelectingRule();
        tagSelectingRuleDao.insert(statelessSession, tagSelectingRule);
        updated = true;
      }

      // Update rule
      final UpdateTracker<TagSelectingRule> updateTracker =
          new UpdateTracker<>(tagSelectingRule).beginUpdate();
      tagSelectingRule.setTagSelector(tagSelectingRuleDto.getTagSelector());
      tagSelectingRule.setActiveFrom(tagSelectingRuleDto.getActiveFrom());
      tagSelectingRule.setActiveTo(tagSelectingRuleDto.getActiveTo());
      tagSelectingRule.setPosition(position);
      tagSelectingRule.setSourceId(sourceId);
      updateTracker.endUpdate();
      if (updateTracker.wasObjectUpdated()) {
        tagSelectingRuleDao.update(statelessSession, tagSelectingRule);
        updated = true;
      }

      position++;
    }

    // Delete rules
    for (final TagSelectingRule entity : tagSelectingRulesById.getObjects()) {
      tagSelectingRuleDao.delete(statelessSession, entity);
      updated = true;
    }

    return updated;
  }

  private boolean isTaskActive(final String name, final long sourceId,
      final DocumentProcessingState targetState) {
    final Collection<DocumentsDeprocessingTask> activeTasks =
        taskManager.getActiveTasks(name, DocumentsDeprocessingTask.class);
    for (final DocumentsDeprocessingTask activeTask : activeTasks) {
      if (activeTask.getSourceId() == sourceId && activeTask.getTargetState() == targetState) {
        return true;
      }
    }
    return false;
  }

  public void setVisible(final StatelessSession statelessSession, final long id,
      final Boolean visible) {
    if (visible == null) {
      return;
    }
    // Get
    final Source source = sourceDao.getEntity(statelessSession, id);
    if (source == null) {
      return;
    }
    // Update
    source.setVisible(visible);
    sourceDao.update(statelessSession, source);
  }

  public void setStatisticsChecking(final StatelessSession statelessSession, final long id,
      final Boolean statisticsChecking) {
    if (statisticsChecking == null) {
      return;
    }
    // Get
    final Source source = sourceDao.getEntity(statelessSession, id);
    if (source == null) {
      return;
    }
    // Update
    source.setStatisticsChecking(statisticsChecking);
    sourceDao.update(statelessSession, source);
  }

  public void setSourceTypes(final StatelessSession statelessSession, final long sourceId,
      final Collection<SourceTypeDto> sourceTypeDtos) {
    if (sourceTypeDtos == null) {
      return;
    }

    setTypes(statelessSession, sourceId, sourceTypeDtos);
  }

  private void setTypes(final StatelessSession statelessSession, final long sourceId,
      final Collection<SourceTypeDto> sourceTypeDtos) {
    final List<Source2SourceType> assignments =
        source2SourceTypeDao.getForSourceId(statelessSession, sourceId);
    final EntitiesById<Source2SourceType> assignmentsBySourceTypeId =
        new EntitiesById<>(assignments, e -> e.getSourceTypeId());

    for (final SourceTypeDto sourceTypeDto : sourceTypeDtos) {
      final Long sourceTypeId = sourceTypeDto.getId();
      Source2SourceType assignment = assignmentsBySourceTypeId.poll(sourceTypeId);

      // Create assignment
      if (assignment == null) {
        assignment = new Source2SourceType();
        assignment.setSourceId(sourceId);
        assignment.setSourceTypeId(sourceTypeId);
        source2SourceTypeDao.insert(statelessSession, assignment);
      }
    }

    // Delete assignments
    for (final Source2SourceType assignment : assignmentsBySourceTypeId.getObjects()) {
      source2SourceTypeDao.delete(statelessSession, assignment);
    }
  }

}
