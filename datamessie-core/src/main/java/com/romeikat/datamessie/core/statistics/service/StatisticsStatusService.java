package com.romeikat.datamessie.core.statistics.service;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2018 Dr. Raphael Romeikat
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.StatisticsDao;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.Project2SourceQuery;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsSparseTable;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;
import com.romeikat.datamessie.core.statistics.task.StatisticsStatus;
import edu.stanford.nlp.util.StringUtils;
import jersey.repackaged.com.google.common.collect.Sets;

@Service
public class StatisticsStatusService {

  private static final Logger LOG = LoggerFactory.getLogger(StatisticsStatusService.class);

  private static final double THRESHOLD_DAY_BEFORE = 0.1;
  private static final double THRESHOLD_WEEK_BEFORE = 0.1;

  @Autowired
  private StatisticsDao statisticsDao;

  @Autowired
  private SourceDao sourceDao;

  @Autowired
  private StringUtil stringUtil;

  @Autowired
  private SessionFactory sessionFactory;

  private StatisticsStatusService() {}

  public String createStatisticsStatusReport(final Long projectId) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Determine sources
    final Collection<Source> sourcesToCheck =
        getSourcesToCheck(sessionProvider.getStatelessSession(), projectId);
    if (sourcesToCheck.isEmpty()) {
      return null;
    }

    // Check status
    final Collection<StatisticsStatus> allStatisticsStatus =
        determineStatisticsStatus(sessionProvider.getStatelessSession(), sourcesToCheck);
    sessionProvider.closeStatelessSession();

    // Determine critical status
    final Collection<StatisticsStatus> criticalStatisticsStatus =
        determineCriticalStatisticsStatus(allStatisticsStatus);

    // Create report
    final String report = createReport(allStatisticsStatus, criticalStatisticsStatus);
    return report;
  }

  private Collection<Source> getSourcesToCheck(final SharedSessionContract ssc,
      final Long projectId) {
    // Query: Project2Source
    Collection<Long> sourceIds = null;
    if (projectId != null) {
      final Project2SourceQuery project2SourceQuery = new Project2SourceQuery();
      project2SourceQuery.addRestriction(Restrictions.eq("projectId", projectId));
      sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
      if (sourceIds.isEmpty()) {
        return Collections.emptyList();
      }
    }

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<Source>(SourceImpl.class);
    sourceQuery.addRestriction(Restrictions.eq("statisticsChecking", true));
    if (sourceIds != null) {
      sourceQuery.addRestriction(Restrictions.in("id", sourceIds));
    }

    // Done
    final List<Source> result = sourceQuery.listObjects(ssc);
    return result;
  }

  private Set<StatisticsStatus> determineStatisticsStatus(final SharedSessionContract ssc,
      final Collection<Source> sources) {
    final Set<Long> sourceIds = sources.stream().map(s -> s.getId()).collect(Collectors.toSet());

    final LocalDate today = LocalDate.now();
    final StatisticsSparseTable statisticsToday =
        statisticsDao.getStatistics(ssc, sourceIds, today);

    final LocalDate minus1 = today.minusDays(1);
    final StatisticsSparseTable statisticsMinus1 =
        statisticsDao.getStatistics(ssc, sourceIds, minus1);

    final LocalDate minus7 = today.minusDays(7);
    final StatisticsSparseTable statisticsMinus7 =
        statisticsDao.getStatistics(ssc, sourceIds, minus7);

    final Collection<DocumentProcessingState> successStates =
        DocumentProcessingState.getSuccessStates();

    final Set<StatisticsStatus> result = Sets.newHashSetWithExpectedSize(sourceIds.size());
    for (final Source source : sources) {
      final StatisticsStatus statisticsStatus = new StatisticsStatus(source.getName());

      final DocumentsPerState documentsPerStateToday =
          statisticsToday.getValue(source.getId(), today);
      final long successfulToday = documentsPerStateToday == null ? 0l
          : documentsPerStateToday.get(successStates.toArray(new DocumentProcessingState[] {}));

      final DocumentsPerState documentsPerStateMinus1 =
          statisticsMinus1.getValue(source.getId(), minus1);
      final long successfulMinus1 = documentsPerStateMinus1 == null ? 0l
          : documentsPerStateMinus1.get(successStates.toArray(new DocumentProcessingState[] {}));

      final DocumentsPerState documentsPerStateMinus7 =
          statisticsMinus7.getValue(source.getId(), minus7);
      final long successfulMinus7 = documentsPerStateMinus7 == null ? 0l
          : documentsPerStateMinus7.get(successStates.toArray(new DocumentProcessingState[] {}));

      statisticsStatus.setSuccessfulToday(successfulToday);
      statisticsStatus.setSuccessfulMinus1(successfulMinus1);
      statisticsStatus.setSuccessfulMinus7(successfulMinus7);

      result.add(statisticsStatus);
    }

    return result;
  }

  private Set<StatisticsStatus> determineCriticalStatisticsStatus(
      final Collection<StatisticsStatus> allStatisticsStatus) {
    final Set<StatisticsStatus> criticalStatisticsStatus = allStatisticsStatus.stream()
        .filter(statisticsStatus -> isCritical(statisticsStatus)).collect(Collectors.toSet());
    return criticalStatisticsStatus;
  }

  private boolean isCritical(final StatisticsStatus statisticsStatus) {
    final long successfulToday = statisticsStatus.getSuccessfulToday();
    final long successfulMinus1 = statisticsStatus.getSuccessfulMinus1();
    final long successfulMinus7 = statisticsStatus.getSuccessfulMinus7();

    // Compare to week before
    final boolean comparisonWithWeekBeforeApplies = successfulMinus7 > 0;
    if (comparisonWithWeekBeforeApplies) {
      final boolean successDropComparedToWeekBefore =
          successfulToday < (successfulMinus7 * THRESHOLD_WEEK_BEFORE);
      // Success drop compared to week before
      if (successDropComparedToWeekBefore) {
        return true;
      }
    }

    // Compare to day before
    final boolean comparisonWithDayBeforeApplies = successfulToday > 0 && successfulMinus1 > 0;
    if (comparisonWithDayBeforeApplies) {
      final boolean successDropComparedToDayBefore =
          successfulToday < (successfulMinus1 * THRESHOLD_DAY_BEFORE);
      // Success drop compared to week before
      if (successDropComparedToDayBefore) {
        return true;
      }
    }

    return false;
  }

  private String createReport(final Collection<StatisticsStatus> allStatisticsStatus,
      final Collection<StatisticsStatus> criticalStatisticsStatus) {
    final int numberOfAllSources = allStatisticsStatus.size();
    final int numberOfCriticalSources = criticalStatisticsStatus.size();

    final StringBuilder msg = new StringBuilder();

    // No criticals
    if (numberOfCriticalSources == 0) {
      msg.append("everything is alright");
      LOG.info(msg.toString());
    }
    // Criticals
    else {
      final List<String> criticalSourceNames = Lists.newArrayList(criticalStatisticsStatus.stream()
          .map(s -> s.getSourceName()).collect(Collectors.toList()));
      Collections.sort(criticalSourceNames);

      msg.append(numberOfCriticalSources);
      msg.append(" out of ");
      msg.append(numberOfAllSources);
      msg.append(" ");
      msg.append(stringUtil.getSingularOrPluralTerm("is", "are", numberOfCriticalSources));
      msg.append(" critical: ");
      msg.append(StringUtils.join(criticalSourceNames, ", "));
      LOG.warn(msg.toString());
    }

    // Done
    return msg.toString();
  }

}
