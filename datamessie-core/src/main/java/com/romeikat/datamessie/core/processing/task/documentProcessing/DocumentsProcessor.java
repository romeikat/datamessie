package com.romeikat.datamessie.core.processing.task.documentProcessing;

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

import org.apache.wicket.util.collections.ConcurrentHashSet;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.context.ApplicationContext;

import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cache.DocumentsProcessingCache;

public class DocumentsProcessor {

  private final ApplicationContext ctx;
  private final SessionFactory sessionFactory;

  private final Double processingParallelismFactor;

  private List<Document> documents;
  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;
  private final ConcurrentHashSet<Long> failedDocumentIds;

  public DocumentsProcessor(final ApplicationContext ctx) {
    this.ctx = ctx;
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);

    processingParallelismFactor =
        Double.parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));

    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();
    failedDocumentIds = new ConcurrentHashSet<Long>();
  }

  public void processDocuments(final List<Document> documents) throws TaskCancelledException {
    this.documents = documents;
    doProcessing();
  }

  private void doProcessing() throws TaskCancelledException {
    final DocumentsProcessingCache documentsProcessingCache = new DocumentsProcessingCache(documents, ctx);
    new ParallelProcessing<Document>(sessionFactory, documents, processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final Document document) {
        new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
          @Override
          protected void execute(final StatelessSession statelessSession) {
            final DocumentProcessor documentProcessor = createDocumentProcessor(ctx, documentsProcessingCache);
            documentProcessor.processDocument(sessionProvider.getStatelessSession(), document);
            statisticsToBeRebuilt.putValues(documentProcessor.getStatisticsToBeRebuilt());
            failedDocumentIds.addAll(documentProcessor.getFailedDocumentIds());
          }
        }.execute();
      }
    };
  }

  protected DocumentProcessor createDocumentProcessor(final ApplicationContext ctx,
      final DocumentsProcessingCache documentsProcessingCache) {
    return new DocumentProcessor(ctx, documentsProcessingCache);
  }

  public StatisticsRebuildingSparseTable getStatisticsToBeRebuilt() {
    return statisticsToBeRebuilt;
  }

  public ConcurrentHashSet<Long> getFailedDocumentIds() {
    return failedDocumentIds;
  }

}
