package com.romeikat.datamessie.core.statistics.task;

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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.comparator.DescendingComparator;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.statistics.dao.DocumentDao;
import com.romeikat.datamessie.core.statistics.service.StatisticsService;

@Service
public class StatisticsRebuilder {

  private static final Logger LOG = LoggerFactory.getLogger(StatisticsRebuilder.class);

  @Value("${statistics.rebuilding.rebuildAllAtStartup}")
  private boolean rebuildAllAtStartup;

  @Value("${statistics.rebuilding.pause}")
  private long pause;

  private final StatisticsToBeRebuilt statisticsToBeRebuilt;

  private TaskExecutionWork work;

  @Autowired
  private StatisticsService statisticsService;

  @Autowired
  @Qualifier("statisticsDocumentDao")
  private DocumentDao documentDao;

  @Autowired
  private SessionFactory sessionFactory;

  public StatisticsRebuilder() {
    statisticsToBeRebuilt = new StatisticsToBeRebuilt();
  }

  public void toBeRebuilt(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    this.statisticsToBeRebuilt.toBeRebuilt(statisticsToBeRebuilt);
  }

  public void toBeRebuilt(final Long sourceId, final LocalDate published) {
    statisticsToBeRebuilt.toBeRebuilt(sourceId, published);
  }

  public void performRebuilding(final TaskExecution taskExecution) throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Initialize
    taskExecution.reportWork("Starting statistics rebuilding");

    if (rebuildAllAtStartup) {
      // Determine all published dates
      final List<LocalDate> publishedDates = getPublishedDates(taskExecution, sessionProvider.getStatelessSession());

      // Process from maximum to minimum published date
      for (final LocalDate publishedDate : publishedDates) {
        toBeRebuilt(null, publishedDate);
      }
      sessionProvider.closeStatelessSession();
    }

    // Repeatedly rebuild statistics
    while (true) {
      final SourceAndPublished sourceAndPublished = statisticsToBeRebuilt.poll();

      // If no statistics to be rebuilt at the moment, wait and go on
      if (sourceAndPublished == null) {
        taskExecution.checkpoint(pause);
        continue;
      }

      // Rebuild statistics
      final Long sourceId = sourceAndPublished.getSourceId();
      final LocalDate publishedDate = sourceAndPublished.getPublished();
      final String msg = "Rebuilding statistics for " + printSourceAndPublished(sourceId, publishedDate);
      work = taskExecution.reportWorkStart(msg);
      new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
        @Override
        protected void execute(final StatelessSession statelessSession) {
          statisticsService.rebuildStatistics(statelessSession, sourceId, publishedDate);
        }

        @Override
        protected void onException(final Exception e) {
          final String msg = "Could not rebuild statistics for " + printSourceAndPublished(sourceId, publishedDate);
          LOG.error(msg, e);
        };
      }.execute();
      sessionProvider.closeStatelessSession();
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private List<LocalDate> getPublishedDates(final TaskExecution taskExecution,
      final StatelessSession statelessSession) {
    final TaskExecutionWork work = taskExecution.reportWorkStart("Loading all published dates");

    final Collection<LocalDate> publishedDates = documentDao.getPublishedDates(statelessSession);
    taskExecution.reportWork(String.format("Loaded %s published date(s)", publishedDates.size()));
    final List<LocalDate> uniquePublishedDatesSorted = Lists.newArrayList(publishedDates);
    Collections.sort(uniquePublishedDatesSorted, new DescendingComparator<LocalDate>());

    taskExecution.reportWorkEnd(work);
    return uniquePublishedDatesSorted;
  }

  private String printSourceAndPublished(final Long sourceId, final LocalDate publishedDate) {
    final StringBuilder result = new StringBuilder();
    if (sourceId != null) {
      result.append("source ");
      result.append(sourceId);
    }
    if (sourceId != null && publishedDate != null) {
      result.append(" and ");
    }
    if (publishedDate != null) {
      result.append("published date ");
      result.append(LocalDateConverter.INSTANCE_UI.convertToString(publishedDate));
    }
    return result.toString();
  }



}
