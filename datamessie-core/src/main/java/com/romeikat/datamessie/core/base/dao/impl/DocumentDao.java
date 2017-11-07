package com.romeikat.datamessie.core.base.dao.impl;

import java.sql.Date;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.wicket.util.lang.Objects;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.query.document.DocumentFilterSettingsQuery;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.base.service.NamedEntityService;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.publishedDates.loading.parallel.CountPublishedDateParallelLoadingStrategy;
import com.romeikat.datamessie.core.base.util.publishedDates.loading.sequence.ListPublishedDateSequenceLoadingStrategy;
import com.romeikat.datamessie.core.domain.dto.DocumentDto;
import com.romeikat.datamessie.core.domain.dto.DocumentOverviewDto;
import com.romeikat.datamessie.core.domain.dto.NamedEntityDto;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;

@Repository
public class DocumentDao extends AbstractEntityWithIdAndVersionDao<Document> {

  @Autowired
  @Qualifier("namedEntityService")
  private NamedEntityService namedEntityService;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Value("${documents.loading.parallelism.factor}")
  private Double parallelismFactor;

  public DocumentDao() {
    super(Document.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public Document getForUrlAndSource(final SharedSessionContract ssc, final String url,
      final long sourceId) {
    // Query: Download
    final EntityWithIdQuery<Download> downloadQuery = new EntityWithIdQuery<>(Download.class);
    downloadQuery.addRestriction(Restrictions.eq("url", url));
    downloadQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    final Long documentId = downloadQuery.uniqueIdForProperty(ssc, "documentId");
    if (documentId == null) {
      return null;
    }

    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.idEq(documentId));
    final Document document = documentQuery.uniqueObject(ssc);
    return document;
  }

  public List<Document> getForSourceAndDownloaded(final SharedSessionContract ssc,
      final long sourceId, final LocalDate downloaded) {
    final LocalDateTime minDownloaded = LocalDateTime.of(downloaded, LocalTime.MIDNIGHT);
    final LocalDateTime maxDownloaded =
        LocalDateTime.of(downloaded.plusDays(1), LocalTime.MIDNIGHT);

    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    documentQuery.addRestriction(Restrictions.ge("downloaded", minDownloaded));
    documentQuery.addRestriction(Restrictions.lt("downloaded", maxDownloaded));

    // Done
    final List<Document> entities = documentQuery.listObjects(ssc);
    return entities;
  }

  public Map<RawContent, Document> getForRawContents(final SharedSessionContract ssc,
      final Collection<RawContent> rawContents) {
    // Query for documents
    final Set<Long> documentIds =
        rawContents.stream().map(c -> c.getDocumentId()).collect(Collectors.toSet());
    final Map<Long, Document> documentsById = getIdsWithEntities(ssc, documentIds);

    // Map rawContents -> documents
    final Map<RawContent, Document> result = Maps.newHashMapWithExpectedSize(rawContents.size());
    for (final RawContent rawContent : rawContents) {
      final Document document = documentsById.get(rawContent.getDocumentId());
      result.put(rawContent, document);
    }
    return result;
  }

  public Map<CleanedContent, Document> getForCleanedContents(final SharedSessionContract ssc,
      final Collection<CleanedContent> cleanedContents) {
    // Query for documents
    final Set<Long> documentIds =
        cleanedContents.stream().map(c -> c.getDocumentId()).collect(Collectors.toSet());
    final Map<Long, Document> documentsById = getIdsWithEntities(ssc, documentIds);

    // Map cleanedContents -> documents
    final Map<CleanedContent, Document> result =
        Maps.newHashMapWithExpectedSize(cleanedContents.size());
    for (final CleanedContent cleanedContent : cleanedContents) {
      final Document document = documentsById.get(cleanedContent.getDocumentId());
      result.put(cleanedContent, document);
    }
    return result;
  }

  public Map<StemmedContent, Document> getForStemmedContents(final SharedSessionContract ssc,
      final Collection<StemmedContent> stemmedContents) {
    // Query for documents
    final Set<Long> documentIds =
        stemmedContents.stream().map(c -> c.getDocumentId()).collect(Collectors.toSet());
    final Map<Long, Document> documentsById = getIdsWithEntities(ssc, documentIds);

    // Map rawContents -> documents
    final Map<StemmedContent, Document> result =
        Maps.newHashMapWithExpectedSize(stemmedContents.size());
    for (final StemmedContent stemmedContent : stemmedContents) {
      final Document document = documentsById.get(stemmedContent.getDocumentId());
      result.put(stemmedContent, document);
    }
    return result;
  }

  public Long count(final SharedSessionContract ssc, final DocumentsFilterSettings dfs) {
    final CountPublishedDateParallelLoadingStrategy loadingStrategy =
        new CountPublishedDateParallelLoadingStrategy(dfs, sessionFactory, sharedBeanProvider,
            parallelismFactor) {

          @Override
          protected MutableObject<Long> load(final SharedSessionContract ssc,
              final DocumentsFilterSettings dfsWithPublishedDate) {
            final Long count = countInternal(ssc, dfsWithPublishedDate);
            return new MutableObject<>(count);
          }
        };
    return loadingStrategy.getResult().getValue();
  }

  public DocumentDto getAsDto(final SharedSessionContract ssc, final long id) {
    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.idEq(id));
    final Document document = documentQuery.uniqueObject(ssc);
    if (document == null) {
      return null;
    }

    // Query: RawContent
    final EntityWithIdQuery<RawContent> rawContentQuery = new EntityWithIdQuery<>(RawContent.class);
    rawContentQuery.addRestriction(Restrictions.eq("documentId", document.getId()));
    final RawContent rawContent = rawContentQuery.uniqueObject(ssc);

    // Query: CleanedContent
    final EntityWithIdQuery<CleanedContent> cleanedContentQuery =
        new EntityWithIdQuery<>(CleanedContent.class);
    cleanedContentQuery.addRestriction(Restrictions.eq("documentId", document.getId()));
    final CleanedContent cleanedContent = cleanedContentQuery.uniqueObject(ssc);

    // Query: StemmedContent
    final EntityWithIdQuery<StemmedContent> stemmedContentQuery =
        new EntityWithIdQuery<>(StemmedContent.class);
    stemmedContentQuery.addRestriction(Restrictions.eq("documentId", document.getId()));
    final StemmedContent stemmedContent = stemmedContentQuery.uniqueObject(ssc);

    // Query: Source
    final EntityWithIdQuery<Source> sourceQuery = new EntityWithIdQuery<>(Source.class);
    sourceQuery.addRestriction(Restrictions.idEq(document.getSourceId()));
    final Source source = sourceQuery.uniqueObject(ssc);
    if (source == null) {
      return null;
    }

    // Transform
    final DocumentDto dto = new DocumentDto();
    dto.setId(document.getId());
    dto.setTitle(document.getTitle());
    dto.setStemmedTitle(document.getStemmedTitle());
    dto.setUrl(document.getUrl());
    dto.setDescription(document.getDescription());
    dto.setStemmedDescription(document.getStemmedDescription());
    dto.setPublished(document.getPublished());
    dto.setDownloaded(document.getDownloaded());
    dto.setStatusCode(document.getStatusCode());
    dto.setState(document.getState());
    dto.setRawContent(rawContent == null ? null : rawContent.getContent());
    dto.setCleanedContent(cleanedContent == null ? null : cleanedContent.getContent());
    dto.setStemmedContent(stemmedContent == null ? null : stemmedContent.getContent());
    dto.setSourceId(source.getId());
    dto.setSourceName(source.getName());
    dto.setSourceUrl(source.getUrl());

    // Named entities
    final List<NamedEntityDto> namedEntities = namedEntityService.getAsDtosByDocument(ssc, id);
    final String namedEntitiesAsString = getNamedEntitesAsString(namedEntities);
    dto.setNamedEntities(namedEntitiesAsString);

    // Done
    return dto;
  }

