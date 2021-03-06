package com.romeikat.datamessie.core.base.util.publishedDates;

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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.Document;

public abstract class PublishedDateStrategy {

  private DocumentsFilterSettings dfs;
  private final List<LocalDate> publishedDates;

  public PublishedDateStrategy(final DocumentsFilterSettings dfs,
      final SessionFactory sessionFactory, final SharedBeanProvider sharedBeanProvider) {
    this.dfs = dfs;

    // Modify DFS in order to determine document IDs only once
    transformCleanedContentIntoDocumentIds(sharedBeanProvider);

    // Determine published dates (in descending order)
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    publishedDates = getPublishedDatesForProcessing(sessionProvider.getStatelessSession(), dfs);
    sessionProvider.closeStatelessSession();
  }

  private void transformCleanedContentIntoDocumentIds(final SharedBeanProvider sharedBeanProvider) {
    // Modify in order to determine document IDs only once
    final String luceneQueryString = dfs.getCleanedContent();
    if (StringUtils.isEmpty(luceneQueryString)) {
      return;
    }

    final DocumentsFilterSettings modifiedDfs = dfs.clone();
    modifiedDfs.transformCleanedContentIntoDocumentIds(sharedBeanProvider);
    dfs = modifiedDfs;
  }

  private List<LocalDate> getPublishedDatesForProcessing(final SharedSessionContract ssc,
      final DocumentsFilterSettings dfs) {
    final LocalDate fromDate = dfs.getFromDate();
    final LocalDate toDate = dfs.getToDate();

    Collection<LocalDate> publishedDatesForProcessing = null;

    // If both bounds are provided => use the dates in between as starting point
    if (fromDate != null && toDate != null) {
      publishedDatesForProcessing = DateUtil.getLocalDatesBetween(toDate, fromDate);
    }

    // If document IDs are provided, restrict to their published dates
    if (dfs.getDocumentIds() != null) {
      final Set<LocalDate> restrictingDates =
          getPublishedDatesOfDocuments(ssc, dfs.getDocumentIds());
      publishedDatesForProcessing = restrictToDates(publishedDatesForProcessing, restrictingDates);
    }

    // If still no published dates, use all published dates that actually exist for documents
    if (publishedDatesForProcessing == null) {
      final Set<LocalDate> allPublishedDates = getAllPublishedDates(ssc);
      final Predicate<LocalDate> publishedDatePredicate = new Predicate<LocalDate>() {
        @Override
        public boolean apply(final LocalDate publishedDate) {
          final boolean fromDateMatch = fromDate == null || publishedDate.compareTo(fromDate) >= 0;
          final boolean toDateMatch = toDate == null || publishedDate.compareTo(toDate) <= 0;
          return fromDateMatch && toDateMatch;
        }
      };
      publishedDatesForProcessing = Collections2.filter(allPublishedDates, publishedDatePredicate);
    }

    // Sort descending
    final List<LocalDate> publishedDatesForProcessingSorted =
        Lists.newArrayList(publishedDatesForProcessing);
    Collections.sort(publishedDatesForProcessingSorted);
    Collections.reverse(publishedDatesForProcessingSorted);
    // Done
    return publishedDatesForProcessingSorted;
  }

  private Collection<LocalDate> restrictToDates(final Collection<LocalDate> candidateDates,
      @NotNull final Collection<LocalDate> restrictingDates) {
    if (candidateDates == null) {
      return restrictingDates;
    }

    candidateDates.retainAll(restrictingDates);
    return candidateDates;
  }

  private Set<LocalDate> getPublishedDatesOfDocuments(final SharedSessionContract ssc,
      @NotNull final Collection<Long> documentIds) {
    if (documentIds.isEmpty()) {
      return Collections.emptySet();
    }

    final List<LocalDateTime> publishedTimestamps =
        getPublishedTimestampsOfDocuments(ssc, documentIds);
    final List<LocalDate> publishedDates =
        Lists.transform(publishedTimestamps, new Function<LocalDateTime, LocalDate>() {
          @Override
          public LocalDate apply(final LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
          }
        });
    final Set<LocalDate> uniquePublishedDates = Sets.newHashSet(publishedDates);
    return uniquePublishedDates;
  }

  private List<LocalDateTime> getPublishedTimestampsOfDocuments(final SharedSessionContract ssc,
      @NotNull final Collection<Long> documentIds) {
    // Query
    final Criteria criteria = ssc.createCriteria(Document.class);
    criteria.add(Restrictions.in("id", documentIds));
    // Projection
    criteria.setProjection(Projections.property("published"));
    // Done
    @SuppressWarnings("unchecked")
    final List<LocalDateTime> publishedTimestamps = criteria.list();
    return publishedTimestamps;
  }

  private Set<LocalDate> getAllPublishedDates(final SharedSessionContract ssc) {
    final List<LocalDateTime> publishedTimestamps = getAllPublishedTimestamps(ssc);
    final List<LocalDate> publishedDates =
        Lists.transform(publishedTimestamps, new Function<LocalDateTime, LocalDate>() {
          @Override
          public LocalDate apply(final LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
          }
        });
    final Set<LocalDate> uniquePublishedDates = Sets.newHashSet(publishedDates);
    return uniquePublishedDates;
  }

  private List<LocalDateTime> getAllPublishedTimestamps(final SharedSessionContract ssc) {
    // Query
    final Criteria criteria = ssc.createCriteria(Document.class);
    criteria.add(Restrictions.isNotNull("published"));
    // Projection
    criteria.setProjection(Projections.property("published"));
    // Done
    @SuppressWarnings("unchecked")
    final List<LocalDateTime> publishedTimestamps = criteria.list();
    return publishedTimestamps;
  }

  /**
   * Returns the original {@link DocumentsFilterSettings}.
   *
   * @return
   */
  protected DocumentsFilterSettings getDocumentsFilterSettings() {
    return dfs;
  }

  /**
   * Returns a list of all published dates in descending order.
   *
   * @return
   */
  protected List<LocalDate> getPublishedDates() {
    return publishedDates;
  }

}
