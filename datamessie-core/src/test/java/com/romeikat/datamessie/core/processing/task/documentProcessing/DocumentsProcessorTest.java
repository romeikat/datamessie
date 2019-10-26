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
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCleanedContent;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCrawling;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDownload;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoNamedEntity;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoNamedEntityOccurrence;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoRawContent;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoStemmedContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
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
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.Crawling;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.CrawlingImpl;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.RawContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
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

public class DocumentsProcessorTest extends AbstractDbSetupBasedTest {


  private static final String URL_1 = "http://www.document1.de/";
  private static final String URL_2 = "http://www.document2.de/";
  private static final String REDIRECTED_URL = "http://www.this_is_a_redirected_url.com/";
  private static final String REDIRECTED_RAW_CONTENT = "This is a redirected raw content";
  private static final LocalDateTime REDIRECTED_DOWNLOADED = LocalDateTime.now();
  private static final Integer REDIRECTED_STATUS_CODE = 200;

  private static final String CLEANED_CONTENT = "This is a cleaned content";

  private static final String STEMMED_TITLE = "This is a stemmed title";
  private static final String STEMMED_DESCRIPTION = "This is a stemmed description";
  private static final String STEMMED_CONTENT = "This is a stemmed content";

  private static final String NAMED_ENTITY_NAME = "This is a named entity";
  private static final String NAMED_ENTITY_CATEGORY_NAME = "This is a named entity category";

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
  private RawContentDao rawContentDao;

  @Autowired
  private CleanedContentDao cleanedContentDao;

  @Autowired
  private StemmedContentDao stemmedContentDao;

  @Autowired
  @Qualifier("processingDocumentDao")
  private DocumentDao documentDao;

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


