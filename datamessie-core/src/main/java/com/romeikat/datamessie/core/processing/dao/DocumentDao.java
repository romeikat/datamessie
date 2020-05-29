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
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;

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
    if (fromDate == null || toDate == null || CollectionUtils.isEmpty(states)
        || CollectionUtils.isEmpty(sourceIds)) {
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

  public void persistDocumentProcessingOutput(final Document documentToBeUpdated,
      final CleanedContent cleanedContentToBeCreatedOrUpdated,
      final StemmedContent stemmedContentToBeCreatedOrUpdated,
      final Collection<NamedEntityOccurrence> namedEntityOccurrencesToBeReplaced) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        updateDocument(statelessSession, documentToBeUpdated);
        createOrUpdateCleanedContent(statelessSession, cleanedContentToBeCreatedOrUpdated);
        createOrUpdateStemmedContent(statelessSession, stemmedContentToBeCreatedOrUpdated);
        replaceNamedEntityOccurrences(statelessSession, documentToBeUpdated.getId(),
            namedEntityOccurrencesToBeReplaced);

        documentService.deleteProcessedEntitiesOfDocumentsWithState(statelessSession,
            Lists.newArrayList(documentToBeUpdated), DocumentProcessingState.TO_BE_DELETED, true,
            true, true, true);
      }

      @Override
      protected boolean shouldRethrowException() {
        return true;
      }
    }.execute();
    sessionProvider.closeStatelessSession();
  }

  public void persistDocumentsProcessingOutput(final Collection<Document> documentsToBeUpdated,
      final Collection<Download> downloadsToBeCreatedOrUpdated,
      final Collection<RawContent> rawContentsToBeUpdated,
      final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced,
      final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        updateDocuments(statelessSession, documentsToBeUpdated);
        createOrUpdateDownloads(statelessSession, downloadsToBeCreatedOrUpdated);
        updateRawContents(statelessSession, rawContentsToBeUpdated);
        replaceNamedEntityOccurrences(statelessSession, namedEntityOccurrencesToBeReplaced);
        saveNamedEntityCategories(statelessSession, namedEntityCategoriesToBeSaved);

        documentService.deleteProcessedEntitiesOfDocumentsWithState(statelessSession,
            documentsToBeUpdated, DocumentProcessingState.TO_BE_DELETED, true, true, true, true);
      }

      @Override
      protected boolean shouldRethrowException() {
        return true;
      }
    }.execute();
    sessionProvider.closeStatelessSession();
  }

  private void updateDocuments(final StatelessSession statelessSession,
      final Collection<Document> documents) {
    if (documents == null) {
      return;
    }

    for (final Document document : documents) {
      updateDocument(statelessSession, document);
    }
  }

  private void updateDocument(final StatelessSession statelessSession, final Document document) {
    if (document == null) {
      return;
    }

    try {
      update(statelessSession, document);
    } catch (final Exception e) {
      LOG.error("Could not update document {} in version {}}", document.getId(),
          document.getVersion());
      throw e;
    }
  }

  private void createOrUpdateDownloads(final StatelessSession statelessSession,
      final Collection<Download> downloads) {
    if (downloads == null) {
      return;
    }

    for (final Download download : downloads) {
      downloadDao.insertOrUpdate(statelessSession, download);
    }
  }

  private void updateRawContents(final StatelessSession statelessSession,
      final Collection<RawContent> rawContents) {
    if (rawContents == null) {
      return;
    }

    for (final RawContent rawContent : rawContents) {
      rawContentDao.update(statelessSession, rawContent);
    }
  }

  private void createOrUpdateCleanedContent(final StatelessSession statelessSession,
      final CleanedContent cleanedContent) {
    if (cleanedContent == null) {
      return;
    }

    cleanedContentDao.insertOrUpdate(statelessSession, cleanedContent);
  }

  private void createOrUpdateStemmedContent(final StatelessSession statelessSession,
      final StemmedContent stemmedContent) {
    if (stemmedContent == null) {
      return;
    }

    stemmedContentDao.insertOrUpdate(statelessSession, stemmedContent);
  }

  private void replaceNamedEntityOccurrences(final StatelessSession statelessSession,
      final long documentId,
      final Collection<NamedEntityOccurrence> namedEntityOccurrencesToBeReplaced) {
    if (namedEntityOccurrencesToBeReplaced == null) {
      return;
    }

    final Map<Long, Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplacedAsMap =
        Maps.newHashMap();
    namedEntityOccurrencesToBeReplacedAsMap.put(documentId, namedEntityOccurrencesToBeReplaced);
    replaceNamedEntityOccurrences(statelessSession, namedEntityOccurrencesToBeReplacedAsMap);
  }

  private void replaceNamedEntityOccurrences(final StatelessSession statelessSession,
      final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced) {
    if (namedEntityOccurrencesToBeReplaced == null) {
      return;
    }

    // Delete
    final Collection<Long> documentIds = namedEntityOccurrencesToBeReplaced.keySet();
    namedEntityOccurrenceDao.deleteForDocuments(statelessSession, documentIds);

    // Create
    for (final long documentId : documentIds) {
      final Collection<NamedEntityOccurrence> namedEntityOccurrences =
          namedEntityOccurrencesToBeReplaced.get(documentId);
      for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
        namedEntityOccurrenceDao.insert(statelessSession, namedEntityOccurrence);
      }
    }
  }

  private void saveNamedEntityCategories(final StatelessSession statelessSession,
      final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved) {
    if (namedEntityCategoriesToBeSaved == null) {
      return;
    }

    for (final NamedEntityCategory namedEntityCategory : namedEntityCategoriesToBeSaved) {
      namedEntityCategoryDao.insert(statelessSession, namedEntityCategory);
    }
  }

}
