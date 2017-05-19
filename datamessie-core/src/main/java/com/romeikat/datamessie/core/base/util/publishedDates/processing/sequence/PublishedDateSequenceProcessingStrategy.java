package com.romeikat.datamessie.core.base.util.publishedDates.processing.sequence;

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
import com.romeikat.datamessie.core.base.util.publishedDates.processing.PublishedDateProcessingStrategy;

public abstract class PublishedDateSequenceProcessingStrategy extends PublishedDateProcessingStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(PublishedDateSequenceProcessingStrategy.class);

  private final HibernateSessionProvider sessionProvider;

  public PublishedDateSequenceProcessingStrategy(final DocumentsFilterSettings dfs, final SessionFactory sessionFactory,
      final SharedBeanProvider sharedBeanProvider) {
    super(dfs, sessionFactory, sharedBeanProvider);

    sessionProvider = new HibernateSessionProvider(sessionFactory);
  }

  public void process() {
    // Process published dates in sequence
    final List<LocalDate> publishedDates = getPublishedDates();
    for (final LocalDate publishedDate : publishedDates) {
      LOG.debug("Loading data for publishedDate {}", publishedDate);
      processForPublishedDate(sessionProvider.getStatelessSession(), publishedDate);
    }

    // Done
    sessionProvider.closeStatelessSession();
  }

  private void processForPublishedDate(final SharedSessionContract ssc, final LocalDate publishedDate) {
    final DocumentsFilterSettings dfs = getDocumentsFilterSettings();

    // Restrict filter settings to the provided published date
    final DocumentsFilterSettings dfsForQuery = dfs.clone();
    dfsForQuery.setFromDate(publishedDate);
    dfsForQuery.setToDate(publishedDate);

    process(ssc, dfsForQuery);
  }

  protected abstract void process(SharedSessionContract ssc, DocumentsFilterSettings dfsWithPublishedDate);

}
