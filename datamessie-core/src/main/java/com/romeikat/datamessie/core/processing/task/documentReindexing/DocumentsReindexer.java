package com.romeikat.datamessie.core.processing.task.documentReindexing;

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

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;

@Service
public class DocumentsReindexer {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsReindexer.class);

  @Value("${documents.reindexing.pause}")
  private long pause;

  private final DocumentsToBeReindexed documentsToBeReindexed;

  private TaskExecutionWork work;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private StringUtil stringUtil;

  public DocumentsReindexer() {
    documentsToBeReindexed = new DocumentsToBeReindexed();
  }

  public void toBeReindexed(final Collection<Long> documentIds) {
    documentsToBeReindexed.toBeReindexed(documentIds);
  }

  public void performReindexing(final TaskExecution taskExecution) throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Initialize
    taskExecution.reportWork("Starting documents reindexing");

    // Repeatedly reindex documents
    while (true) {
      final Collection<Long> documentIds = documentsToBeReindexed.pollAll();

      // If no statistics to be rebuilt at the moment, wait and go on
      if (documentIds.isEmpty()) {
        taskExecution.checkpoint(pause);
        continue;
      }

      // Rebuild statistics
      updateFullTextIndex(sessionProvider, taskExecution, documentIds);
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private void updateFullTextIndex(final HibernateSessionProvider sessionProvider,
      final TaskExecution taskExecution, final Collection<Long> documentIds)
      throws TaskCancelledException {
    final String singularPlural =
        stringUtil.getSingularOrPluralTerm("document", documentIds.size());
    work = taskExecution
        .reportWorkStart(String.format("Reindexing %s %s", documentIds.size(), singularPlural));

    try {
      final Collection<CleanedContent> cleanedContents =
          cleanedContentDao.getEntities(sessionProvider.getSession(), documentIds);

      updateFullTextIndex(sessionProvider.getSession(), cleanedContents);

      sessionProvider.closeSession();
      taskExecution.reportWorkEnd(work);
    } catch (final Exception e) {
      final String msg = "Could not reindex documents " + StringUtils.join(documentIds, ", ");
      LOG.error(msg, e);
    }

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
  }

  private void updateFullTextIndex(final Session session, final Collection<?> objects) {
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    for (final Object o : objects) {
      fullTextSession.index(o);
    }
  }

}
