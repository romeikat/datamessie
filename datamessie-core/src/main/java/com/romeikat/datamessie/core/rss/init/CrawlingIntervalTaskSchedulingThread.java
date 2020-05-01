package com.romeikat.datamessie.core.rss.init;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.task.scheduling.IntervalTaskSchedulingThread;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;
import com.romeikat.datamessie.core.rss.task.rssCrawling.RssCrawlingTask;

public class CrawlingIntervalTaskSchedulingThread extends IntervalTaskSchedulingThread {

  private final ApplicationContext ctx;
  private final SessionFactory sessionFactory;
  private final TaskManager taskManager;
  private final ProjectDao projectDao;
  private final CrawlingDao crawlingDao;

  private final HibernateSessionProvider sessionProvider;

  private final long projectId;

  public CrawlingIntervalTaskSchedulingThread(final long projectId, final ApplicationContext ctx) {
    this.ctx = ctx;
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    taskManager = ctx.getBean(TaskManager.class);
    projectDao = ctx.getBean("projectDao", ProjectDao.class);
    crawlingDao = ctx.getBean("rssCrawlingDao", CrawlingDao.class);

    sessionProvider = new HibernateSessionProvider(sessionFactory);

    this.projectId = projectId;
  }


  private Project getProject() {
    final Project project = projectDao.getEntity(sessionProvider.getStatelessSession(), projectId);
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
    final LocalDateTime latestCrawlingStart = crawlingDao
        .getStartOfLatestCompletedCrawling(sessionProvider.getStatelessSession(), project.getId());
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

}
