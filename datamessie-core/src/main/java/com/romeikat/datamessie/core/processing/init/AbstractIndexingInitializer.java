package com.romeikat.datamessie.core.processing.init;

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

import javax.annotation.PostConstruct;

import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.processing.service.fulltext.index.IndexBuilder;

public abstract class AbstractIndexingInitializer {

  private IndexBuilder indexBuilder;

  @PostConstruct
  private void initialize() {
    if (shouldReindexAtStartup()) {
      startIndexing();
    }
  }

  protected abstract boolean shouldReindexAtStartup();

  protected abstract int getBatchSize();

  protected int getNumberOfThreads() {
    return Runtime.getRuntime().availableProcessors();
  }

  protected abstract HibernateSessionProvider getHibernateSessionProvider();

  protected abstract Class<?>[] getClassesToIndex();

  public void startIndexing() {
    indexBuilder =
        new IndexBuilder(getHibernateSessionProvider(), getClassesToIndex(), getBatchSize(), getNumberOfThreads());
    indexBuilder.startBuildingIndex();
  }

  public void waitUntilIndexesInitialized(final TaskExecution taskExecution) {
    if (indexBuilder == null) {
      return;
    }

    indexBuilder.waitUntilBuildingIsComplete(taskExecution);
  }

}
