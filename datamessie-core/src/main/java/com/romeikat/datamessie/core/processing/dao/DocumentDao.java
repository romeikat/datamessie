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

import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.Project2SourceQuery;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Repository("processingDocumentDao")
public class DocumentDao extends com.romeikat.datamessie.core.base.dao.impl.DocumentDao {

  public List<Document> getToProcess(final SharedSessionContract ssc, final LocalDate downloaded,
      final int maxResults) {
    final LocalDateTime minDownloaded = LocalDateTime.of(downloaded, LocalTime.MIDNIGHT);
    final LocalDateTime maxDownloaded = LocalDateTime.of(downloaded.plusDays(1), LocalTime.MIDNIGHT);

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
    final Object[] statesForProcessing = new DocumentProcessingState[] {DocumentProcessingState.DOWNLOADED,
        DocumentProcessingState.REDIRECTED, DocumentProcessingState.CLEANED};
    documentQuery.addRestriction(Restrictions.in("state", statesForProcessing));
    documentQuery.setMaxResults(maxResults);

    // Done
    final List<Document> entities = documentQuery.listObjects(ssc);
    return entities;
  }

}
