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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;

public class ProjectCrawlerTest extends AbstractDbSetupBasedTest {

  private SourceCrawler sourceCrawler;

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private ProjectCrawler projectCrawler;

  @Autowired
  private ProjectDao projectDao;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Mock
  private TaskExecution taskExecution;

  @Override
  protected Operation initDb() {
    final Project project1 = new ProjectImpl(1, "Project1", false, false);
    final Source source1 = new Source(1, "Source1", "http://www.source1.de/", true, false);
    final Project2Source project2Source = new Project2Source(1, 1);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        sequenceOf(CommonOperations.insertIntoProject(project1),
            CommonOperations.insertIntoSource(source1),
            CommonOperations.insertIntoProject2Source(project2Source)));
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    sourceCrawler = new SourceCrawler(ctx);

    sourceCrawler = createAndInjectSpy(sourceCrawler, projectCrawler, "sourceCrawler");
  }

  @Test
  public void performCrawling_newCrawling() throws TaskCancelledException {
    final Project project1 = projectDao.getEntity(sessionProvider.getStatelessSession(), 1);

    // Crawl
    doNothing().when(sourceCrawler).performCrawling(Matchers.any(), Matchers.any(),
        Matchers.anyLong(), Matchers.any());
    projectCrawler.performCrawling(taskExecution, project1.getId());

    // New crawling is created
    final List<Crawling> crawlings =
        crawlingDao.getForProject(sessionProvider.getStatelessSession(), project1.getId());
    assertEquals(1, crawlings.size());

    // Completed is set
    final Crawling crawling = crawlings.iterator().next();
    assertNotNull(crawling.getCompleted());
  }

  @Test
  public void performCrawling_exceptionWhileCrawlingSource() throws TaskCancelledException {
    final Project project1 = projectDao.getEntity(sessionProvider.getStatelessSession(), 1);

    // Crawl
    doThrow(Exception.class).when(sourceCrawler).performCrawling(Matchers.any(), Matchers.any(),
        Matchers.anyLong(), Matchers.any());
    projectCrawler.performCrawling(taskExecution, project1.getId());

    // New crawling is created
    final List<Crawling> crawlings =
        crawlingDao.getForProject(sessionProvider.getStatelessSession(), project1.getId());
    assertEquals(1, crawlings.size());

    // Completed is set
    final Crawling crawling = crawlings.iterator().next();
    assertNotNull(crawling.getCompleted());
  }

}