  @Override
  protected Operation initDb() {
    final Project project1 = new ProjectImpl(1, "Project1", false, false);
    final Source source1 = new SourceImpl(1, "Source1", "http://www.source1.de/", true, false);
    final Crawling crawling1 = new CrawlingImpl(1, project1.getId());
    final NamedEntity namedEntity = new NamedEntity(1, "Outdated NamedEntity");
    final LocalDateTime now = LocalDateTime.now();
    // Document1 with download success
    final LocalDateTime published1 = now.minusDays(1);
    final Document document1 = new DocumentImpl(1, crawling1.getId(), source1.getId())
        .setTitle("Title1").setUrl(URL_1).setDescription("Description1").setPublished(published1)
        .setDownloaded(now).setState(DocumentProcessingState.DOWNLOADED).setStatusCode(200);
    final RawContent rawContent1 = new RawContentImpl(document1.getId(), "RawContent1");
    final CleanedContent cleanedContent1 =
        new CleanedContentImpl(document1.getId(), "Outdated CleanedContent1");
    final StemmedContent stemmedContent1 =
        new StemmedContentImpl(document1.getId(), "Outdated StemmedContent1");
    final NamedEntityOccurrence namedEntityOccurrence1 = new NamedEntityOccurrence(1,
        namedEntity.getId(), namedEntity.getId(), NamedEntityType.MISC, 1, document1.getId());
    final Download download1 =
        new Download(1, source1.getId(), document1.getId(), true).setUrl(URL_1);
    // Document2 with failed download
    final LocalDateTime published2 = now.minusDays(2);
    final Document document2 = new DocumentImpl(2, crawling1.getId(), source1.getId())
        .setTitle("Title2").setUrl(URL_2).setDescription("Description2").setPublished(published2)
        .setDownloaded(now).setState(DocumentProcessingState.DOWNLOAD_ERROR).setStatusCode(400);
    final RawContent rawContent2 = new RawContentImpl(document2.getId(), "Outdated RawContent2");
    final CleanedContent cleanedContent2 =
        new CleanedContentImpl(document2.getId(), "Outdated CleanedContent2");
    final StemmedContent stemmedContent2 =
        new StemmedContentImpl(document2.getId(), "Outdated StemmedContent3");
    final NamedEntityOccurrence namedEntityOccurrence2 = new NamedEntityOccurrence(2,
        namedEntity.getId(), namedEntity.getId(), NamedEntityType.MISC, 1, document2.getId());
    final Download download2 =
        new Download(2, source1.getId(), document2.getId(), false).setUrl(URL_2);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        sequenceOf(insertIntoProject(project1), insertIntoSource(source1),
            insertIntoCrawling(crawling1), insertIntoNamedEntity(namedEntity),
            insertIntoDocument(document1), insertIntoRawContent(rawContent1),
            insertIntoCleanedContent(cleanedContent1), insertIntoStemmedContent(stemmedContent1),
            insertIntoNamedEntityOccurrence(namedEntityOccurrence1), insertIntoDocument(document2),
            insertIntoRawContent(rawContent2), insertIntoCleanedContent(cleanedContent2),
            insertIntoStemmedContent(stemmedContent2),
            insertIntoNamedEntityOccurrence(namedEntityOccurrence2), insertIntoDownload(download1),
            insertIntoDownload(download2)));
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    // Simulate successful redirecting
    final DocumentRedirectingResult expectedDocumentRedirectingResult =
        new DocumentRedirectingResult(REDIRECTED_URL, new DownloadResult(null, REDIRECTED_URL,
            REDIRECTED_RAW_CONTENT, REDIRECTED_DOWNLOADED, REDIRECTED_STATUS_CODE));
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
  public void processDocument() {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing was successful (with redirection):
    // state becomes STEMMED
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues =
        new DocumentAndContentValues(oldValues.getTitle(), STEMMED_TITLE, REDIRECTED_URL,
            oldValues.getDescription(), STEMMED_DESCRIPTION, oldValues.getPublished(),
            REDIRECTED_DOWNLOADED, DocumentProcessingState.STEMMED, REDIRECTED_STATUS_CODE,
            REDIRECTED_RAW_CONTENT, CLEANED_CONTENT, STEMMED_CONTENT);
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);

    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(1, namedEntityOccurrences.size());
    final NamedEntityOccurrence namedEntityOccurrence = namedEntityOccurrences.iterator().next();
    NamedEntity namedEntity = namedEntityDao.getEntity(sessionProvider.getStatelessSession(),
        namedEntityOccurrence.getNamedEntityId());
    assertEquals(NAMED_ENTITY_NAME, namedEntity.getName());

