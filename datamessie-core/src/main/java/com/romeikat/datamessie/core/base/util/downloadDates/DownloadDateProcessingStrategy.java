package com.romeikat.datamessie.core.base.util.downloadDates;

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
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.util.DocumentsDatesConsumer;
import jersey.repackaged.com.google.common.base.Objects;

public class DownloadDateProcessingStrategy {

  private final LocalDate fromDate;
  private final LocalDate toDate;
  private final Collection<DocumentProcessingState> states;
  private final Collection<Long> sourceIds;

  private final int batchSize;

  private final SessionFactory sessionFactory;
  private final DocumentDao documentDao;
  private final StringUtil stringUtil;

  public DownloadDateProcessingStrategy(final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> states, final Collection<Long> sourceIds,
      final int batchSize, final ApplicationContext ctx) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.states = states;
    this.sourceIds = sourceIds;

    this.batchSize = batchSize;

    this.sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    this.documentDao = ctx.getBean("processingDocumentDao", DocumentDao.class);
    this.stringUtil = ctx.getBean(StringUtil.class);
  }

  public void process(final TaskExecution taskExecution, final ProcessingCallback callback)
      throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Determine downloaded dates
    final TaskExecutionWork work = taskExecution.reportWorkStart(String.format("Initializing"));
    final SortedMap<LocalDate, Long> datesWithDocuments =
        documentDao.getDownloadedDatesWithNumberOfDocuments(sessionProvider.getStatelessSession(),
            fromDate, toDate, states, sourceIds);
    final DocumentsDatesConsumer documentsDatesConsumer =
        new DocumentsDatesConsumer(datesWithDocuments, batchSize);
    taskExecution.reportWorkEnd(work);
    if (documentsDatesConsumer.isEmpty()) {
      taskExecution.reportWork("No documents to be processed");
      return;
    }

    // Initialize first date range
    MutablePair<LocalDate, LocalDate> downloadedDateRange = new MutablePair<LocalDate, LocalDate>();
    final Pair<LocalDate, LocalDate> firstDateRange = documentsDatesConsumer.getNextDateRange();
    downloadedDateRange.setLeft(firstDateRange.getLeft());
    downloadedDateRange.setRight(firstDateRange.getRight());

    // Process date ranges
    while (downloadedDateRange != null) {
      // Process
      final int processedDocuments = process(sessionProvider.getStatelessSession(), taskExecution,
          downloadedDateRange.getLeft(), downloadedDateRange.getRight(), states, callback);

      // Prepare for next iteration
      final boolean noMoreDocumentsToDeprocess = processedDocuments < batchSize;
      if (noMoreDocumentsToDeprocess) {
        documentsDatesConsumer.removeDates(downloadedDateRange.getRight());
        if (documentsDatesConsumer.isEmpty()) {
          downloadedDateRange = null;
        } else {
          final Pair<LocalDate, LocalDate> nextDateRange =
              documentsDatesConsumer.getNextDateRange();
          downloadedDateRange.setLeft(nextDateRange.getLeft());
          downloadedDateRange.setRight(nextDateRange.getRight());
        }
      }
    }
  }

  private int process(final StatelessSession statelessSession, final TaskExecution taskExecution,
      final LocalDate fromDate, final LocalDate toDate,
      final Collection<DocumentProcessingState> states, final ProcessingCallback callback)
      throws TaskCancelledException {
    // Load
    final boolean oneDateOnly = Objects.equal(fromDate, toDate);
    final StringBuilder msg = new StringBuilder();
    msg.append("Loading documents to process for download date");
    if (oneDateOnly) {
      msg.append(String.format(" %s", LocalDateConverter.INSTANCE_UI.convertToString(fromDate)));
    } else {
      msg.append(
          String.format("s %s to %s", LocalDateConverter.INSTANCE_UI.convertToString(fromDate),
              LocalDateConverter.INSTANCE_UI.convertToString(toDate)));
    }
    TaskExecutionWork work = taskExecution.reportWorkStart(msg.toString());
    final List<Document> documentsToMaintain =
        documentDao.getToProcess(statelessSession, fromDate, toDate, states, null, null, batchSize);
    taskExecution.reportWorkEnd(work);

    // Process
    final String singularPlural =
        stringUtil.getSingularOrPluralTerm("document", documentsToMaintain.size());
    work = taskExecution.reportWorkStart(
        String.format("Processing %s %s", documentsToMaintain.size(), singularPlural));
    callback.onProcessing(statelessSession, documentsToMaintain);
    taskExecution.reportWorkEnd(work);

    taskExecution.checkpoint();

    return documentsToMaintain.size();
  }

  public static interface ProcessingCallback {
    void onProcessing(StatelessSession statelessSession, Collection<Document> documents);
  }

}
