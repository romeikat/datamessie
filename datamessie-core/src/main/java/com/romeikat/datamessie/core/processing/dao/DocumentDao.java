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
import java.util.Map.Entry;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.StemmedContentDao;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.Project2SourceQuery;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Repository("processingDocumentDao")
public class DocumentDao extends com.romeikat.datamessie.core.base.dao.impl.DocumentDao {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentDao.class);

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

  public List<Document> getToProcess(final SharedSessionContract ssc, final LocalDate downloaded,
      Collection<DocumentProcessingState> statesForProcessing, final int maxResults) {
    if (statesForProcessing.isEmpty()) {
      return Collections.emptyList();
    }

    final LocalDateTime minDownloaded = LocalDateTime.of(downloaded, LocalTime.MIDNIGHT);
    final LocalDateTime maxDownloaded =
        LocalDateTime.of(downloaded.plusDays(1), LocalTime.MIDNIGHT);

    // Query: Project
    final EntityWithIdQuery<Project> projectQuery = new EntityWithIdQuery<>(Project.class);
    projectQuery.addRestriction(Restrictions.eq("preprocessingEnabled", true));
    final Collection<Long> projectIds = projectQuery.listIds(ssc);
    if (projectIds.isEmpty()) {
      return Collections.emptyList();
    }
    // Query: Project2Source
    final Project2SourceQuery project2SourceQuery = new Project2SourceQuery();
    project2SourceQuery.addRestriction(Restrictions.in("projectId", projectIds));
    final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
    if (sourceIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.in("sourceId", sourceIds));
    documentQuery.addRestriction(Restrictions.ge("downloaded", minDownloaded));
    documentQuery.addRestriction(Restrictions.lt("downloaded", maxDownloaded));
    documentQuery.addRestriction(Restrictions.in("state", statesForProcessing));
    documentQuery.setMaxResults(maxResults);

    // Done
    final List<Document> entities = documentQuery.listObjects(ssc);
    return entities;
  }

  public void persistDocumentsProcessingOutput(final StatelessSession statelessSession,
      final Collection<Document> documentsToBeUpdated,
      final Collection<Download> downloadsToBeCreatedOrUpdated,
      final Collection<RawContent> rawContentsToBeUpdated,
      final Collection<CleanedContent> cleanedContentsToBeCreatedOrUpdated,
      final Collection<StemmedContent> stemmedContentsToBeCreatedOrUpdated,
      final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced,
      final Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved) {
    new ExecuteWithTransaction(statelessSession) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        updateDocuments(statelessSession, documentsToBeUpdated);
        createOrUpdateDownloads(statelessSession, downloadsToBeCreatedOrUpdated);
        updateRawContents(statelessSession, rawContentsToBeUpdated);
        createOrUpdateCleanedContents(statelessSession, cleanedContentsToBeCreatedOrUpdated);
        createOrUpdateStemmedContents(statelessSession, stemmedContentsToBeCreatedOrUpdated);
        replaceNamedEntityOccurrences(statelessSession, namedEntityOccurrencesToBeReplaced);
        saveNamedEntityCategories(statelessSession, namedEntityCategoriesToBeSaved);
      }

      @Override
      protected boolean shouldRethrowException() {
        return true;
      }
    }.execute();
  }

  private void updateDocuments(final StatelessSession statelessSession,
      final Collection<Document> documentsToBeUpdated) {
    for (final Document document : documentsToBeUpdated) {
      update(statelessSession, document);
    }
  }

  private void createOrUpdateDownloads(final StatelessSession statelessSession,
      final Collection<Download> downloadsToBeCreatedOrUpdated) {
    for (final Download download : downloadsToBeCreatedOrUpdated) {
      downloadDao.insertOrUpdate(statelessSession, download);
    }
  }

  private void updateRawContents(final StatelessSession statelessSession,
      final Collection<RawContent> rawContentsToBeUpdated) {
    for (final RawContent rawContent : rawContentsToBeUpdated) {
      rawContentDao.update(statelessSession, rawContent);
    }
  }

  private void createOrUpdateCleanedContents(final StatelessSession statelessSession,
      final Collection<CleanedContent> cleanedContentsToBeCreatedOrUpdated) {
    for (final CleanedContent cleanedContents : cleanedContentsToBeCreatedOrUpdated) {
      cleanedContentDao.insertOrUpdate(statelessSession, cleanedContents);
    }
  }

  private void createOrUpdateStemmedContents(final StatelessSession statelessSession,
      final Collection<StemmedContent> stemmedContentsToBeCreatedOrUpdated) {
    for (final StemmedContent stemmedContents : stemmedContentsToBeCreatedOrUpdated) {
      stemmedContentDao.insertOrUpdate(statelessSession, stemmedContents);
    }
  }

  private void replaceNamedEntityOccurrences(final StatelessSession statelessSession,
      final Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced) {
    // Delete
    final Collection<Long> documentIds = namedEntityOccurrencesToBeReplaced.keySet();
    namedEntityOccurrenceDao.deleteForDocuments(statelessSession, documentIds);

    // Create
    for (final Entry<Long, ? extends Collection<NamedEntityOccurrence>> entry : namedEntityOccurrencesToBeReplaced
        .entrySet()) {
      final Collection<NamedEntityOccurrence> namedEntityOccurrences = entry.getValue();
      for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
        try {
          namedEntityOccurrenceDao.insert(statelessSession, namedEntityOccurrence);
        } catch (final Exception e) {
          final String msg =
              String.format("Could not insert named entity occurrence %s", namedEntityOccurrences);
          LOG.error(msg, e);
        }
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