    final List<NamedEntityCategory> namedEntityCategories =
        namedEntityCategoryDao.getByNamedEntity(sessionProvider.getStatelessSession(), namedEntity);
    assertEquals(1, namedEntityCategories.size());
    final NamedEntityCategory namedEntityCategory = namedEntityCategories.iterator().next();
    namedEntity = namedEntityDao.getEntity(sessionProvider.getStatelessSession(),
        namedEntityCategory.getNamedEntityId());
    final NamedEntity categoryNamedEntity = namedEntityDao.getEntity(
        sessionProvider.getStatelessSession(), namedEntityCategory.getCategoryNamedEntityId());
    assertEquals(NAMED_ENTITY_NAME, namedEntity.getName());
    assertEquals(NAMED_ENTITY_CATEGORY_NAME, categoryNamedEntity.getName());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(2, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());
    final Download redurectedDownload1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), REDIRECTED_URL, 1);
    assertNotNull(redurectedDownload1);
    assertEquals(1, redurectedDownload1.getDocumentId());
    assertTrue(redurectedDownload1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_downloadError() throws Exception {
    Document document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    RawContent rawContent2 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    CleanedContent cleanedContent2 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    StemmedContent stemmedContent2 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document2, rawContent2, cleanedContent2, stemmedContent2);

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document2));

    // Processing failed:
    // state becomes DOWNLOAD_ERROR; raw content remains unchanged, cleaned and stemmed contents
    // become empty
    document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    rawContent2 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    cleanedContent2 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    stemmedContent2 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    final DocumentAndContentValues expectedValues = new DocumentAndContentValues(
        oldValues.getTitle(), oldValues.getStemmedTitle(), oldValues.getUrl(),
        oldValues.getDescription(), oldValues.getStemmedDescription(), oldValues.getPublished(),
        oldValues.getDownloaded(), DocumentProcessingState.DOWNLOAD_ERROR,
        oldValues.getStatusCode(), oldValues.getRawContent(), "", "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document2, rawContent2, cleanedContent2, stemmedContent2);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 2);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 2);
    assertEquals(1, downloads.size());
    final Download download2 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_2, 1);
    assertNotNull(download2);
    assertEquals(2, download2.getDocumentId());
    assertFalse(download2.getSuccess());

    // Statistics are not rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document2.getSourceId(), document2.getPublishedDate()));
  }

  @Test
  public void processDocument_noRedirectionFound() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate absent redirection
    final DocumentRedirectingResult expectedDocumentRedirectingResult =
        new DocumentRedirectingResult(null, null);
    doReturn(expectedDocumentRedirectingResult).when(redirectCallback).redirect(any(Document.class),
        any(RawContent.class), anyListOf(RedirectingRule.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing was successful (without redirection):
    // state becomes STEMMED
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues =
        new DocumentAndContentValues(oldValues.getTitle(), STEMMED_TITLE, oldValues.getUrl(),
            oldValues.getDescription(), STEMMED_DESCRIPTION, oldValues.getPublished(),
            oldValues.getDownloaded(), DocumentProcessingState.STEMMED, oldValues.getStatusCode(),
            oldValues.getRawContent(), CLEANED_CONTENT, STEMMED_CONTENT);
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(1, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(1, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_redirectedDownloadError() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate error
    final DocumentRedirectingResult expectedDocumentRedirectingResult =
        new DocumentRedirectingResult(REDIRECTED_URL,
            new DownloadResult(document1.getUrl(), REDIRECTED_URL, null, LocalDateTime.now(), 503));
    doReturn(expectedDocumentRedirectingResult).when(redirectCallback).redirect(any(Document.class),
        any(RawContent.class), anyListOf(RedirectingRule.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing failed:
    // state becomes REDIRECTING_ERROR; cleaned and stemmed contents become empty; other fields
    // remain unmodified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues = new DocumentAndContentValues(
        oldValues.getTitle(), oldValues.getStemmedTitle(), oldValues.getUrl(),
        oldValues.getDescription(), oldValues.getStemmedDescription(), oldValues.getPublished(),
        oldValues.getDownloaded(), DocumentProcessingState.REDIRECTING_ERROR,
        oldValues.getStatusCode(), oldValues.getRawContent(), "", "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(2, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());
    final Download redurectedDownload1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), REDIRECTED_URL, 1);
    assertNotNull(redurectedDownload1);
    assertEquals(1, redurectedDownload1.getDocumentId());
    assertFalse(redurectedDownload1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_redirectingException() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate exception
    doThrow(Exception.class).when(redirectCallback).redirect(any(Document.class),
        any(RawContent.class), anyListOf(RedirectingRule.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing failed:
    // state becomes TECHNICAL_ERROR; cleaned and stemmed contents become empty; other fields remain
    // unmodified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues = new DocumentAndContentValues(
        oldValues.getTitle(), oldValues.getStemmedTitle(), oldValues.getUrl(),
        oldValues.getDescription(), oldValues.getStemmedDescription(), oldValues.getPublished(),
        oldValues.getDownloaded(), DocumentProcessingState.TECHNICAL_ERROR,
        oldValues.getStatusCode(), oldValues.getRawContent(), "", "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(1, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_cleaningError() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate error
    final DocumentCleaningResult expectedDocumentCleaningResult = new DocumentCleaningResult(null);
    doReturn(expectedDocumentCleaningResult).when(cleanCallback).clean(any(Document.class),
        any(RawContent.class), anyListOf(TagSelectingRule.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing failed:
    // state becomes CLEANING_ERROR; redirection succeeds, cleaned and stemmed contents become
    // empty;
    // other fields remain unmodified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues =
        new DocumentAndContentValues(oldValues.getTitle(), oldValues.getStemmedTitle(),
            REDIRECTED_URL, oldValues.getDescription(), oldValues.getStemmedDescription(),
            oldValues.getPublished(), REDIRECTED_DOWNLOADED, DocumentProcessingState.CLEANING_ERROR,
            REDIRECTED_STATUS_CODE, REDIRECTED_RAW_CONTENT, "", "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(2, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());
    final Download redurectedDownload1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), REDIRECTED_URL, 1);
    assertNotNull(redurectedDownload1);
    assertEquals(1, redurectedDownload1.getDocumentId());
    assertTrue(redurectedDownload1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_cleaningException() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate exception
    doThrow(Exception.class).when(cleanCallback).clean(any(Document.class), any(RawContent.class),
        anyListOf(TagSelectingRule.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing failed:
    // state becomes TECHNICAL_ERROR; redirection succeeds; cleaned and stemmed contents become
    // empty; other fields remain unmodified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues = new DocumentAndContentValues(
        oldValues.getTitle(), oldValues.getStemmedTitle(), REDIRECTED_URL,
        oldValues.getDescription(), oldValues.getStemmedDescription(), oldValues.getPublished(),
        REDIRECTED_DOWNLOADED, DocumentProcessingState.TECHNICAL_ERROR, oldValues.getStatusCode(),
        REDIRECTED_RAW_CONTENT, "", "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(2, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());
    final Download redurectedDownload1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), REDIRECTED_URL, 1);
    assertNotNull(redurectedDownload1);
    assertEquals(1, redurectedDownload1.getDocumentId());
    assertTrue(redurectedDownload1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void processDocument_stemmingException() throws Exception {
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    CleanedContent cleanedContent1 =
        cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    StemmedContent stemmedContent1 =
        stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues oldValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);

    // Simulate exception
    doThrow(Exception.class).when(stemCallback).stem(any(Document.class), any(String.class),
        any(Language.class));

    // Process
    documentsProcessor.processDocuments(Lists.newArrayList(document1));

    // Processing failed:
    // state becomes TECHNICAL_ERROR; redirection succeeds; cleaning succeeds, stemmed content
    // becomes empty; other fields remain unmodified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    cleanedContent1 = cleanedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    stemmedContent1 = stemmedContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final DocumentAndContentValues expectedValues = new DocumentAndContentValues(
        oldValues.getTitle(), oldValues.getStemmedTitle(), REDIRECTED_URL,
        oldValues.getDescription(), oldValues.getStemmedDescription(), oldValues.getPublished(),
        REDIRECTED_DOWNLOADED, DocumentProcessingState.TECHNICAL_ERROR, oldValues.getStatusCode(),
        REDIRECTED_RAW_CONTENT, CLEANED_CONTENT, "");
    final DocumentAndContentValues actualValues =
        new DocumentAndContentValues(document1, rawContent1, cleanedContent1, stemmedContent1);
    assertEquals(expectedValues, actualValues);
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(0, namedEntityOccurrences.size());

    final List<Download> downloads =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), 1);
    assertEquals(2, downloads.size());
    final Download download1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), URL_1, 1);
    assertNotNull(download1);
    assertEquals(1, download1.getDocumentId());
    assertTrue(download1.getSuccess());
    final Download redurectedDownload1 =
        downloadDao.getForUrlAndSource(sessionProvider.getStatelessSession(), REDIRECTED_URL, 1);
    assertNotNull(redurectedDownload1);
    assertEquals(1, redurectedDownload1.getDocumentId());
    assertTrue(redurectedDownload1.getSuccess());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsProcessor.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

}
