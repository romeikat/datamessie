package com.romeikat.datamessie.core.processing.service.fulltext.index;

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

import java.util.List;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.Waiter;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import edu.stanford.nlp.util.StringUtils;

public class IndexBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IndexBuilder.class);

  private final HibernateSessionProvider sessionProvider;

  private final Class<?>[] classesToIndex;

  private final int batchSize;

  private final int numberOfThreads;

  private boolean isBuildingComplete;

  public IndexBuilder(final HibernateSessionProvider sessionProvider,
      final Class<?>[] classesToIndex, final int batchSize, final int numberOfThreads) {
    this.sessionProvider = sessionProvider;
    this.classesToIndex = classesToIndex;
    this.batchSize = batchSize;
    this.numberOfThreads = numberOfThreads;
    isBuildingComplete = false;
  }

  public void startBuildingIndex() {
    if (isBuildingComplete) {
      LOG.warn("Index for {} has already been built", getClassNames());
      return;
    }

    LOG.info("Building index for {}", getClassNames());
    final FullTextSession fullTextSession = Search.getFullTextSession(sessionProvider.getSession());
    final MassIndexer massIndexer = fullTextSession.createIndexer(classesToIndex);
    massIndexer.threadsToLoadObjects(numberOfThreads);
    massIndexer.batchSizeToLoadObjects(batchSize);
    massIndexer.progressMonitor(new SimpleIndexingProgressMonitor(batchSize) {
      @Override
      public void indexingCompleted() {
        super.indexingCompleted();

        sessionProvider.closeSession();
        isBuildingComplete = true;
        LOG.info("Completed building index for {}", getClassNames());
      }
    });
    massIndexer.start();
  }

  public void waitUntilBuildingIsComplete() {
    waitUntilBuildingIsComplete(null);
  }

  public void waitUntilBuildingIsComplete(final TaskExecution taskExecution) {
    final Waiter waiter = new Waiter() {
      @Override
      public boolean isConditionFulfilled() {
        return isBuildingComplete;
      }
    };
    waiter.setFeedbackMessage("Waiting until index for " + getClassNames() + " is built...");
    waiter.setTaskExecution(taskExecution);
    waiter.waitUntilConditionIsFulfilled();
  }

  private String getClassNames() {
    final List<String> classNames = Lists.newLinkedList();
    for (final Class<?> classToIndex : classesToIndex) {
      final String className = classToIndex.getSimpleName();
      classNames.add(className);
    }
    return StringUtils.join(classNames, ", ");
  }

}
