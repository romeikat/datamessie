package com.romeikat.datamessie.core.base.query;

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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCleanedContent;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCrawling;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject2Source;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource2SourceType;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSourceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.query.document.DocumentFilterSettingsQuery;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.function.EntityWithIdToIdFunction;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.Project2Source;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project2SourceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceTypeImpl;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentFilterSettingsQueryTest extends AbstractDbSetupBasedTest {

  @Autowired
  private SharedBeanProvider sharedBeanProvider;

  @Override
  protected Operation initDb() {
    final SourceType sourceType1 = new SourceTypeImpl(1, "SourceType1");
    final SourceType sourceType2 = new SourceTypeImpl(2, "SourceType2");
    final Source source11 = new SourceImpl(11, "Source11", "http://www.source11.de/", true, false);
    final Source source12 = new SourceImpl(12, "Source12", "http://www.source12.de/", false, false);
    final Source source21 = new SourceImpl(21, "Source21", "http://www.source21.de/", true, false);
    final Source source22 = new SourceImpl(22, "Source22", "http://www.source22.de/", false, false);
    final Source2SourceType source2SourceType111 = new Source2SourceType(11, 1);
    final Source2SourceType source2SourceType211 = new Source2SourceType(21, 1);
    final Source2SourceType source2SourceType122 = new Source2SourceType(12, 2);
    final Source2SourceType source2SourceType222 = new Source2SourceType(22, 2);
    final Project project1 = new ProjectImpl(1, "Project1", false, false);
    final Project project2 = new ProjectImpl(2, "Project2", false, false);
    final Project2Source project2Source111 = new Project2SourceImpl(1, 11);
    final Project2Source project2Source112 = new Project2SourceImpl(1, 12);
    final Project2Source project2Source221 = new Project2SourceImpl(2, 21);
    final Project2Source project2Source222 = new Project2SourceImpl(2, 22);
    final Crawling crawling11 = new Crawling(11, project1.getId());
    final Crawling crawling12 = new Crawling(12, project1.getId());
    final Crawling crawling21 = new Crawling(21, project2.getId());
    final Crawling crawling22 = new Crawling(22, project2.getId());
    final Document document11 = new Document(11, crawling11.getId(), source11.getId())
        .setPublished(LocalDateTime.of(2015, 9, 30, 12, 0))
        .setDownloaded(LocalDateTime.of(2015, 9, 30, 12, 0))
        .setState(DocumentProcessingState.DOWNLOADED);
    final Document document12 = new Document(12, crawling12.getId(), source11.getId())
        .setPublished(LocalDateTime.of(2015, 9, 30, 13, 0))
        .setDownloaded(LocalDateTime.of(2015, 9, 30, 13, 0))
        .setState(DocumentProcessingState.DOWNLOAD_ERROR);
    final Document document13 = new Document(13, crawling11.getId(), source12.getId())
        .setPublished(LocalDateTime.of(2015, 9, 30, 14, 0))
        .setDownloaded(LocalDateTime.of(2015, 9, 30, 14, 0))
        .setState(DocumentProcessingState.DOWNLOADED);
    final Document document14 = new Document(14, crawling12.getId(), source12.getId())
        .setPublished(LocalDateTime.of(2015, 9, 30, 15, 0))
        .setDownloaded(LocalDateTime.of(2015, 9, 30, 15, 0))
        .setState(DocumentProcessingState.DOWNLOAD_ERROR);
    final Document document21 = new Document(21, crawling21.getId(), source21.getId())
        .setPublished(LocalDateTime.of(2015, 10, 1, 12, 0))
        .setDownloaded(LocalDateTime.of(2015, 10, 1, 12, 0))
        .setState(DocumentProcessingState.DOWNLOADED);
    final Document document22 = new Document(22, crawling22.getId(), source21.getId())
        .setPublished(LocalDateTime.of(2015, 10, 1, 13, 0))
        .setDownloaded(LocalDateTime.of(2015, 10, 1, 13, 0))
        .setState(DocumentProcessingState.DOWNLOAD_ERROR);
    final Document document23 = new Document(23, crawling21.getId(), source22.getId())
        .setPublished(LocalDateTime.of(2015, 10, 1, 14, 0))
        .setDownloaded(LocalDateTime.of(2015, 10, 1, 14, 0))
        .setState(DocumentProcessingState.DOWNLOADED);
    final Document document24 = new Document(24, crawling22.getId(), source22.getId())
        .setPublished(LocalDateTime.of(2015, 10, 1, 15, 0))
        .setDownloaded(LocalDateTime.of(2015, 10, 1, 15, 0))
        .setState(DocumentProcessingState.DOWNLOAD_ERROR);
    final CleanedContent cleanedContent11 = new CleanedContent(document11.getId(), "Foo is good");
    final CleanedContent cleanedContent13 = new CleanedContent(document13.getId(), "Bar is better");
    final CleanedContent cleanedContent21 = new CleanedContent(document21.getId(), "Foo rocks");
    final CleanedContent cleanedContent23 = new CleanedContent(document23.getId(), "Bar hurts");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        // Source types 1, 2
        insertIntoSourceType(sourceType1), insertIntoSourceType(sourceType2),

        // Sources 11, 12, 21, 22
        insertIntoSource(source11), insertIntoSource(source12), insertIntoSource(source21),
        insertIntoSource(source22),
        // Sources 11, 21 -> Source type 1
        insertIntoSource2SourceType(source2SourceType111),
        insertIntoSource2SourceType(source2SourceType211),
        // Sources 12, 22 -> Source type 2
        insertIntoSource2SourceType(source2SourceType122),
        insertIntoSource2SourceType(source2SourceType222),

        // Project 1
        insertIntoProject(project1),
        // Project 1 -> Sources 11, 12
        insertIntoProject2Source(project2Source111), insertIntoProject2Source(project2Source112),
        // Crawlings 11, 12
        insertIntoCrawling(crawling11), insertIntoCrawling(crawling12),
        // Documents 11, 12, 13, 14
        insertIntoDocument(document11), insertIntoDocument(document12),
        insertIntoDocument(document13), insertIntoDocument(document14),
        // Contents 11, 13
        insertIntoCleanedContent(cleanedContent11), insertIntoCleanedContent(cleanedContent13),

        // Project 2
        insertIntoProject(project2),
        // Project 2 -> Sources 21, 22
        insertIntoProject2Source(project2Source221), insertIntoProject2Source(project2Source222),
        // Crawlings 21, 22
        insertIntoCrawling(crawling21), insertIntoCrawling(crawling22),
        // Documents 21, 22, 23, 24
        insertIntoDocument(document21), insertIntoDocument(document22),
        insertIntoDocument(document23), insertIntoDocument(document24),
        // Contents 21, 23
        insertIntoCleanedContent(cleanedContent21), insertIntoCleanedContent(cleanedContent23));
  }

  // Queries for Documents

  @Test
  public void list_allDocuments() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings();
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(8, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByProjectId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setProjectId(1l);
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsBySourceId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setSourceId(11l);
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsBySourceVisible() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setSourceVisible(true);
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsBySourceTypeIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setSourceTypeIds(Sets.newHashSet(1l));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByCrawlingId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCrawlingId(11l);
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByFromDate() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setFromDate(LocalDate.of(2015, 10, 1));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByToDate() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setToDate(LocalDate.of(2015, 9, 30));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByCleanedContent() {
    rebuildDataMessieIndex();

    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCleanedContent("foo");
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByState() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setState(DocumentProcessingState.DOWNLOADED);
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByDocumentIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l, 12l));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByCleanedContentAndDocumentIds() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCleanedContent("foo")
        .setDocumentIds(Lists.newArrayList(11l, 13l, 21l));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    final Collection<Long> resultIds =
        Collections2.transform(result, new EntityWithIdToIdFunction());
    final Collection<Long> expected = Lists.newArrayList(11l, 21l);
    assertTrue(CollectionUtils.isEqualCollection(expected, resultIds));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_documentsByAllFilters() {
    rebuildDataMessieIndex();

    final DocumentsFilterSettings dfs = new DocumentsFilterSettings(1l, 11l, true,
        Sets.newHashSet(1l, 2l), 11l, LocalDate.of(2015, 9, 30), LocalDate.of(2015, 10, 1),
        "good is foo", DocumentProcessingState.getAllStates(), Lists.newArrayList(11l));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final List<Document> result = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(1, result.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueResult_documentsByDocumentId() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l));
    final DocumentFilterSettingsQuery<Document> query =
        new DocumentFilterSettingsQuery<Document>(dfs, Document.class, sharedBeanProvider);

    final Document result = query.uniqueObject(sessionProvider.getStatelessSession());
    assertEquals(11l, result.getId());

    dbSetupTracker.skipNextLaunch();
  }

  // Queries for Crawlings

  @Test
  public void list_crawlingIdsByDocumentIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l, 21l));
    final DocumentFilterSettingsQuery<Crawling> query =
        new DocumentFilterSettingsQuery<Crawling>(dfs, Crawling.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(21l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_crawlingIdsBySourceId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setSourceId(11l);
    final DocumentFilterSettingsQuery<Crawling> query =
        new DocumentFilterSettingsQuery<Crawling>(dfs, Crawling.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(12l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_crawlingIdsBySourceTypeId() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setSourceTypeIds(Sets.newHashSet(1l));
    final DocumentFilterSettingsQuery<Crawling> query =
        new DocumentFilterSettingsQuery<Crawling>(dfs, Crawling.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(4, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(12l));
    assertTrue(result.contains(21l));
    assertTrue(result.contains(22l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_crawlingIdsByProjectId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setProjectId(1l);
    final DocumentFilterSettingsQuery<Crawling> query =
        new DocumentFilterSettingsQuery<Crawling>(dfs, Crawling.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(12l));

    dbSetupTracker.skipNextLaunch();
  }

  // Queries for Sources

  @Test
  public void list_sourceIdsByDocumentIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l, 21l));
    final DocumentFilterSettingsQuery<Source> query =
        new DocumentFilterSettingsQuery<Source>(dfs, SourceImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(21l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceIdsByCrawlingId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCrawlingId(11l);
    final DocumentFilterSettingsQuery<Source> query =
        new DocumentFilterSettingsQuery<Source>(dfs, SourceImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(12l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceIdsBySourceTypeIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setSourceTypeIds(Sets.newHashSet(1l));
    final DocumentFilterSettingsQuery<Source> query =
        new DocumentFilterSettingsQuery<Source>(dfs, SourceImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(21l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceIdsByProjectId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setProjectId(1l);
    final DocumentFilterSettingsQuery<Source> query =
        new DocumentFilterSettingsQuery<Source>(dfs, SourceImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(11l));
    assertTrue(result.contains(12l));

    dbSetupTracker.skipNextLaunch();
  }

  // Queries for SourceTypes

  @Test
  public void list_sourceTypeIdsByDocumentIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l, 21l));
    final DocumentFilterSettingsQuery<SourceType> query =
        new DocumentFilterSettingsQuery<SourceType>(dfs, SourceTypeImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(1, result.size());
    assertTrue(result.contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceTypeIdsByCrawlingId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCrawlingId(11l);
    final DocumentFilterSettingsQuery<SourceType> query =
        new DocumentFilterSettingsQuery<SourceType>(dfs, SourceTypeImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(1l));
    assertTrue(result.contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceTypeIdsBySourceIds() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setSourceId(11l);
    final DocumentFilterSettingsQuery<SourceType> query =
        new DocumentFilterSettingsQuery<SourceType>(dfs, SourceTypeImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(1, result.size());
    assertTrue(result.contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_sourceTypeIdsByProjectId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setProjectId(1l);
    final DocumentFilterSettingsQuery<SourceType> query =
        new DocumentFilterSettingsQuery<SourceType>(dfs, SourceTypeImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(1l));
    assertTrue(result.contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

  // Queries for Projects

  @Test
  public void list_projectIdsByDocumentIds() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setDocumentIds(Lists.newArrayList(11l, 21l));
    final DocumentFilterSettingsQuery<Project> query =
        new DocumentFilterSettingsQuery<Project>(dfs, ProjectImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(1l));
    assertTrue(result.contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_projectIdsByCrawlingId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setCrawlingId(11l);
    final DocumentFilterSettingsQuery<Project> query =
        new DocumentFilterSettingsQuery<Project>(dfs, ProjectImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(1, result.size());
    assertTrue(result.contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_projectIdsBySourceId() {
    final DocumentsFilterSettings dfs = new DocumentsFilterSettings().setSourceId(11l);
    final DocumentFilterSettingsQuery<Project> query =
        new DocumentFilterSettingsQuery<Project>(dfs, ProjectImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(1, result.size());
    assertTrue(result.contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void list_projectIdsBySourceTypeId() {
    final DocumentsFilterSettings dfs =
        new DocumentsFilterSettings().setSourceTypeIds(Sets.newHashSet(1l));
    final DocumentFilterSettingsQuery<Project> query =
        new DocumentFilterSettingsQuery<Project>(dfs, ProjectImpl.class, sharedBeanProvider);

    final List<Long> result = query.listIds(sessionProvider.getStatelessSession());
    assertEquals(2, result.size());
    assertTrue(result.contains(1l));
    assertTrue(result.contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

}
