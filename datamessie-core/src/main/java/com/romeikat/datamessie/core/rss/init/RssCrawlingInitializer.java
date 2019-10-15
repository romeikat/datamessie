package com.romeikat.datamessie.core.rss.init;

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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.task.scheduling.IntervalTaskSchedulingThread;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;
import com.romeikat.datamessie.core.rss.task.rssCrawling.RssCrawlingTask;

@Service
public class RssCrawlingInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(RssCrawlingInitializer.class);

  @Value("${rss.module.enabled}")
  private boolean moduleEnabled;

  @Value("${crawling.enabled}")
  private boolean crawlingEnabled;

  private final Set<Long> crawlingProjectIds;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  @Qualifier("projectDao")
  private ProjectDao projectDao;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ApplicationContext ctx;

  private RssCrawlingInitializer() {
    crawlingProjectIds = new HashSet<Long>();
  }

  @PostConstruct
  private void initialize() {
    if (moduleEnabled) {
      // Schedule crawlings
      if (crawlingEnabled) {
        LOG.info("Initializing RSS crawling");
        scheduleCrawling();
      } else {
        LOG.info("RSS crawling is disabled");
      }
    }
  }

  private void scheduleCrawling() {
    // Get all projects
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final List<Project> projects = projectDao.getAllEntites(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    // Schedule crawling for each project
    for (final Project project : projects) {
      scheduleCrawling(project.getId());
    }
  }

  private void scheduleCrawling(final long projectId) {
    synchronized (crawlingProjectIds) {
      // Only add project if not yet added
      if (crawlingProjectIds.contains(projectId)) {
        LOG.debug("Not adding project {} as a crawling has already scheduled for that project",
            projectId);
        return;
      }
      // Add crawling
      LOG.debug("Adding project {}", projectId);
      crawlingProjectIds.add(projectId);
      // Schedule crawlings
      new IntervalTaskSchedulingThread() {

        private final HibernateSessionProvider sessionProvider =
            new HibernateSessionProvider(sessionFactory);

        private Project getProject() {
          final Project project =
              projectDao.getEntity(sessionProvider.getStatelessSession(), projectId);
          sessionProvider.closeStatelessSession();
          return project;
        }

        @Override
        protected Task getTask() {
          final Project project = getProject();
          if (project == null) {
            return null;
          }
          final RssCrawlingTask task =
              (RssCrawlingTask) ctx.getBean(RssCrawlingTask.BEAN_NAME, project.getId());
          return task;
        }

        @Override
        protected String getTaskName() {
          return "RSS crawling for project " + projectId;
        }

        @Override
        protected Integer getTaskExecutionInterval() {
          // Project
          final Project project = getProject();
          if (project == null) {
            return null;
          }
          // Crawling interval
          final Integer crawlingIntervalInMins = project.getCrawlingInterval();
          if (crawlingIntervalInMins == null) {
            return null;
          }
          final int crawlingIntervalInMillis = crawlingIntervalInMins * 60 * 1000;
          return crawlingIntervalInMillis;
        }

        @Override
        protected LocalDateTime getActualStartOfLatestCompletedTask() {
          final Project project = getProject();
          if (project == null) {
            return null;
          }
          final LocalDateTime latestCrawlingStart = crawlingDao.getStartOfLatestCompletedCrawling(
              sessionProvider.getStatelessSession(), project.getId());
          sessionProvider.closeStatelessSession();
          return latestCrawlingStart;
        }

        @Override
        protected boolean shouldSkipTaskExecution() {
          final Project project = getProject();
          if (project == null) {
            return true;
          }

          final Boolean isCrawlingEnabled = project.getCrawlingEnabled();
          return BooleanUtils.isNotTrue(isCrawlingEnabled);
        }

        @Override
        protected TaskManager getTaskManager() {
          return taskManager;
        }

      }.start();
    }
  }

}
