package com.romeikat.datamessie.core.base.util.publishedDates.loading.sequence;

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
import com.romeikat.datamessie.core.base.util.publishedDates.loading.PublishedDateLoadingStrategy;

public abstract class PublishedDateSequenceLoadingStrategy<T> extends PublishedDateLoadingStrategy<T> {

  private static final Logger LOG = LoggerFactory.getLogger(PublishedDateSequenceLoadingStrategy.class);

  @Override
  protected abstract T initializeEmptyResult();

  protected abstract T load(SharedSessionContract ssc, DocumentsFilterSettings dfsWithPublishedDate,
      long firstForPublishedDate, final long countForPublishedDate);

  protected abstract long count(SharedSessionContract ssc, DocumentsFilterSettings dfsWithPublishedDate);

  @Override
  protected abstract void mergeResults(T previousResult, T nextResult);

  protected abstract long getCount(T result);

  private final long first;
  private final long count;
  private final HibernateSessionProvider sessionProvider;

  public PublishedDateSequenceLoadingStrategy(final DocumentsFilterSettings dfs, final long first, final long count,
      final SessionFactory sessionFactory, final SharedBeanProvider sharedBeanProvider) {
    super(dfs, sessionFactory, sharedBeanProvider);

    this.first = first;
    this.count = count;
    sessionProvider = new HibernateSessionProvider(sessionFactory);
  }

  public T getResult() {
    // Otherwise, load separately for each published date
    final T result = initializeEmptyResult();

    // Process published dates in sequence
    long firstForPublishedDate = first;
    long countForPublishedDate = count;
    final List<LocalDate> publishedDates = getPublishedDates();
    for (final LocalDate publishedDate : publishedDates) {
      if (countForPublishedDate <= 0) {
        break;
      }
      if (firstForPublishedDate < 0) {
        firstForPublishedDate = 0;
      }

      // No results to be skipped
      if (firstForPublishedDate == 0) {
        // Load result for published date
        LOG.debug("Loading data for publishedDate {}", publishedDate);
        final T nextResult = loadForPublishedDate(sessionProvider.getStatelessSession(), publishedDate,
            firstForPublishedDate, countForPublishedDate);
        if (nextResult == null) {
          // Skip whole published date
          continue;
        }

        // Merge with previous results
        synchronized (result) {
          mergeResults(result, nextResult);
        }

        // Reduce remaining number
        countForPublishedDate = countForPublishedDate - getCount(nextResult);
      }

      // Results to be skipped
      else {
        final long toBeSkipped = countForPublishedDate(sessionProvider.getStatelessSession(), publishedDate);
        // Skip whole published date
        if (toBeSkipped < firstForPublishedDate) {
          // Reduce remaining number
          firstForPublishedDate = firstForPublishedDate - toBeSkipped;
        }
        // Use result of published date
        else {
          // Load result for published date
          LOG.debug("Loading data for publishedDate {}", publishedDate);
          final T nextResult = loadForPublishedDate(sessionProvider.getStatelessSession(), publishedDate,
              firstForPublishedDate, countForPublishedDate);

          // Merge with previous results
          synchronized (result) {
            mergeResults(result, nextResult);
          }

          // No more skipping
          firstForPublishedDate = 0;
          // Reduce remaining number
          countForPublishedDate = countForPublishedDate - getCount(nextResult);
        }
      }
    }

    // Done
    sessionProvider.closeStatelessSession();
    return result;
  }

  private T loadForPublishedDate(final SharedSessionContract ssc, final LocalDate publishedDate,
      final long firstForPublishedDate, final long countForPublishedDate) {
    final DocumentsFilterSettings dfs = getDocumentsFilterSettings();

    // Restrict filter settings to the provided published date
    final DocumentsFilterSettings dfsForQuery = dfs.clone();
    dfsForQuery.setFromDate(publishedDate);
    dfsForQuery.setToDate(publishedDate);

    final T resultForPublishedDate = load(ssc, dfsForQuery, firstForPublishedDate, countForPublishedDate);
    return resultForPublishedDate;
  }

  private long countForPublishedDate(final SharedSessionContract ssc, final LocalDate publishedDate) {
    final DocumentsFilterSettings dfs = getDocumentsFilterSettings();

    // Restrict filter settings to the provided published date
    final DocumentsFilterSettings dfsForQuery = dfs.clone();
    dfsForQuery.setFromDate(publishedDate);
    dfsForQuery.setToDate(publishedDate);

    final long countForPublishedDate = count(ssc, dfsForQuery);
    return countForPublishedDate;
  }

}
