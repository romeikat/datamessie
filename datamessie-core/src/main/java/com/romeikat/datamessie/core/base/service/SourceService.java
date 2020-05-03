package com.romeikat.datamessie.core.base.service;

import java.time.LocalDate;
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
import java.util.concurrent.CompletableFuture;
import org.apache.commons.collections4.ListUtils;
import org.hibernate.SessionFactory;
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
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
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
import com.romeikat.datamessie.core.base.util.DateRange;
import com.romeikat.datamessie.core.base.util.EntitiesById;
import com.romeikat.datamessie.core.base.util.EntitiesWithIdById;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.UpdateTracker;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransactionAndResult;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
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
import jersey.repackaged.com.google.common.collect.Lists;

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
  @Qualifier("documentDao")
  private DocumentDao documentDao;

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

  @Autowired
  private SessionFactory sessionFactory;

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
        final Source source = new Source(0, name, "", null, true, true, false);
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
    source.setCookie(sourceDto.getCookie());
    source.setCrawlingEnabled(sourceDto.getCrawlingEnabled());
    source.setVisible(sourceDto.getVisible());
    source.setStatisticsChecking(sourceDto.getStatisticsChecking());
    source.setNotes(sourceDto.getNotes());

    // Set new types
    setSourceTypes(statelessSession, source.getId(), sourceDto.getTypes());

    // Set new rules
    final List<DateRange> dateRangesForRedirectingRules =
        updateRedirectingRules(statelessSession, sourceDto.getRedirectingRules(), source.getId());
    final List<DateRange> dateRangesForDeletingRules =
        updateDeletingRules(statelessSession, sourceDto.getDeletingRules(), source.getId());
    final List<DateRange> dateRangesForTagSelectingRules =
        updateTagSelectingRules(statelessSession, sourceDto.getTagSelectingRules(), source.getId());
    final List<DateRange> dateRangesForDeletingAndTagSelectingRules =
        ListUtils.union(dateRangesForDeletingRules, dateRangesForTagSelectingRules);
    final List<DateRange> dateRangesForAll =
        ListUtils.union(dateRangesForRedirectingRules, dateRangesForDeletingAndTagSelectingRules);

    // Update
    sourceDao.update(statelessSession, source);

    // If the rules have changed, trigger deprocessing of respective documents
    CompletableFuture
        .runAsync(() -> triggerDocumentsDeprocessing(source, dateRangesForRedirectingRules,
            dateRangesForDeletingAndTagSelectingRules, dateRangesForAll));
  }

  private void triggerDocumentsDeprocessing(final Source source,
      final List<DateRange> dateRangesForRedirectingRules,
      final List<DateRange> dateRangesForDeletingAndTagSelectingRules,
      final List<DateRange> dateRangesForAll) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    final boolean redirectingRulesChanged = !dateRangesForRedirectingRules.isEmpty();
    final boolean deletingOrTagSelectingRulesRulesChanged =
        !dateRangesForDeletingAndTagSelectingRules.isEmpty();

    // Only redirecting rules changed
    if (redirectingRulesChanged && !deletingOrTagSelectingRulesRulesChanged) {
      final DocumentProcessingState targetState = DocumentProcessingState.DOWNLOADED;

      // Determine all existing download dates for the respective states and source
      final Collection<LocalDate> downloadDates = determineDownloadDates(
          sessionProvider.getStatelessSession(), targetState, source.getId());

      // Determine all affected download dates
      final Collection<LocalDate> affectedDownloadDates =
          DateRange.applyDateRangesTo(dateRangesForRedirectingRules, downloadDates);

      triggerNewDocumentsDeprocessingTaskIfNecessary(source.getId(), affectedDownloadDates,
          targetState);
    }

    // Only deleting / tag selecting rules changed
    else if (!redirectingRulesChanged && deletingOrTagSelectingRulesRulesChanged) {
      final DocumentProcessingState targetState = DocumentProcessingState.REDIRECTED;

      // Determine all existing download dates for the respective states and source
      final Collection<LocalDate> downloadDates = determineDownloadDates(
          sessionProvider.getStatelessSession(), targetState, source.getId());

      // Determine all affected download dates
      final Collection<LocalDate> affectedDownloadDates =
          DateRange.applyDateRangesTo(dateRangesForDeletingAndTagSelectingRules, downloadDates);

      triggerNewDocumentsDeprocessingTaskIfNecessary(source.getId(), affectedDownloadDates,
          targetState);
    }

    // All redirecting and deleting / tag selecting rules changed
    else if (redirectingRulesChanged && deletingOrTagSelectingRulesRulesChanged) {
      final DocumentProcessingState targetState = DocumentProcessingState.DOWNLOADED;

      // Determine all existing download dates for the respective states and source
      final Collection<LocalDate> downloadDates = determineDownloadDates(
          sessionProvider.getStatelessSession(), targetState, source.getId());

      // Determine all affected download dates
      final Collection<LocalDate> affectedDownloadDates =
          DateRange.applyDateRangesTo(dateRangesForAll, downloadDates);

      triggerNewDocumentsDeprocessingTaskIfNecessary(source.getId(), affectedDownloadDates,
          targetState);
    }

    sessionProvider.closeStatelessSession();
  }

  private Collection<LocalDate> determineDownloadDates(final StatelessSession statelessSession,
      final DocumentProcessingState targetState, final long sourceId) {
    final Collection<DocumentProcessingState> statesToBeDeprocessed =
        DocumentProcessingState.getStatesForDeprocessing(targetState);
    final Collection<LocalDate> downloadDates =
        documentDao.getDownloadedDates(statelessSession, statesToBeDeprocessed, sourceId);
    return downloadDates;
  }

  private void triggerNewDocumentsDeprocessingTaskIfNecessary(final long sourceId,
      final Collection<LocalDate> downloadDates, final DocumentProcessingState targetState) {
    final DocumentsDeprocessingTask activeTask = getActiveDeprocessingTask(sourceId, targetState);

    // No task active => add new task
    if (activeTask == null) {
      final DocumentsDeprocessingTask task = (DocumentsDeprocessingTask) ctx
          .getBean(DocumentsDeprocessingTask.BEAN_NAME, sourceId, targetState, downloadDates);
      taskManager.addTask(task);
    }
    // Task active and date range covered => no new task necessary
    else if (doesTaskCoverAffectedDownloadDates(activeTask, downloadDates)) {
    }
    // Task active, but date range not covered => cancel and add new task
    else {
      taskManager.cancelTask(activeTask);
      final DocumentsDeprocessingTask task = (DocumentsDeprocessingTask) ctx
          .getBean(DocumentsDeprocessingTask.BEAN_NAME, sourceId, targetState, downloadDates);
      taskManager.addTask(task);
    }
  }

  private boolean doesTaskCoverAffectedDownloadDates(final DocumentsDeprocessingTask task,
      final Collection<LocalDate> downloadDates) {
    if (task == null) {
      return false;
    }

    if (downloadDates.isEmpty()) {
      return true;
    }

    // Check from date
    final boolean allDownloadDatesCovered = task.getDownloadDates().containsAll(downloadDates);
    return allDownloadDatesCovered;
  }

  /**
   * Determines which date ranges are affected by updating the redirecting rules.
   *
   * @param statelessSession
   * @param redirectingRuleDtos
   * @param sourceId
   * @return
   */
  private List<DateRange> updateRedirectingRules(final StatelessSession statelessSession,
      final List<RedirectingRuleDto> redirectingRuleDtos, final long sourceId) {
    // Affected date ranges
    final List<DateRange> affectedDateRanges = Lists.newLinkedList();

    final Collection<RedirectingRule> redirectingRules =
        redirectingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<RedirectingRule> redirectingRulesById =
        new EntitiesWithIdById<>(redirectingRules);
    int position = 0;

    // Create / update
    for (final RedirectingRuleDto redirectingRuleDto : redirectingRuleDtos) {
      RedirectingRule redirectingRule = redirectingRulesById.poll(redirectingRuleDto.getId());

      final DateRange oldDateRange = redirectingRule == null ? null
          : DateRange.create(redirectingRule.getActiveFrom(), redirectingRule.getActiveTo());
      final DateRange newDateRange =
          DateRange.create(redirectingRuleDto.getActiveFrom(), redirectingRuleDto.getActiveTo());

      // Create new rule
      if (redirectingRule == null) {
        redirectingRule = new RedirectingRule();

        redirectingRule.setRegex(redirectingRuleDto.getRegex());
        redirectingRule.setRegexGroup(redirectingRuleDto.getRegexGroup());
        redirectingRule.setActiveFrom(redirectingRuleDto.getActiveFrom());
        redirectingRule.setActiveTo(redirectingRuleDto.getActiveTo());
        redirectingRule.setMode(redirectingRuleDto.getMode());
        redirectingRule.setPosition(position);
        redirectingRule.setSourceId(sourceId);
        redirectingRuleDao.insert(statelessSession, redirectingRule);

        // New date range only
        affectedDateRanges.add(newDateRange);
      }

      // Update existing rule
      else {
        final UpdateTracker updateTracker = new UpdateTracker(redirectingRule).beginUpdate();

        redirectingRule.setRegex(redirectingRuleDto.getRegex());
        redirectingRule.setRegexGroup(redirectingRuleDto.getRegexGroup());
        redirectingRule.setActiveFrom(redirectingRuleDto.getActiveFrom());
        redirectingRule.setActiveTo(redirectingRuleDto.getActiveTo());
        redirectingRule.setMode(redirectingRuleDto.getMode());
        redirectingRule.setPosition(position);
        redirectingRule.setSourceId(sourceId);

        updateTracker.endUpdate();
        if (updateTracker.wasSourceRuleUpdated()) {
          redirectingRuleDao.update(statelessSession, redirectingRule);

          final boolean dateRangeUpdated = updateTracker.wasSourceRuleDateRangeUpdated();
          final boolean logicUpdated = updateTracker.wasSourceRuleLogicUpdated();

          // Only dates updated => XOR with new date range
          if (dateRangeUpdated && !logicUpdated) {
            affectedDateRanges.addAll(DateRange.combineRangesWithXor(oldDateRange, newDateRange));
          }

          // Only logic updated => date range (old == new)
          else if (!dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
          }

          // Dates and logic updated => old and new date range
          else if (dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
            affectedDateRanges.add(newDateRange);
          }
        }
      }

      position++;
    }

    // Delete rules
    for (final RedirectingRule redirectingRule : redirectingRulesById.getObjects()) {
      redirectingRuleDao.delete(statelessSession, redirectingRule);

      final DateRange oldDateRange =
          DateRange.create(redirectingRule.getActiveFrom(), redirectingRule.getActiveTo());

      // Old date range only
      affectedDateRanges.add(oldDateRange);
    }

    return affectedDateRanges;
  }

  private List<DateRange> updateDeletingRules(final StatelessSession statelessSession,
      final List<DeletingRuleDto> deletingRuleDtos, final long sourceId) {
    // Affected date ranges
    final List<DateRange> affectedDateRanges = Lists.newLinkedList();

    final Collection<DeletingRule> deletingRules =
        deletingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<DeletingRule> deletingRulesById = new EntitiesWithIdById<>(deletingRules);
    int position = 0;

    // Create / update
    for (final DeletingRuleDto deletingRuleDto : deletingRuleDtos) {
      DeletingRule deletingRule = deletingRulesById.poll(deletingRuleDto.getId());

      final DateRange oldDateRange = deletingRule == null ? null
          : DateRange.create(deletingRule.getActiveFrom(), deletingRule.getActiveTo());
      final DateRange newDateRange =
          DateRange.create(deletingRuleDto.getActiveFrom(), deletingRuleDto.getActiveTo());

      // Create new rule
      if (deletingRule == null) {
        deletingRule = new DeletingRule();

        deletingRule.setSelector(deletingRuleDto.getSelector());
        deletingRule.setActiveFrom(deletingRuleDto.getActiveFrom());
        deletingRule.setActiveTo(deletingRuleDto.getActiveTo());
        deletingRule.setMode(deletingRuleDto.getMode());
        deletingRule.setPosition(position);
        deletingRule.setSourceId(sourceId);

        deletingRuleDao.insert(statelessSession, deletingRule);

        // New date range only
        affectedDateRanges.add(newDateRange);
      }

      // Update existing rule
      else {
        final UpdateTracker updateTracker = new UpdateTracker(deletingRule).beginUpdate();

        deletingRule.setSelector(deletingRuleDto.getSelector());
        deletingRule.setActiveFrom(deletingRuleDto.getActiveFrom());
        deletingRule.setActiveTo(deletingRuleDto.getActiveTo());
        deletingRule.setMode(deletingRuleDto.getMode());
        deletingRule.setPosition(position);
        deletingRule.setSourceId(sourceId);

        updateTracker.endUpdate();
        if (updateTracker.wasSourceRuleUpdated()) {
          deletingRuleDao.update(statelessSession, deletingRule);

          final boolean dateRangeUpdated = updateTracker.wasSourceRuleDateRangeUpdated();
          final boolean logicUpdated = updateTracker.wasSourceRuleLogicUpdated();

          // Only dates updated => XOR with new date range
          if (dateRangeUpdated && !logicUpdated) {
            affectedDateRanges.addAll(DateRange.combineRangesWithXor(oldDateRange, newDateRange));
          }

          // Only logic updated => date range (old == new)
          else if (!dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
          }

          // Dates and logic updated => old and new date range
          else if (dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
            affectedDateRanges.add(newDateRange);
          }
        }
      }

      position++;
    }

    // Delete rules
    for (final DeletingRule deletingRule : deletingRulesById.getObjects()) {
      deletingRuleDao.delete(statelessSession, deletingRule);

      final DateRange oldDateRange =
          DateRange.create(deletingRule.getActiveFrom(), deletingRule.getActiveTo());

      // Old date range only
      affectedDateRanges.add(oldDateRange);
    }

    return affectedDateRanges;
  }

  private List<DateRange> updateTagSelectingRules(final StatelessSession statelessSession,
      final List<TagSelectingRuleDto> tagSelectingRuleDtos, final long sourceId) {
    // Affected date ranges
    final List<DateRange> affectedDateRanges = Lists.newLinkedList();

    final Collection<TagSelectingRule> tagSelectingRules =
        tagSelectingRuleDao.getOfSource(statelessSession, sourceId);
    final EntitiesById<TagSelectingRule> tagSelectingRulesById =
        new EntitiesWithIdById<>(tagSelectingRules);
    int position = 0;

    // Create / update
    for (final TagSelectingRuleDto tagSelectingRuleDto : tagSelectingRuleDtos) {
      TagSelectingRule tagSelectingRule = tagSelectingRulesById.poll(tagSelectingRuleDto.getId());

      final DateRange oldDateRange = tagSelectingRule == null ? null
          : DateRange.create(tagSelectingRule.getActiveFrom(), tagSelectingRule.getActiveTo());
      final DateRange newDateRange =
          DateRange.create(tagSelectingRuleDto.getActiveFrom(), tagSelectingRuleDto.getActiveTo());

      // Create new rule
      if (tagSelectingRule == null) {
        tagSelectingRule = new TagSelectingRule();

        tagSelectingRule.setSelector(tagSelectingRuleDto.getSelector());
        tagSelectingRule.setActiveFrom(tagSelectingRuleDto.getActiveFrom());
        tagSelectingRule.setActiveTo(tagSelectingRuleDto.getActiveTo());
        tagSelectingRule.setMode(tagSelectingRuleDto.getMode());
        tagSelectingRule.setPosition(position);
        tagSelectingRule.setSourceId(sourceId);

        tagSelectingRuleDao.insert(statelessSession, tagSelectingRule);

        // New date range only
        affectedDateRanges.add(newDateRange);
      }

      // Update existing rule
      else {
        final UpdateTracker updateTracker = new UpdateTracker(tagSelectingRule).beginUpdate();

        tagSelectingRule.setSelector(tagSelectingRuleDto.getSelector());
        tagSelectingRule.setActiveFrom(tagSelectingRuleDto.getActiveFrom());
        tagSelectingRule.setActiveTo(tagSelectingRuleDto.getActiveTo());
        tagSelectingRule.setMode(tagSelectingRuleDto.getMode());
        tagSelectingRule.setPosition(position);
        tagSelectingRule.setSourceId(sourceId);

        updateTracker.endUpdate();
        if (updateTracker.wasSourceRuleUpdated()) {
          tagSelectingRuleDao.update(statelessSession, tagSelectingRule);

          final boolean dateRangeUpdated = updateTracker.wasSourceRuleDateRangeUpdated();
          final boolean logicUpdated = updateTracker.wasSourceRuleLogicUpdated();

          // Only dates updated => XOR with new date range
          if (dateRangeUpdated && !logicUpdated) {
            affectedDateRanges.addAll(DateRange.combineRangesWithXor(oldDateRange, newDateRange));
          }

          // Only logic updated => date range (old == new)
          else if (!dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
          }

          // Dates and logic updated => old and new date range
          else if (dateRangeUpdated && logicUpdated) {
            affectedDateRanges.add(oldDateRange);
            affectedDateRanges.add(newDateRange);
          }
        }
      }

      position++;
    }

    // Delete rules
    for (final TagSelectingRule tagSelectingRule : tagSelectingRulesById.getObjects()) {
      tagSelectingRuleDao.delete(statelessSession, tagSelectingRule);

      final DateRange oldDateRange =
          DateRange.create(tagSelectingRule.getActiveFrom(), tagSelectingRule.getActiveTo());

      // Old date range only
      affectedDateRanges.add(oldDateRange);
    }

    return affectedDateRanges;
  }

  private DocumentsDeprocessingTask getActiveDeprocessingTask(final long sourceId,
      final DocumentProcessingState targetState) {
    final Collection<DocumentsDeprocessingTask> activeTasks =
        taskManager.getActiveTasks(DocumentsDeprocessingTask.class);
    for (final DocumentsDeprocessingTask activeTask : activeTasks) {
      if (activeTask.getSourceId() == sourceId && activeTask.getTargetState() == targetState) {
        return activeTask;
      }
    }
    return null;
  }

  public void setCrawlingEnabled(final StatelessSession statelessSession, final long id,
      final Boolean crawlingEnabled) {
    if (crawlingEnabled == null) {
      return;
    }
    // Get
    final Source source = sourceDao.getEntity(statelessSession, id);
    if (source == null) {
      return;
    }
    // Update
    source.setCrawlingEnabled(crawlingEnabled);
    sourceDao.update(statelessSession, source);
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
