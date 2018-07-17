package com.romeikat.datamessie.core.rss.task.maintenance;

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

import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.service.download.ContentDownloader;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.XmlUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;
import com.romeikat.datamessie.core.rss.service.DocumentService;

@SuppressWarnings("unused")
@Service(MaintenanceTask.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MaintenanceTask implements Task {

  public static final String BEAN_NAME = "maintenanceTask";

  public static final String NAME = "Maintenance";

  private static final Logger LOG = LoggerFactory.getLogger(MaintenanceTask.class);

  @Value("${maintenance.batch.size}")
  private int batchSize;

  @Autowired
  private ContentDownloader contentDownloader;

  @Autowired
  @Qualifier("rssDocumentService")
  private DocumentService documentService;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  private DocumentDao documentDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Autowired
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  private XmlUtil xmlUtil;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isVisibleAfterCompleted() {
    return false;
  }

  @Override
  public Integer getPriority() {
    return 5;
  }

  @Override
  public void execute(final TaskExecution taskExecution) throws TaskCancelledException {
    // Initialize
    taskExecution.reportWork("Starting maintenance");

    setCompletedTimestampForIncompleteCrawlings();

    // Document download timestamps
    // makeDocumentDownloadTimestampsUnique(taskExecution);

    // Invalid XML 1.0 characters
    // stripNonXml10Chars(taskExecution);

    // Escaped HTML characters
    // unescapeHtmlCharsFromContent(taskExecution);
    // unescapeHtmlCharsFromDocument(taskExecutio);

    // Done
    taskExecution.reportWork("Completed maintenance");
  }

  private void setCompletedTimestampForIncompleteCrawlings() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    final List<Crawling> incompletedCrawlings =
        crawlingDao.getIncompleted(sessionProvider.getStatelessSession());
    for (final Crawling crawling : incompletedCrawlings) {
      try {
        final LocalDateTime maxDownloaded =
            documentDao.getMaxDownloaded(sessionProvider.getStatelessSession(), crawling.getId());
        if (maxDownloaded == null) {
          continue;
        }
        crawling.setCompleted(maxDownloaded);
        crawlingDao.update(sessionProvider.getStatelessSession(), crawling);
      } catch (final Exception e) {
        LOG.error("Could not update completed date of crawling " + crawling.getId(), e);
        sessionProvider.closeStatelessSession();
      }
    }

    // Done
    sessionProvider.closeStatelessSession();
  }

  private void stripNonXml10Chars(final TaskExecution taskExecution) throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    // Get all IDs
    final List<Long> ids = documentDao.getIds(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    // Process IDs in batches
    final List<List<Long>> batches = CollectionUtil.splitIntoSubListsBySize(ids, batchSize);
    for (final List<Long> batch : batches) {
      new ParallelProcessing<Long>(sessionFactory, batch) {
        @Override
        public void doProcessing(final HibernateSessionProvider sessionProvider,
            final Long documentId) {
          // Strip characters
          final RawContent rawContent =
              rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
          if (rawContent == null) {
            return;
          }
          final String content = rawContent.getContent();
          final String strippedContent = xmlUtil.stripNonValidXMLCharacters(content);
          // If characters were stripped, remove any preprocessed information
          if (!strippedContent.equals(content)) {
            rawContent.setContent(strippedContent);
            rawContentDao.update(sessionProvider.getStatelessSession(), rawContent);
            final Document document =
                documentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
            document.setState(DocumentProcessingState.DOWNLOADED);
            documentDao.update(sessionProvider.getStatelessSession(), document);
            LOG.info("Stripped invalid XML 1.0 characters from content of document {}", documentId);
          }
        }
      };
      taskExecution.checkpoint();
    }
  }

  private void unescapeHtmlCharsFromContent(final TaskExecution taskExecution)
      throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    // Get all IDs
    final List<Long> ids = documentDao.getIds(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    // Process IDs in batches
    final List<List<Long>> batches = CollectionUtil.splitIntoSubListsBySize(ids, batchSize);
    for (final List<Long> batch : batches) {
      new ParallelProcessing<Long>(sessionFactory, batch) {
        @Override
        public void doProcessing(final HibernateSessionProvider sessionProvider,
            final Long documentId) {
          // Unescape characters
          final RawContent rawContent =
              rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
          if (rawContent == null) {
            return;
          }
          final String content = rawContent.getContent();
          final String unescapedContent = StringEscapeUtils.unescapeHtml4(content);
          // Remove any preprocessed information
          rawContent.setContent(unescapedContent);
          rawContentDao.update(sessionProvider.getStatelessSession(), rawContent);
          final Document document =
              documentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
          document.setState(DocumentProcessingState.DOWNLOADED);
          documentDao.update(sessionProvider.getStatelessSession(), document);
          final List<NamedEntityOccurrence> namedEntities = namedEntityOccurrenceDao
              .getByDocument(sessionProvider.getStatelessSession(), documentId);
          for (final NamedEntityOccurrence namedEntity : namedEntities) {
            namedEntityOccurrenceDao.delete(sessionProvider.getStatelessSession(), namedEntity);
          }
          LOG.info("Unescaped HTML characters from content of document {}", documentId);
        }
      };
      taskExecution.checkpoint();
    }
  }

  private void unescapeHtmlCharsFromDocument(final TaskExecution taskExecution)
      throws TaskCancelledException {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    // Get all IDs
    final List<Long> ids = documentDao.getIds(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    // Process IDs in batches
    final List<List<Long>> batches = CollectionUtil.splitIntoSubListsBySize(ids, batchSize);
    for (final List<Long> batch : batches) {
      new ParallelProcessing<Long>(sessionFactory, batch) {
        @Override
        public void doProcessing(final HibernateSessionProvider sessionProvider,
            final Long documentId) {
          // Unescape characters
          final Document document =
              documentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
          if (document == null) {
            return;
          }
          final String title = document.getTitle();
          final String unescapedTitle = StringEscapeUtils.unescapeHtml4(title);
          document.setTitle(unescapedTitle);
          final String description = document.getDescription();
          final String unescapedDescription = StringEscapeUtils.unescapeHtml4(description);
          document.setDescription(unescapedDescription);
          final boolean titleChanged =
              title != null && unescapedTitle != null && !title.equals(unescapedTitle);
          final boolean descriptionChanged = description != null && unescapedDescription != null
              && !description.equals(unescapedDescription);
          if (titleChanged || descriptionChanged) {
            documentDao.update(sessionProvider.getStatelessSession(), document);
            LOG.info("Unescaped HTML characters from document {}", documentId);
          }
        }
      };
      taskExecution.checkpoint();
    }
  }

}
