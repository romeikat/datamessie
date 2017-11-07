package com.romeikat.datamessie.core.base.util.publishedDates.loading.parallel;

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
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.base.util.publishedDates.loading.PublishedDateLoadingStrategy;

public abstract class PublishedDateParallelLoadingStrategy<T>
    extends PublishedDateLoadingStrategy<T> {

  private static final Logger LOG =
      LoggerFactory.getLogger(PublishedDateParallelLoadingStrategy.class);

  @Override
  protected abstract T initializeEmptyResult();

  protected abstract T load(SharedSessionContract ssc,
      DocumentsFilterSettings dfsWithPublishedDate);

  @Override
  protected abstract void mergeResults(T previousResult, T nextResult);

  private final SessionFactory sessionFactory;
  private final Double parallelismFactor;

  public PublishedDateParallelLoadingStrategy(final DocumentsFilterSettings dfs,
      final SessionFactory sessionFactory, final SharedBeanProvider sharedBeanProvider,
      final Double parallelismFactor) {
    super(dfs, sessionFactory, sharedBeanProvider);

    this.sessionFactory = sessionFactory;
    this.parallelismFactor = parallelismFactor;
  }

  public T getResult() {
    // Otherwise, load separately for each published date
    final T result = initializeEmptyResult();

    // Process published dates in parallel
    final List<LocalDate> publishedDates = getPublishedDates();
    new ParallelProcessing<LocalDate>(sessionFactory, publishedDates, parallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final LocalDate publishedDate) {
        LOG.debug("Loading data for publishedDate {}", publishedDate);

        // Load result for published date
        final T nextResult =
            loadForPublishedDate(sessionProvider.getStatelessSession(), publishedDate);
        if (nextResult == null) {
          return;
        }

        // Merge with previous results
        synchronized (result) {
          mergeResults(result, nextResult);
        }
      }
    };

    // Done
    return result;
  }

  protected T loadForPublishedDate(final SharedSessionContract ssc, final LocalDate publishedDate) {
    final DocumentsFilterSettings dfs = getDocumentsFilterSettings();

    // Restrict filter settings to the provided published date
    final DocumentsFilterSettings dfsForQuery = dfs.clone();
    dfsForQuery.setFromDate(publishedDate);
    dfsForQuery.setToDate(publishedDate);

    final T resultForPublishedDate = load(ssc, dfsForQuery);
    return resultForPublishedDate;
  }

}
