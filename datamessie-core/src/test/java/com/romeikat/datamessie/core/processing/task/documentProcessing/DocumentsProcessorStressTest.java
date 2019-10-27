package com.romeikat.datamessie.core.processing.task.documentProcessing;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.StemmedContentDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.Crawling;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.Download;
import com.romeikat.datamessie.core.domain.entity.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.TagSelectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.CrawlingImpl;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.DownloadImpl;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityImpl;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.RawContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContentImpl;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.CleanCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.ProvideNamedEntityCategoryTitlesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.RedirectCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.StemCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaningResult;
import com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting.DocumentRedirectingResult;
import com.romeikat.datamessie.core.processing.task.documentProcessing.stemming.DocumentStemmingResult;

public class DocumentsProcessorStressTest extends AbstractDbSetupBasedTest {

  private static final String CLEANED_CONTENT = "This is a cleaned content";

  private static final String STEMMED_TITLE = "This is a stemmed title";
  private static final String STEMMED_DESCRIPTION = "This is a stemmed description";
  private static final String STEMMED_CONTENT = "This is a stemmed content";

  private static final String NAMED_ENTITY_NAME = "This is a named entity";
  private static final String NAMED_ENTITY_CATEGORY_NAME = "This is a named entity category";

  private static final int NUMBER_OF_DOCUMENTS = 1234;

  @Value("${documents.processing.parallelism.factor}")
  private Double documentsParallelismFactor;


  private DocumentsProcessor documentsProcessor;


  @Mock
  private RedirectCallback redirectCallback;

  @Mock
  private CleanCallback cleanCallback;

  @Mock
  private StemCallback stemCallback;

  @Mock
  private ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback;


  @Autowired
  @Qualifier("processingDocumentDao")
  private DocumentDao documentDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private StemmedContentDao stemmedContentDao;

  @Autowired
  private DownloadDao downloadDao;

  @Autowired
  private NamedEntityDao namedEntityDao;

  @Autowired
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Autowired
  private ApplicationContext ctx;


  private static List<Integer> getDocumentIds() {
    final List<Integer> documentIds = Lists.newArrayListWithExpectedSize(NUMBER_OF_DOCUMENTS);
    for (int documentId = 1; documentId <= NUMBER_OF_DOCUMENTS; documentId++) {
      documentIds.add(documentId);
    }
    return documentIds;
  }

  @Override
  protected Operation initDb() {
    final Project project1 = new ProjectImpl(1, "Project1", false, false);
    final Source source1 = new SourceImpl(1, "Source1", "http://www.source1.de/", true, false);
    final Crawling crawling1 = new CrawlingImpl(1, project1.getId());
    final NamedEntity namedEntity = new NamedEntityImpl(1, "NamedEntity");

    // Project, source, and crawling
    final Operation initBaseData =
        sequenceOf(sequenceOf(CommonOperations.insertIntoProject(project1),
            CommonOperations.insertIntoSource(source1),
            CommonOperations.insertIntoCrawling(crawling1),
            CommonOperations.insertIntoNamedEntity(namedEntity)));

    // Document for each ID
    final List<Operation> operations = Lists.newArrayListWithExpectedSize(5 * NUMBER_OF_DOCUMENTS);
    for (final int documentId : getDocumentIds()) {
      final String url = "http://www.document" + documentId + ".de/";
      final Document document = new DocumentImpl(documentId, crawling1.getId(), source1.getId())
          .setTitle("Title" + documentId).setUrl(url).setDescription("Description" + documentId)
          .setPublished(LocalDateTime.now()).setDownloaded(LocalDateTime.now())
          .setState(DocumentProcessingState.DOWNLOADED).setStatusCode(200);
      final RawContent rawContent = new RawContentImpl(documentId, "RawContent" + documentId);
      final CleanedContent cleanedContent =
          new CleanedContentImpl(documentId, "Outdated CleanedContent" + documentId);
      final StemmedContent stemmedContent =
          new StemmedContentImpl(documentId, "Outdated StemmedContent" + documentId);
      final NamedEntityOccurrence namedEntityOccurrence = new NamedEntityOccurrence(documentId,
          namedEntity.getId(), namedEntity.getId(), NamedEntityType.MISC, 1, document.getId());
      final Download download =
          new DownloadImpl(documentId, source1.getId(), documentId, true).setUrl(url);

      operations.add(CommonOperations.insertIntoDocument(document));
      operations.add(CommonOperations.insertIntoRawContent(rawContent));
      operations.add(CommonOperations.insertIntoCleanedContent(cleanedContent));
      operations.add(CommonOperations.insertIntoStemmedContent(stemmedContent));
      operations.add(CommonOperations.insertIntoNamedEntityOccurrence(namedEntityOccurrence));
      operations.add(CommonOperations.insertIntoDownload(download));
    }
    final Operation initDocuments = sequenceOf(operations);

    // Done
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, initBaseData, initDocuments);
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    // Simulate absent redirection
    final DocumentRedirectingResult expectedDocumentRedirectingResult =
        new DocumentRedirectingResult(null, null);
    doReturn(expectedDocumentRedirectingResult).when(redirectCallback).redirect(any(Document.class),
        any(RawContent.class), anyListOf(RedirectingRule.class));
    // Simulate successful cleaning
    final DocumentCleaningResult expectedDocumentCleaningResult =
        new DocumentCleaningResult(CLEANED_CONTENT);
    doReturn(expectedDocumentCleaningResult).when(cleanCallback).clean(any(Document.class),
        any(RawContent.class), anyListOf(TagSelectingRule.class));
    // Simulate successful stemming
    final NamedEntityDetectionDto namedEntityDetection =
        new NamedEntityDetectionDto().setName(NAMED_ENTITY_NAME).setParentName(NAMED_ENTITY_NAME)
            .setType(NamedEntityType.MISC).setQuantity(1);
    final DocumentStemmingResult expectedDocumentStemmingResult =
        new DocumentStemmingResult(STEMMED_TITLE, STEMMED_DESCRIPTION, STEMMED_CONTENT,
            Lists.newArrayList(namedEntityDetection));
    doReturn(expectedDocumentStemmingResult).when(stemCallback).stem(any(Document.class),
        any(String.class), any(Language.class));
    // Simulate named entity categories
    final List<String> namedEntityCategories = Lists.newArrayListWithExpectedSize(1);
    namedEntityCategories.add(NAMED_ENTITY_CATEGORY_NAME);
    doReturn(namedEntityCategories).when(provideNamedEntityCategoryTitlesCallback)
        .provideCategoryTitles(any(String.class));

