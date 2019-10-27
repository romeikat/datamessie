package com.romeikat.datamessie.core.base.query.document;

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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery.ReturnMode;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.CleanedContentQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.CrawlingQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.DocumentQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.NamedEntityOccurrenceQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.Project2SourceQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.ProjectQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.RawContentQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.Source2SourceTypeQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.SourceQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.SourceTypeQuery;
import com.romeikat.datamessie.core.base.query.entity.entities.StemmedContentQuery;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.entity.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.Crawling;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;
import com.romeikat.datamessie.core.domain.entity.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.SourceType;
import com.romeikat.datamessie.core.domain.entity.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.CrawlingImpl;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrenceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.RawContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceTypeImpl;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContentImpl;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;

public class DocumentFilterSettingsQuery<E extends EntityWithId> {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentFilterSettingsQuery.class);

  private DocumentsFilterSettings dfs;
  private final Class<? extends E> targetClass;
  private Integer firstResult;
  private Integer maxResults;
  private final List<Set<Long>> idRestrictions;
  private final SetMultimap<Class<?>, Criterion> restrictions;
  private final ListMultimap<Class<?>, Order> orders;
  private final SharedBeanProvider sharedBeanProvider;

  // Queries containing restrictions of the DFS
  private ProjectQuery projectQuery;
  private SourceTypeQuery sourceTypeQuery;
  private SourceQuery sourceQuery;
  private CrawlingQuery crawlingQuery;
  private DocumentQuery documentQuery;
  private RawContentQuery rawContentQuery;
  private CleanedContentQuery cleanedContentQuery;
  private StemmedContentQuery stemmedContentQuery;
  private NamedEntityOccurrenceQuery namedEntityOccurrenceQuery;

  private final Set<Class<?>> processedClasses;

  public DocumentFilterSettingsQuery(final DocumentsFilterSettings dfs,
      final Class<? extends E> targetClass, final SharedBeanProvider sharedBeanProvider) {
    this(dfs, targetClass, null, null, sharedBeanProvider);
  }

  public DocumentFilterSettingsQuery(final DocumentsFilterSettings dfs,
      final Class<? extends E> targetClass, final Integer firstResult, final Integer maxResults,
      final SharedBeanProvider sharedBeanProvider) {
    this.dfs = dfs;
    this.targetClass = targetClass;
    this.firstResult = firstResult;
    this.maxResults = maxResults;
    idRestrictions = Lists.newLinkedList();
    this.restrictions = HashMultimap.create();
    this.orders = ArrayListMultimap.create();
    this.sharedBeanProvider = sharedBeanProvider;

    processedClasses = Sets.newHashSet();
  }

  public DocumentFilterSettingsQuery<E> addIdRestriction(final Collection<Long> ids) {
    idRestrictions.add(Sets.newHashSet(ids));

    return this;
  }

  public DocumentFilterSettingsQuery<E> addRestriction(final Criterion restriction) {
    return addRestriction(targetClass, restriction);
  }

  public DocumentFilterSettingsQuery<E> addRestriction(final Class<?> clazz,
      final Criterion restriction) {
    restrictions.put(clazz, restriction);

    return this;
  }

  public DocumentFilterSettingsQuery<E> addOrder(final Order order) {
    return addOrder(targetClass, order);
  }

  public DocumentFilterSettingsQuery<E> addOrder(final Class<?> clazz, final Order order) {
    orders.put(clazz, order);

    return this;
  }

  public List<Long> listIds(final SharedSessionContract ssc) {
    final EntityWithIdQuery<E> overallQuery = buildOverallQuery(ssc);
    final List<Long> ids = overallQuery.listIds(ssc);
    return ids;
  }

  public List<E> listObjects(final SharedSessionContract ssc) {
    final EntityWithIdQuery<E> overallQuery = buildOverallQuery(ssc);
    final List<E> objects = overallQuery.listObjects(ssc);
    return objects;
  }

  public Long uniqueId(final SharedSessionContract ssc) {
    final EntityWithIdQuery<E> overallQuery = buildOverallQuery(ssc);
    final Long id = overallQuery.uniqueId(ssc);
    return id;
  }

  public E uniqueObject(final SharedSessionContract ssc) {
    final EntityWithIdQuery<E> overallQuery = buildOverallQuery(ssc);
    final E object = overallQuery.uniqueObject(ssc);
    return object;
  }

  public Long count(final SharedSessionContract ssc) {
    final EntityWithIdQuery<E> overallQuery = buildOverallQuery(ssc);
    final Long count = overallQuery.count(ssc);
    return count;
  }

  private EntityWithIdQuery<E> buildOverallQuery(final SharedSessionContract ssc) {
    // Modify DFS to determine document IDs only once
    transformCleanedContentIntoDocumentIds();

    // Query
    createSingleQueries();
    setReturnModesOfSingleQueries();
    final EntityWithIdQuery<E> overallQuery = createOverallQuery(ssc);

    // Restrictions
    for (final Entry<Class<?>, Criterion> entry : restrictions.entries()) {
      final Class<?> clazz = entry.getKey();
      final EntityWithIdQuery<?> query = getQuery(clazz);
      if (query == null) {
        LOG.warn("No query for class {} available", clazz.getName());
        continue;
      }

      final Criterion restriction = entry.getValue();
      query.addRestriction(restriction);
    }

    // Orders
    for (final Entry<Class<?>, Order> entry : orders.entries()) {
      final Class<?> clazz = entry.getKey();
      if (clazz != targetClass) {
        LOG.warn("Order for class {} cannot be applied to target class {}", clazz.getName(),
            targetClass.getName());
        continue;
      }

      final Order order = entry.getValue();
      overallQuery.addOrder(order);
    }

    // First and max
    overallQuery.setFirstResult(firstResult);
    overallQuery.setMaxResults(maxResults);

    return overallQuery;
  }

  private void transformCleanedContentIntoDocumentIds() {
    // Modify in order to determine document IDs only once
    final String luceneQueryString = dfs.getCleanedContent();
    if (StringUtils.isEmpty(luceneQueryString)) {
      return;
    }

    final DocumentsFilterSettings modifiedDfs = dfs.clone();
    modifiedDfs.transformCleanedContentIntoDocumentIds(sharedBeanProvider);
    dfs = modifiedDfs;
  }

  private void createSingleQueries() {
    projectQuery = new ProjectQuery(dfs);
    sourceTypeQuery = new SourceTypeQuery(dfs);
    sourceQuery = new SourceQuery(dfs);
    crawlingQuery = new CrawlingQuery(dfs);
    documentQuery = new DocumentQuery(dfs, idRestrictions);
    rawContentQuery = new RawContentQuery();
    cleanedContentQuery = new CleanedContentQuery();
    stemmedContentQuery = new StemmedContentQuery();
    namedEntityOccurrenceQuery = new NamedEntityOccurrenceQuery(dfs);
  }

  private void setReturnModesOfSingleQueries() {
    // If the query only returns an intermediate result, use null as special return value
    if (targetClass != ProjectImpl.class) {
      projectQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != SourceTypeImpl.class) {
      sourceTypeQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != SourceImpl.class) {
      sourceQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != CrawlingImpl.class) {
      crawlingQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != DocumentImpl.class) {
      documentQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != RawContentImpl.class) {
      rawContentQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != CleanedContentImpl.class) {
      cleanedContentQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != StemmedContentImpl.class) {
      stemmedContentQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
    if (targetClass != NamedEntityOccurrenceImpl.class) {
      namedEntityOccurrenceQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
    }
  }

  @SuppressWarnings("unchecked")
  private EntityWithIdQuery<E> createOverallQuery(final SharedSessionContract ssc) {
    EntityWithIdQuery<E> overallQuery = null;

    processedClasses.clear();
    if (targetClass == ProjectImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processProjectQuery(ssc);
    } else if (targetClass == SourceTypeImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processSourceTypeQuery(ssc);
    } else if (targetClass == SourceImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processSourceQuery(ssc);
    } else if (targetClass == CrawlingImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processCrawlingQuery(ssc);
    } else if (targetClass == DocumentImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processDocumentQuery(ssc);
    } else if (targetClass == RawContentImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processRawContentQuery(ssc);
    } else if (targetClass == CleanedContentImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processCleanedContentQuery(ssc);
    } else if (targetClass == StemmedContentImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processStemmedContentQuery(ssc);
    } else if (targetClass == NamedEntityOccurrenceImpl.class) {
      overallQuery = (EntityWithIdQuery<E>) processNamedEntityOccurrenceQuery(ssc);
    }
    processedClasses.clear();

    if (overallQuery == null) {
      final String msg =
          String.format("No path available to target class %s", targetClass.getName());
      throw new IllegalArgumentException(msg);
    }

    return overallQuery;
  }

  private EntityWithIdQuery<Project> processProjectQuery(final SharedSessionContract ssc) {
    final EntityWithIdQuery<Project> inputQuery = projectQuery;
    final EntityWithIdQuery<Project> processedQuery = new EntityWithIdQuery<Project>(inputQuery);

    processedClasses.add(ProjectImpl.class);

    if (!processedClasses.contains(SourceImpl.class)) {
      // Source...
      final EntityWithIdQuery<Source> processedSourceQuery = processSourceQuery(ssc);
      final Collection<Long> sourceIds = processedSourceQuery.listIds(ssc);
      // ...via Project2Source...
      final Project2SourceQuery project2SourceQuery = new Project2SourceQuery();
      project2SourceQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
      addInRestrictionForIds(project2SourceQuery, "sourceId", sourceIds);
      final Collection<Long> projectIds = project2SourceQuery.listIdsForProperty(ssc, "projectId");
      // ...to Project
      addInRestrictionForIds(processedQuery, "id", projectIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<SourceType> processSourceTypeQuery(final SharedSessionContract ssc) {
    final EntityWithIdQuery<SourceType> inputQuery = sourceTypeQuery;
    final EntityWithIdQuery<SourceType> processedQuery =
        new EntityWithIdQuery<SourceType>(inputQuery);

    processedClasses.add(SourceTypeImpl.class);

    if (!processedClasses.contains(SourceImpl.class)) {
      // Source...
      final EntityWithIdQuery<Source> processedSourceQuery = processSourceQuery(ssc);
      final Collection<Long> sourceIds = processedSourceQuery.listIds(ssc);
      // ...via Source2SourceType...
      final Source2SourceTypeQuery source2SourceTypeQuery = new Source2SourceTypeQuery();
      source2SourceTypeQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
      addInRestrictionForIds(source2SourceTypeQuery, "sourceId", sourceIds);
      final Collection<Long> sourceTypeIds =
          source2SourceTypeQuery.listIdsForProperty(ssc, "sourceTypeId");
      // ...to SourceType
      addInRestrictionForIds(processedQuery, "id", sourceTypeIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<Source> processSourceQuery(final SharedSessionContract ssc) {
    final EntityWithIdQuery<Source> inputQuery = sourceQuery;
    final EntityWithIdQuery<Source> processedQuery = new EntityWithIdQuery<Source>(inputQuery);

    processedClasses.add(SourceImpl.class);

    if (!processedClasses.contains(ProjectImpl.class)) {
      // Project...
      final EntityWithIdQuery<Project> processedProjectQuery = processProjectQuery(ssc);
      final Collection<Long> projectIds = processedProjectQuery.listIds(ssc);
      // ...via Project2Source...
      final Project2SourceQuery project2SourceQuery = new Project2SourceQuery();
      project2SourceQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
      addInRestrictionForIds(project2SourceQuery, "projectId", projectIds);
      final Collection<Long> sourceIds = project2SourceQuery.listIdsForProperty(ssc, "sourceId");
      // ...to Source
      addInRestrictionForIds(processedQuery, "id", sourceIds);
    }

    if (!processedClasses.contains(SourceTypeImpl.class)) {
      // SourceType...
      final EntityWithIdQuery<SourceType> processedSourceTypeQuery = processSourceTypeQuery(ssc);
      final Collection<Long> sourceTypeIds = processedSourceTypeQuery.listIds(ssc);
      // ...via Source2SourceType...
      final Source2SourceTypeQuery source2SourceTypeQuery = new Source2SourceTypeQuery();
      source2SourceTypeQuery.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);
      addInRestrictionForIds(source2SourceTypeQuery, "sourceTypeId", sourceTypeIds);
      final Collection<Long> sourceIds = source2SourceTypeQuery.listIdsForProperty(ssc, "sourceId");
      // ...to Source
      addInRestrictionForIds(processedQuery, "id", sourceIds);
    }

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to Source
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> sourceIds = processedDocumentQuery.listIdsForProperty(ssc, "sourceId");
      addInRestrictionForIds(processedQuery, "id", sourceIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<Crawling> processCrawlingQuery(final SharedSessionContract ssc) {
    final EntityWithIdQuery<Crawling> inputQuery = crawlingQuery;
    final EntityWithIdQuery<Crawling> processedQuery = new EntityWithIdQuery<Crawling>(inputQuery);

    processedClasses.add(CrawlingImpl.class);

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to Crawling
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> crawlingIds =
          processedDocumentQuery.listIdsForProperty(ssc, "crawlingId");
      addInRestrictionForIds(processedQuery, "id", crawlingIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<Document> processDocumentQuery(final SharedSessionContract ssc) {
    final EntityWithIdQuery<Document> inputQuery = documentQuery;
    final EntityWithIdQuery<Document> processedQuery = new EntityWithIdQuery<Document>(inputQuery);

    processedClasses.add(DocumentImpl.class);

    if (!processedClasses.contains(CrawlingImpl.class)) {
      // Crawling to Document
      final EntityWithIdQuery<Crawling> processedCrawlingQuery = processCrawlingQuery(ssc);
      final Collection<Long> crawlingIds = processedCrawlingQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "crawlingId", crawlingIds);
    }

    if (!processedClasses.contains(SourceImpl.class)) {
      // Source to Document
      final EntityWithIdQuery<Source> processedSourceQuery = processSourceQuery(ssc);
      final Collection<Long> sourceIds = processedSourceQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "sourceId", sourceIds);
    }

    if (!processedClasses.contains(RawContentImpl.class)) {
      // RawContent to Document
      final EntityWithIdQuery<RawContent> processedRawContentQuery = processRawContentQuery(ssc);
      final Collection<Long> documentIds =
          processedRawContentQuery.listIdsForProperty(ssc, "documentId");
      addInRestrictionForIds(processedQuery, "id", documentIds);
    }

    if (!processedClasses.contains(CleanedContentImpl.class)) {
      // CleanedContent to Document
      final EntityWithIdQuery<CleanedContent> processedCleanedContentQuery =
          processCleanedContentQuery(ssc);
      final Collection<Long> documentIds =
          processedCleanedContentQuery.listIdsForProperty(ssc, "documentId");
      addInRestrictionForIds(processedQuery, "id", documentIds);
    }

    if (!processedClasses.contains(StemmedContentImpl.class)) {
      // StemmedContent to Document
      final EntityWithIdQuery<StemmedContent> processedStemmedContentQuery =
          processStemmedContentQuery(ssc);
      final Collection<Long> documentIds =
          processedStemmedContentQuery.listIdsForProperty(ssc, "documentId");
      addInRestrictionForIds(processedQuery, "id", documentIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<RawContent> processRawContentQuery(final SharedSessionContract ssc) {
    processedClasses.add(RawContentImpl.class);

    final EntityWithIdQuery<RawContent> inputQuery = rawContentQuery;
    final EntityWithIdQuery<RawContent> processedQuery =
        new EntityWithIdQuery<RawContent>(inputQuery);

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to RawContent
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> documentIds = processedDocumentQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "documentId", documentIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<CleanedContent> processCleanedContentQuery(
      final SharedSessionContract ssc) {
    processedClasses.add(CleanedContentImpl.class);

    final EntityWithIdQuery<CleanedContent> inputQuery = cleanedContentQuery;
    final EntityWithIdQuery<CleanedContent> processedQuery =
        new EntityWithIdQuery<CleanedContent>(inputQuery);

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to RawContent
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> documentIds = processedDocumentQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "documentId", documentIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<StemmedContent> processStemmedContentQuery(
      final SharedSessionContract ssc) {
    processedClasses.add(StemmedContentImpl.class);

    final EntityWithIdQuery<StemmedContent> inputQuery = stemmedContentQuery;
    final EntityWithIdQuery<StemmedContent> processedQuery =
        new EntityWithIdQuery<StemmedContent>(inputQuery);

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to StemmedContent
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> documentIds = processedDocumentQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "documentId", documentIds);
    }

    return processedQuery;
  }

  private EntityWithIdQuery<NamedEntityOccurrence> processNamedEntityOccurrenceQuery(
      final SharedSessionContract ssc) {
    processedClasses.add(NamedEntityOccurrenceImpl.class);

    final EntityWithIdQuery<NamedEntityOccurrence> inputQuery = namedEntityOccurrenceQuery;
    final EntityWithIdQuery<NamedEntityOccurrence> processedQuery =
        new EntityWithIdQuery<NamedEntityOccurrence>(inputQuery);

    if (!processedClasses.contains(DocumentImpl.class)) {
      // Document to NamedEntityOccurrence
      final EntityWithIdQuery<Document> processedDocumentQuery = processDocumentQuery(ssc);
      final Collection<Long> documentIds = processedDocumentQuery.listIds(ssc);
      addInRestrictionForIds(processedQuery, "documentId", documentIds);
    }

    return processedQuery;
  }

  private void addInRestrictionForIds(final EntityQuery<?> query, final String propertyName,
      final Collection<Long> ids) {
    if (ids == null) {
      return;
    }

    if (ids.isEmpty()) {
      query.addRestriction(Restrictions.eq(propertyName, -1L));
    } else {
      query.addRestriction(Restrictions.in(propertyName, ids));
    }
  }

  private EntityWithIdQuery<?> getQuery(final Class<?> clazz) {
    if (clazz == ProjectImpl.class) {
      return projectQuery;
    } else if (clazz == SourceTypeImpl.class) {
      return sourceTypeQuery;
    } else if (clazz == SourceImpl.class) {
      return sourceQuery;
    } else if (clazz == CrawlingImpl.class) {
      return crawlingQuery;
    } else if (clazz == DocumentImpl.class) {
      return documentQuery;
    } else if (clazz == RawContentImpl.class) {
      return rawContentQuery;
    } else if (clazz == CleanedContentImpl.class) {
      return cleanedContentQuery;
    } else if (clazz == StemmedContentImpl.class) {
      return stemmedContentQuery;
    } else if (clazz == NamedEntityOccurrenceImpl.class) {
      return namedEntityOccurrenceQuery;
    } else {
      return null;
    }
  }

}
