package com.romeikat.datamessie.core.rss.task.rssCrawling;

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
import java.util.List;

import javax.annotation.PostConstruct;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransactionAndResult;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;
import com.romeikat.datamessie.core.rss.service.CrawlingService;

@Service
public class ProjectCrawler {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectCrawler.class);

  @Autowired
  private ApplicationContext ctx;

  @Value("${crawling.sources.parallelism.factor}")
  private Double sourcesParallelismFactor;

  @Autowired
  private CrawlingService crawlingService;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private SessionFactory sessionFactory;

  private SourceCrawler sourceCrawler;

  private ProjectCrawler() {}

  @PostConstruct
  private void init() {
    sourceCrawler = new SourceCrawler(ctx);
  }

  public void performCrawling(final TaskExecution taskExecution, final Project project) throws TaskCancelledException {
    // Initialize
    taskExecution.reportWork(String.format("Performing crawling for project %s", project.getId()));
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Create new crawling
    final Crawling crawling = createCrawling(project, sessionProvider.getStatelessSession());
    if (crawling == null) {
      sessionProvider.closeStatelessSession();
      return;
    }

    // Load sources
    final List<Source> sources = loadSources(project, sessionProvider.getStatelessSession());
    if (sources == null) {
      sessionProvider.closeStatelessSession();
      return;
    }

    // Crawl sources
    performCrawling(taskExecution, crawling, sources);

    // Complete
    completeCrawling(crawling, sessionProvider.getStatelessSession());

    // Done
    sessionProvider.closeStatelessSession();
    taskExecution.reportWork(String.format("Completed crawling for project %s", project.getId()));
  }

  private Crawling createCrawling(final Project project, final StatelessSession statelessSession) {
    final Crawling crawling = new ExecuteWithTransactionAndResult<Crawling>(statelessSession) {
      @Override
      protected Crawling executeWithResult(final StatelessSession statelessSession) {
        final LocalDateTime started = LocalDateTime.now();
        return crawlingService.createCrawling(statelessSession, started, project);
      }

      @Override
      protected void onException(final Exception e) {
        LOG.error("Could not create crawling for project " + project.getId(), e);
      };
    }.execute();
    return crawling;
  }

  private List<Source> loadSources(final Project project, final StatelessSession statelessSession) {
    final List<Source> sources = new ExecuteWithTransactionAndResult<List<Source>>(statelessSession) {
      @Override
      protected List<Source> executeWithResult(final StatelessSession statelessSession) {
        return sourceDao.getOfProject(statelessSession, project.getId());
      }

      @Override
      protected void onException(final Exception e) {
        LOG.error("Could not create sources of project " + project.getId(), e);
      };
    }.execute();
    return sources;
  }

  private void performCrawling(final TaskExecution taskExecution, final Crawling crawling, final List<Source> sources) {
    new ParallelProcessing<Source>(sessionFactory, sources, sourcesParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final Source source) {
        try {
          sourceCrawler.performCrawling(sessionProvider, taskExecution, crawling, source);
        } catch (final Exception e) {
          LOG.error("Could not perform crawling for source " + source.getId(), e);
        }
      }
    };
  }

  private void completeCrawling(final Crawling crawling, final StatelessSession statelessSession) {
    new ExecuteWithTransaction(statelessSession) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        final LocalDateTime completed = LocalDateTime.now();
        crawling.setCompleted(completed);
        crawlingDao.update(statelessSession, crawling);
      }

      @Override
      protected void onException(final Exception e) {
        LOG.error("Could not update crawling " + crawling.getId(), e);
      };
    }.execute();
  }

}