    documentsProcessor = new DocumentsProcessor(redirectCallback, downloadDao::getIdsWithEntities,
        documentDao::getIdsWithEntities, downloadDao::getDocumentIdsForUrlsAndSource, cleanCallback,
        stemCallback, namedEntityDao::getOrCreate, namedEntityCategoryDao::getWithoutCategories,
        provideNamedEntityCategoryTitlesCallback,
        (documentsToBeUpdated, downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated,
            cleanedContentsToBeCreatedOrUpdated, stemmedContentsToBeCreatedOrUpdated,
            namedEntityOccurrencesToBeReplaced, namedEntityCategoriesToBeSaved) -> documentDao
                .persistDocumentsProcessingOutput(sessionProvider.getStatelessSession(),
                    documentsToBeUpdated, downloadsToBeCreatedOrUpdated, rawContentsToBeUpdated,
                    cleanedContentsToBeCreatedOrUpdated, stemmedContentsToBeCreatedOrUpdated,
                    namedEntityOccurrencesToBeReplaced, namedEntityCategoriesToBeSaved),
        ctx);
  }

  @Test
  public void processDocuments() throws TaskCancelledException {
    final Map<Integer, DocumentAndContentValues> oldValuesPerId =
        Maps.newHashMapWithExpectedSize(NUMBER_OF_DOCUMENTS);
    for (final int documentId : getDocumentIds()) {
      final Document document =
          documentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final RawContent rawContent =
          rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final CleanedContent cleanedContent =
          cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final StemmedContent stemmedContent =
          stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final DocumentAndContentValues oldValues =
          new DocumentAndContentValues(document, rawContent, cleanedContent, stemmedContent);
      oldValuesPerId.put(documentId, oldValues);
    }

    // Process
    final List<Document> documents =
        documentDao.getAllEntites(sessionProvider.getStatelessSession());
    documentsProcessor.processDocuments(documents);

    // Processing was successful:
    // state becomes STEMMED
    for (final int documentId : getDocumentIds()) {
      final Document document =
          documentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final RawContent rawContent =
          rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final CleanedContent cleanedContent =
          cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final StemmedContent stemmedContent =
          stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), documentId);
      final DocumentAndContentValues oldValues = oldValuesPerId.get(documentId);
      final DocumentAndContentValues expectedValues =
          new DocumentAndContentValues(oldValues.getTitle(), STEMMED_TITLE, oldValues.getUrl(),
              oldValues.getDescription(), STEMMED_DESCRIPTION, oldValues.getPublished(),
              oldValues.getDownloaded(), DocumentProcessingState.STEMMED, oldValues.getStatusCode(),
              oldValues.getRawContent(), CLEANED_CONTENT, STEMMED_CONTENT);
      final DocumentAndContentValues actualValues =
          new DocumentAndContentValues(document, rawContent, cleanedContent, stemmedContent);
      assertEquals(expectedValues, actualValues);

      final List<NamedEntityOccurrence> namedEntityOccurrences =
          namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), documentId);
      assertEquals(1, namedEntityOccurrences.size());
      final NamedEntityOccurrence namedEntityOccurrence = namedEntityOccurrences.iterator().next();
      NamedEntity namedEntity = namedEntityDao.getEntity(sessionProvider.getStatelessSession(),
          namedEntityOccurrence.getNamedEntityId());
      assertEquals(NAMED_ENTITY_NAME, namedEntity.getName());

      final List<NamedEntityCategory> namedEntityCategories = namedEntityCategoryDao
          .getByNamedEntity(sessionProvider.getStatelessSession(), namedEntity);
      assertEquals(1, namedEntityCategories.size());
      final NamedEntityCategory namedEntityCategory = namedEntityCategories.iterator().next();
      namedEntity = namedEntityDao.getEntity(sessionProvider.getStatelessSession(),
          namedEntityCategory.getNamedEntityId());
      final NamedEntity categoryNamedEntity = namedEntityDao.getEntity(
          sessionProvider.getStatelessSession(), namedEntityCategory.getCategoryNamedEntityId());
      assertEquals(NAMED_ENTITY_NAME, namedEntity.getName());
      assertEquals(NAMED_ENTITY_CATEGORY_NAME, categoryNamedEntity.getName());

      final List<Download> downloads =
          downloadDao.getForDocument(sessionProvider.getStatelessSession(), documentId);
      assertEquals(1, downloads.size());
      final Download download = downloadDao.getForUrlAndSource(
          sessionProvider.getStatelessSession(), "http://www.document" + documentId + ".de/", 1);
      assertNotNull(download);
      assertEquals(documentId, download.getDocumentId());
      assertTrue(download.getSuccess());

      // Statistics are rebuilt
      final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
          documentsProcessor.getStatisticsToBeRebuilt();
      assertTrue(
          statisticsToBeRebuilt.getValue(document.getSourceId(), document.getPublishedDate()));
    }
  }

}
