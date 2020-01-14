package com.romeikat.datamessie.core.processing.dao;

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
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import com.google.common.collect.Multimap;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.StemmedContentDao;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.service.DocumentService;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import jersey.repackaged.com.google.common.collect.Lists;

@Repository("processingDocumentDao")
public class DocumentDao extends com.romeikat.datamessie.core.base.dao.impl.DocumentDao {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentDao.class);

  @Value("${documents.processing.parallelism.factor}")
  private Double processingParallelismFactor;

  @Autowired
  private DownloadDao downloadDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private StemmedContentDao stemmedContentDao;

  @Autowired
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Autowired
  @Qualifier("documentService")
  private DocumentService documentService;

  @Autowired
  private SessionFactory sessionFactory;

  public List<Document> getToProcess(final SharedSessionContract ssc, final LocalDate fromDate,
      final LocalDate toDate, final Collection<DocumentProcessingState> states,
      final Collection<Long> sourceIds, final Collection<Long> excludedDocumentIds,
      final int maxResults) {
    if (fromDate == null || toDate == null || states.isEmpty() || sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    final LocalDateTime minDownloaded = LocalDateTime.of(fromDate, LocalTime.MIDNIGHT);
    final LocalDateTime maxDownloaded = LocalDateTime.of(toDate.plusDays(1), LocalTime.MIDNIGHT);

    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.ge("downloaded", minDownloaded));
    documentQuery.addRestriction(Restrictions.lt("downloaded", maxDownloaded));
    documentQuery.addRestriction(Restrictions.in("state", states));
    documentQuery.addRestriction(Restrictions.in("sourceId", sourceIds));
    if (CollectionUtils.isNotEmpty(excludedDocumentIds)) {
      documentQuery.addRestriction(Restrictions.not(Restrictions.in("id", excludedDocumentIds)));
    }
    documentQuery.setMaxResults(maxResults);

    // Done
    final List<Document> entities = documentQuery.listObjects(ssc);
    return entities;
  }

  public void updateStates(final SharedSessionContract ssc, final Collection<Long> documentIds,
      final DocumentProcessingState state) {
    if (documentIds.isEmpty() || state == null) {
      return;
    }

    // Query
    final StringBuilder hql = new StringBuilder();
    hql.append("UPDATE " + getEntityClass().getSimpleName() + " ");
    hql.append("SET state = :_state ");
    hql.append("WHERE id IN :_ids ");
    final Query<?> query = ssc.createQuery(hql.toString());
    query.setParameter("_state", state);
    query.setParameter("_ids", documentIds);

    // Execute
    query.executeUpdate();
  }

  public void persistDocumentsProcessingOutput(final Map<Long, Document> documentsToBeUpdated,
      final Multimap<Long, Download> downloadsToBeCreatedOrUpdated,
      final Map<Long, RawContent> rawContentsToBeUpdated,
      final Map<Long, CleanedContent> cleanedContentsToBeCreatedOrUpdated,
      final Map<Long, StemmedContent> stemmedContentsToBeCreatedOrUpdated,
      final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced,
      final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved) {
    // Persist document-independent entities
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        saveNamedEntityCategories(statelessSession, namedEntityCategoriesToBeSaved);
      }

      @Override
      protected boolean shouldRethrowException() {
        return true;
      }
    }.execute();

    // Persist document-related entities
    final Collection<Long> documentIds = documentsToBeUpdated.keySet();
    new ParallelProcessing<Long>(sessionFactory, documentIds, processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Long documentId) {
        new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
          @Override
          protected void execute(final StatelessSession statelessSession) {
            LOG.debug("Persisting document {}", documentId);

            final Document document = documentsToBeUpdated.get(documentId);

            updateDocument(statelessSession, document);
            createOrUpdateDownloads(statelessSession,
                downloadsToBeCreatedOrUpdated.get(documentId));
            updateRawContent(statelessSession, rawContentsToBeUpdated.get(documentId));
            createOrUpdateCleanedContent(statelessSession,
                cleanedContentsToBeCreatedOrUpdated.get(documentId));
            createOrUpdateStemmedContent(statelessSession,
                stemmedContentsToBeCreatedOrUpdated.get(documentId));
            replaceNamedEntityOccurrences(statelessSession, documentId,
                namedEntityOccurrencesToBeReplaced.get(documentId));

            documentService.deleteProcessedEntitiesOfDocumentsWithState(statelessSession,
                Lists.newArrayList(document), DocumentProcessingState.TO_BE_DELETED, true, true,
                true, true);
          }

          @Override
          protected boolean shouldRethrowException() {
            return true;
          }
        }.execute();
      }
    };
  }

  private void updateDocument(final StatelessSession statelessSession, final Document document) {
    update(statelessSession, document);
  }

  private void createOrUpdateDownloads(final StatelessSession statelessSession,
      final Collection<Download> downloads) {
    for (final Download download : downloads) {
      downloadDao.insertOrUpdate(statelessSession, download);
    }
  }

  private void updateRawContent(final StatelessSession statelessSession,
      final RawContent rawContent) {
    if (rawContent != null) {
      rawContentDao.update(statelessSession, rawContent);
    }
  }

  private void createOrUpdateCleanedContent(final StatelessSession statelessSession,
      final CleanedContent cleanedContent) {
    if (cleanedContent != null) {
      cleanedContentDao.insertOrUpdate(statelessSession, cleanedContent);
    }
  }

  private void createOrUpdateStemmedContent(final StatelessSession statelessSession,
      final StemmedContent stemmedContent) {
    if (stemmedContent != null) {
      stemmedContentDao.insertOrUpdate(statelessSession, stemmedContent);
    }
  }

  private void replaceNamedEntityOccurrences(final StatelessSession statelessSession,
      final long documentId, final Collection<NamedEntityOccurrence> namedEntityOccurrences) {
    // Delete
    namedEntityOccurrenceDao.deleteForDocument(statelessSession, documentId);

    // Create
    for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
      try {
        namedEntityOccurrenceDao.insert(statelessSession, namedEntityOccurrence);
      } catch (final Exception e) {
        final String msg =
            String.format("Could not insert named entity occurrence %s", namedEntityOccurrence);
        LOG.error(msg, e);
      }
    }
  }

  private void saveNamedEntityCategories(final StatelessSession statelessSession,
      final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved) {
    for (final NamedEntityCategory namedEntityCategory : namedEntityCategoriesToBeSaved) {
      try {
        namedEntityCategoryDao.insert(statelessSession, namedEntityCategory);
      } catch (final Exception e) {
        final String msg =
            String.format("Could not insert named entity category %s", namedEntityCategory);
        LOG.error(msg, e);
      }
    }
  }

}
