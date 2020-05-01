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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.Project;

@Service
public class RssCrawlingInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(RssCrawlingInitializer.class);

  @Value("${rss.module.enabled}")
  private boolean moduleEnabled;

  @Value("${crawling.enabled}")
  private boolean crawlingEnabled;

  @Autowired
  @Qualifier("projectDao")
  private ProjectDao projectDao;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ApplicationContext ctx;

  private final Set<Long> projectIds;

  private RssCrawlingInitializer() {
    projectIds = new HashSet<Long>();
  }

  @PostConstruct
  private void initialize() {
    if (moduleEnabled) {
      // Schedule crawlings
      if (crawlingEnabled) {
        LOG.info("Initializing RSS crawling");
        scheduleCrawlings();
      } else {
        LOG.info("RSS crawling is disabled");
      }
    }
  }

  private void scheduleCrawlings() {
    // Get all projects
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final List<Project> projects = projectDao.getAllEntites(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    // Schedule crawling for each project
    for (final Project project : projects) {
      scheduleCrawling(project.getId());
    }
  }

  public void scheduleCrawling(final long projectId) {
    synchronized (projectIds) {
      // Only add project if not yet added
      if (projectIds.contains(projectId)) {
        LOG.debug("Not adding project {} as a crawling has already scheduled for that project",
            projectId);
        return;
      }
      // Add crawling
      LOG.debug("Adding project {}", projectId);
      projectIds.add(projectId);
      // Schedule crawlings
      new CrawlingIntervalTaskSchedulingThread(projectId, ctx).start();
    }
  }

}