  private String getNamedEntitesAsString(final List<NamedEntityDto> namedEntities) {
    if (namedEntities.isEmpty()) {
      return "";
    }
    final StringBuilder namedEntitiesSB = new StringBuilder();
    for (final NamedEntityDto namedEntity : namedEntities) {
      namedEntitiesSB.append(namedEntity.getName());
      // if (namedEntity.hasDifferentParent()) {
      // namedEntitiesSB.append(" -> ");
      // namedEntitiesSB.append(namedEntity.getParentName());
      // }
      if (!namedEntity.getCategories().isEmpty()) {
        namedEntitiesSB.append(" <= ");
        namedEntitiesSB.append(namedEntity.getCategories());
      }
      namedEntitiesSB.append(" (");
      namedEntitiesSB.append(namedEntity.getType().getAbbreviation());
      namedEntitiesSB.append(" ");
      namedEntitiesSB.append(namedEntity.getQuantity());
      namedEntitiesSB.append("x)");
      namedEntitiesSB.append(String.format("%n"));
    }
    return namedEntitiesSB.toString();
  }

  public List<DocumentOverviewDto> getAsOverviewDtos(final SharedSessionContract ssc,
      final DocumentsFilterSettings dfs, final long first, final long count) {
    Assert.isTrue(first <= Integer.MAX_VALUE, "first must be within int range");
    Assert.isTrue(count <= Integer.MAX_VALUE, "count must be within int range");

    final ListPublishedDateSequenceLoadingStrategy<DocumentOverviewDto> loadingStrategy =
        new ListPublishedDateSequenceLoadingStrategy<DocumentOverviewDto>(dfs, first, count,
            sessionFactory, sharedBeanProvider) {

          @Override
          protected long count(final SharedSessionContract ssc,
              final DocumentsFilterSettings dfsWithPublishedDate) {
            return countInternal(ssc, dfsWithPublishedDate);
          }

          @Override
          protected List<DocumentOverviewDto> load(final SharedSessionContract ssc,
              final DocumentsFilterSettings dfsWithPublishedDate, final long firstForPublishedDate,
              final long countForPublishedDate) {
            return getAsOverviewDtosInternal(ssc, dfsWithPublishedDate, firstForPublishedDate,
                countForPublishedDate);
          }

        };
    return loadingStrategy.getResult();
  }

