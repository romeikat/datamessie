package com.romeikat.datamessie.core.statistics.task;

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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.statistics.service.StatisticsStatusService;
import jersey.repackaged.com.google.common.collect.Lists;

public class StatisticsChecker {

  private final SessionFactory sessionFactory;
  private final StatisticsStatusService statisticsStatusService;

  private HibernateSessionProvider sessionProvider;
  private TaskExecution taskExecution;
  private TaskExecutionWork work;


  public StatisticsChecker(final ApplicationContext ctx) {
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    statisticsStatusService = ctx.getBean("statisticsStatusService", StatisticsStatusService.class);
  }

  public void performChecking(final TaskExecution taskExecution) throws TaskCancelledException {
    sessionProvider = new HibernateSessionProvider(sessionFactory);
    this.taskExecution = taskExecution;
    taskExecution.reportWork("Starting statistics checking");

    // Determine sources
    final Collection<Long> sourceIdsToCheck = determineSourceIdsToCheck();
    if (sourceIdsToCheck.isEmpty()) {
      taskExecution.reportWorkEnd(work);
      return;
    }

    // Check status
    final Collection<StatisticsStatus> allStatisticsStatus =
        determineStatisticsStatus(sourceIdsToCheck);
    sessionProvider.closeStatelessSession();

    // Determine critical status
    final Set<StatisticsStatus> criticalStatisticsStatus =
        determineCriticalStatisticsStatus(allStatisticsStatus);

    // Report status
    reportStatisticsStatus(allStatisticsStatus, criticalStatisticsStatus);

    taskExecution.reportWorkEnd(work);
  }

  private Collection<Long> determineSourceIdsToCheck() throws TaskCancelledException {
    final String msg = "Determining which sources to check";
    work = taskExecution.reportWorkStart(msg);

    final Collection<Long> sourceIdsToCheck =
        statisticsStatusService.getSourceIdsToCheck(sessionProvider.getStatelessSession());

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
    return sourceIdsToCheck;
  }

  private Collection<StatisticsStatus> determineStatisticsStatus(
      final Collection<Long> sourceIdsToCheck) throws TaskCancelledException {
    final List<Long> sourceIdsAsList = Lists.newArrayList(sourceIdsToCheck);
    Collections.sort(sourceIdsAsList);
    final String sourceIdsAsString = StringUtils.join(sourceIdsAsList, ", ");

    final String msg = String.format("Checking statistics for sources %s", sourceIdsAsString);
    work = taskExecution.reportWorkStart(msg);

    final Collection<StatisticsStatus> allStatisticsStatus = statisticsStatusService
        .determineStatisticsStatus(sessionProvider.getStatelessSession(), sourceIdsToCheck);

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
    return allStatisticsStatus;
  }

  private Set<StatisticsStatus> determineCriticalStatisticsStatus(
      final Collection<StatisticsStatus> allStatisticsStatus) throws TaskCancelledException {
    final String msg = "Determining critical sources";
    work = taskExecution.reportWorkStart(msg);

    final Set<StatisticsStatus> criticalStatisticsStatus = allStatisticsStatus.stream()
        .filter(statisticsStatus -> statisticsStatusService.isCritical(statisticsStatus))
        .collect(Collectors.toSet());

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
    return criticalStatisticsStatus;
  }

  private void reportStatisticsStatus(final Collection<StatisticsStatus> allStatisticsStatus,
      final Collection<StatisticsStatus> criticalStatisticsStatus) throws TaskCancelledException {
    final String msg = "Reporting critical sources";
    work = taskExecution.reportWorkStart(msg);

    statisticsStatusService.reportStatisticsStatus(allStatisticsStatus, criticalStatisticsStatus);

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
  }

}
