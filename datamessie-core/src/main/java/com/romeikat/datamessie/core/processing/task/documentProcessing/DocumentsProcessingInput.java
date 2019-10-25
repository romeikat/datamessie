package com.romeikat.datamessie.core.processing.task.documentProcessing;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.RedirectingRuleDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.dao.impl.TagSelectingRuleDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import jersey.repackaged.com.google.common.collect.Maps;

public class DocumentsProcessingInput {

  private final SessionFactory sessionFactory;
  private final RawContentDao rawContentDao;
  private final SourceDao sourceDao;
  private final RedirectingRuleDao redirectingRuleDao;
  private final TagSelectingRuleDao tagSelectingRuleDao;

  private final ConcurrentMap<Long, Document> documentId2Document;

  private final Map<Long, RawContent> documentId2RawContent;
  private final Map<Long, Source> documentId2Source;
  private final ListMultimap<Long, RedirectingRule> sourceId2RedirectingRules;
  private final ListMultimap<Long, TagSelectingRule> sourceId2TagSelectingRules;

  private final SetMultimap<Long, NamedEntityDetectionDto> namedEntityDetections;


  public DocumentsProcessingInput(final ApplicationContext ctx) {
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    rawContentDao = ctx.getBean(RawContentDao.class);
    sourceDao = ctx.getBean(SourceDao.class);
    redirectingRuleDao = ctx.getBean(RedirectingRuleDao.class);
    tagSelectingRuleDao = ctx.getBean(TagSelectingRuleDao.class);
    namedEntityDetections = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    this.documentId2Document = new ConcurrentHashMap<Long, Document>();
    documentId2RawContent = Maps.newHashMap();
    documentId2Source = Maps.newHashMap();
    sourceId2RedirectingRules = ArrayListMultimap.create();
    sourceId2TagSelectingRules = ArrayListMultimap.create();
  }

  public void addDocuments(final Collection<Document> documents) {
    putDocuments(documents);

    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    loadAndPutRawContents(sessionProvider, documents);
    loadAndPutSources(sessionProvider, documents);
    loadAndPutRedirectingRules(sessionProvider, documents);
    loadAndPutTagSelectingRules(sessionProvider, documents);
    sessionProvider.closeStatelessSession();
  }

  private void putDocuments(final Collection<Document> documents) {
    for (final Document document : documents) {
      this.documentId2Document.put(document.getId(), document);
    }
  }

  private void loadAndPutRawContents(final HibernateSessionProvider sessionProvider,
      final Collection<Document> documents) {
    final Collection<Long> documentIds =
        documents.stream().map(d -> d.getId()).collect(Collectors.toSet());
    final Map<Long, RawContent> documentId2RawContent = new ConcurrentHashMap<Long, RawContent>(
        rawContentDao.getIdsWithEntities(sessionProvider.getStatelessSession(), documentIds));
    this.documentId2RawContent.putAll(documentId2RawContent);
  }

  private void loadAndPutSources(final HibernateSessionProvider sessionProvider,
      final Collection<Document> documents) {
    final Collection<Long> sourceIds =
        documents.stream().map(d -> d.getSourceId()).collect(Collectors.toSet());
    final Map<Long, Source> sourceId2Source =
        sourceDao.getIdsWithEntities(sessionProvider.getStatelessSession(), sourceIds);
    final Map<Long, Source> documentId2Source = documentId2Document.values().stream()
        .collect(Collectors.toMap(d -> d.getId(), d -> sourceId2Source.get(d.getSourceId())));
    this.documentId2Source.putAll(documentId2Source);
  }

  private void loadAndPutRedirectingRules(final HibernateSessionProvider sessionProvider,
      final Collection<Document> documents) {
    final Collection<Long> sourceIds =
        documents.stream().map(d -> d.getSourceId()).collect(Collectors.toSet());
    final ListMultimap<Long, RedirectingRule> sourceId2RedirectingRules =
        redirectingRuleDao.getPerSourceId(sessionProvider.getStatelessSession(), sourceIds);
    this.sourceId2RedirectingRules.putAll(sourceId2RedirectingRules);
  }

  private void loadAndPutTagSelectingRules(final HibernateSessionProvider sessionProvider,
      final Collection<Document> documents) {
    final Collection<Long> sourceIds =
        documents.stream().map(d -> d.getSourceId()).collect(Collectors.toSet());
    final ListMultimap<Long, TagSelectingRule> sourceId2TagSelectingRules =
        tagSelectingRuleDao.getPerSourceId(sessionProvider.getStatelessSession(), sourceIds);

    this.sourceId2TagSelectingRules.putAll(sourceId2TagSelectingRules);
  }

  public Set<Document> getDocuments() {
    return Sets.newHashSet(documentId2Document.values());
  }

  public void replaceDocuments(final Collection<Document> documents) {
    for (final Document document : documents) {
      replaceDocument(document);
    }
  }

  private void replaceDocument(final Document document) {
    documentId2Document.replace(document.getId(), document);
  }

  public void removeDocument(final Document document) {
    documentId2Document.remove(document.getId());
  }

  public RawContent getRawContent(final long documentId) {
    return documentId2RawContent.get(documentId);
  }

  public Source getSource(final long documentId) {
    return documentId2Source.get(documentId);
  }

  public List<RedirectingRule> getActiveRedirectingRules(final Document document,
      final LocalDate date) {
    // Rules of source
    final List<RedirectingRule> redirectingRules =
        sourceId2RedirectingRules.get(document.getSourceId());

    // Active rules
    final List<RedirectingRule> activeRedirectingRules = new LinkedList<RedirectingRule>();
    for (final RedirectingRule redirectingRule : redirectingRules) {
      if (redirectingRule.isActive(date)) {
        activeRedirectingRules.add(redirectingRule);
      }
    }

    return activeRedirectingRules;
  }

  public List<TagSelectingRule> getActiveTagSelectingRules(final Document document,
      final LocalDate date) {
    // Rules of source
    final List<TagSelectingRule> tagSelectingRuleRules =
        sourceId2TagSelectingRules.get(document.getSourceId());

    // Active rules
    final List<TagSelectingRule> activeTagSelectingRules = new LinkedList<TagSelectingRule>();
    for (final TagSelectingRule tagSelectingRule : tagSelectingRuleRules) {
      if (tagSelectingRule.isActive(date)) {
        activeTagSelectingRules.add(tagSelectingRule);
      }
    }

    return activeTagSelectingRules;
  }

  public void putNamedEntityDetections(final long documentId,
      final Collection<NamedEntityDetectionDto> namedEntityDetections) {
    this.namedEntityDetections.putAll(documentId, namedEntityDetections);
  }

  public Set<NamedEntityDetectionDto> getNamedEntityDetections(final long documentId) {
    return namedEntityDetections.get(documentId);
  }

  public Set<NamedEntityDetectionDto> getAllNamedEntityDetections() {
    return Sets.newHashSet(namedEntityDetections.values());
  }

}