  private long countInternal(final SharedSessionContract ssc, final DocumentsFilterSettings dfs) {
    // Query for documents
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);
    final Long count = query.count(ssc);
    return Objects.defaultIfNull(count, 0l);
  }

  private List<DocumentOverviewDto> getAsOverviewDtosInternal(final SharedSessionContract ssc,
      final DocumentsFilterSettings dfs, final long first, final long count) {
    // Query for documents
    final DocumentFilterSettingsQuery<Document> query = new DocumentFilterSettingsQuery<Document>(
        dfs, Document.class, (int) first, (int) count, sharedBeanProvider);
    query.addOrder(Order.desc("published"));
    query.addOrder(Order.asc("sourceId"));
    query.addOrder(Order.asc("id"));
    final List<Document> documents = query.listObjects(ssc);

    // Query for sources
    final Map<Document, Source> sources = sourceDao.getForDocuments(ssc, documents);

    // Transform
    final List<DocumentOverviewDto> dtos =
        Lists.transform(documents, new Function<Document, DocumentOverviewDto>() {
          @Override
          public DocumentOverviewDto apply(final Document document) {
            final DocumentOverviewDto dto = new DocumentOverviewDto();
            final Source source = sources.get(document);

            dto.setId(document.getId());
            dto.setTitle(document.getTitle());
            dto.setUrl(document.getUrl());
            dto.setPublished(document.getPublished());
            dto.setDownloaded(document.getDownloaded());

            dto.setSourceId(source.getId());
            dto.setSourceName(source.getName());
            dto.setSourceUrl(source.getUrl());

            return dto;
          }
        });
    return dtos;
  }

  public List<Document> get(final SharedSessionContract ssc, final LocalDateTime downloaded) {
    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.eq("downloaded", downloaded));

    // Done
    final List<Document> documents = documentQuery.listObjects(ssc);
    return documents;
  }

  public List<LocalDate> getPublishedDates(final SharedSessionContract ssc) {
    // Query
    final StringBuilder hql = new StringBuilder();
    hql.append("SELECT DISTINCT DATE(d.published) ");
    hql.append("FROM document d ");
    hql.append("WHERE d.published IS NOT NULL ");
    @SuppressWarnings("unchecked")
    final Query<Date> query = ssc.createNativeQuery(hql.toString());

    // Execute
    final List<Date> publishedDates = query.list();
    return Lists.transform(publishedDates, d -> DateUtil.toLocalDate(d));
  }

  public LocalDateTime getMinDownloaded(final SharedSessionContract ssc) {
    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);

    // Done
    final Projection projection = Projections.min("downloaded");
    final LocalDateTime result = (LocalDateTime) documentQuery.uniqueForProjection(ssc, projection);
    return result;
  }

  public LocalDateTime getMaxDownloaded(final SharedSessionContract ssc, final long crawlingId) {
    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(Document.class);
    documentQuery.addRestriction(Restrictions.eq("crawlingId", crawlingId));

    // Done
    final Projection projection = Projections.max("downloaded");
    final LocalDateTime maxDownloaded =
        (LocalDateTime) documentQuery.uniqueForProjection(ssc, projection);
    return maxDownloaded;
  }

}
