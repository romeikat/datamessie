package com.romeikat.datamessie.core.rss.task.rssCrawling;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.LocalDateTime;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.DownloadDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;

public class DocumentCrawlerTest extends AbstractDbSetupBasedTest {

  private DocumentCrawler documentCrawler;

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  private SourceDao sourceDao;

  @Autowired
  private DocumentDao documentDao;

  @Autowired
  private DownloadDao downloadDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Override
  protected Operation initDb() {
    final Project project1 = new Project(1, "Project1", false, false);
    final Source source1 = new Source(1, "Source1", "http://www.source1.de/", null, true, false);
    final Crawling crawling1 = new Crawling(1, project1.getId());
    final LocalDateTime now = LocalDateTime.now();
    // Document1 with download success
    final LocalDateTime published1 = now.minusDays(1);
    final Document document1 =
        new Document(1, crawling1.getId(), source1.getId()).setTitle("Title1")
            .setUrl("http://www.url1.de/").setDescription("Description1").setPublished(published1)
            .setDownloaded(now).setState(DocumentProcessingState.DOWNLOADED).setStatusCode(200);
    final Download download1 = new Download(1, 1, 1, true).setUrl("http://www.url1.de/");
    final Download download2 = new Download(2, 1, 1, true).setUrl("http://www.originalUrl1.de/");
    final RawContent rawContent1 = new RawContent(document1.getId(), "RawContent1");
    // Document2 with failed download
    final LocalDateTime published2 = now.minusDays(2);
    final Document document2 =
        new Document(2, crawling1.getId(), source1.getId()).setTitle("Title2")
            .setUrl("http://www.url2.de/").setDescription("Description2").setPublished(published2)
            .setDownloaded(now).setState(DocumentProcessingState.DOWNLOAD_ERROR).setStatusCode(400);
    final Download download3 = new Download(3, 1, 2, false).setUrl("http://www.url2.de/");
    final Download download4 = new Download(4, 1, 2, false).setUrl("http://www.originalUrl2.de/");
    // Document3 with download success, but redirection error
    final LocalDateTime published3 = now.minusDays(3);
    final Document document3 = new Document(3, crawling1.getId(), source1.getId())
        .setTitle("Title3").setUrl("http://www.url3.de/").setDescription("Description3")
        .setPublished(published3).setDownloaded(now)
        .setState(DocumentProcessingState.REDIRECTING_ERROR).setStatusCode(200);
    final Download download5 = new Download(5, 1, 3, true).setUrl("http://www.url3.de/");
    final Download download6 = new Download(6, 1, 3, true).setUrl("http://www.originalUrl3.de/");
    final Download download7 = new Download(7, 1, 3, false).setUrl("http://www.redirectedUrl3.de/");
    final Download download8 =
        new Download(8, 1, 3, false).setUrl("http://www.redirectedOriginalUrl3.de/");
    // Obsolete content4
    final RawContent rawContent4 = new RawContent(4, "RawContent4");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        sequenceOf(CommonOperations.insertIntoProject(project1),
            CommonOperations.insertIntoSource(source1),
            CommonOperations.insertIntoCrawling(crawling1),
            CommonOperations.insertIntoDocument(document1),
            CommonOperations.insertIntoDocument(document2),
            CommonOperations.insertIntoDocument(document3),
            CommonOperations.insertIntoDownload(download1),
            CommonOperations.insertIntoDownload(download2),
            CommonOperations.insertIntoDownload(download3),
            CommonOperations.insertIntoDownload(download4),
            CommonOperations.insertIntoDownload(download5),
            CommonOperations.insertIntoDownload(download6),
            CommonOperations.insertIntoDownload(download7),
            CommonOperations.insertIntoDownload(download8),
            CommonOperations.insertIntoRawContent(rawContent1),
            CommonOperations.insertIntoRawContent(rawContent4)));
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    documentCrawler = new DocumentCrawler(ctx);
  }

  @Test
  public void performCrawling_newDownloadSuccess() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String titleNew = "titleNew";
    final String originalUrlNew = "http://www.originalUrlNew.de/";
    final String urlNew = "http://www.urlNew.de/";
    final String descriptionNew = "descriptionNew";
    final LocalDateTime publishedNew = LocalDateTime.now();
    final LocalDateTime downloadedNew = LocalDateTime.now();
    final Integer statusCodeNew = 200;
    final String newRawContentNew = "ContentNew-new";
    final DownloadResult downloadResultNew = new DownloadResult(originalUrlNew, urlNew,
        newRawContentNew, null, downloadedNew, statusCodeNew);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), titleNew,
            descriptionNew, publishedNew, downloadResultNew, crawling1.getId(), source1.getId());

    // New document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(4, count);

    // Document metadata is set
    final Document documentNew = documentDao
        .getForUrlAndSource(sessionProvider.getStatelessSession(), urlNew, source1.getId());
    assertEquals(titleNew, documentNew.getTitle());
    assertEquals(urlNew, documentNew.getUrl());
    assertEquals(descriptionNew, documentNew.getDescription());
    assertEquals(publishedNew, documentNew.getPublished());
    assertEquals(downloadedNew, documentNew.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, documentNew.getState());
    assertEquals(statusCodeNew, documentNew.getStatusCode());
    assertEquals(crawling1.getId(), documentNew.getCrawlingId());
    assertEquals(source1.getId(), documentNew.getSourceId());

    // Crawling result corresponds to new document
    assertEquals(result.getId(), documentNew.getId());

    // Downloads are created
    final Collection<Download> downloadsNew =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), documentNew.getId());
    assertEquals(2, downloadsNew.size());
    assertEqDownload(getDownload(downloadsNew, urlNew), urlNew, source1.getId(), true);
    assertEqDownload(getDownload(downloadsNew, originalUrlNew), originalUrlNew, source1.getId(),
        true);

    // Content is created
    final RawContent rawContentNew =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentNew.getId());
    assertEquals(newRawContentNew, rawContentNew.getContent());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(documentNew.getSourceId(), documentNew.getPublishedDate()));
  }

  @Test
  public void performCrawling_newDownloadFailed() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String titleNew = "titleNew";
    final String originalUrlNew = "http://www.originalUrlNew.de/";
    final String urlNew = "http://www.urlNew.de/";
    final String descriptionNew = "descriptionNew";
    final LocalDateTime publishedNew = LocalDateTime.now();
    final LocalDateTime downloadedNew = LocalDateTime.now();
    final Integer statusCodeNew = 400;
    final String newRawContentNew = null;
    final DownloadResult downloadResultNew = new DownloadResult(originalUrlNew, urlNew,
        newRawContentNew, null, downloadedNew, statusCodeNew);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), titleNew,
            descriptionNew, publishedNew, downloadResultNew, crawling1.getId(), source1.getId());

    // New document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(4, count);

    // Document metadata is set
    final Document documentNew = documentDao
        .getForUrlAndSource(sessionProvider.getStatelessSession(), urlNew, source1.getId());
    assertEquals(titleNew, documentNew.getTitle());
    assertEquals(urlNew, documentNew.getUrl());
    assertEquals(descriptionNew, documentNew.getDescription());
    assertEquals(publishedNew, documentNew.getPublished());
    assertEquals(downloadedNew, documentNew.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOAD_ERROR, documentNew.getState());
    assertEquals(statusCodeNew, documentNew.getStatusCode());
    assertEquals(crawling1.getId(), documentNew.getCrawlingId());
    assertEquals(source1.getId(), documentNew.getSourceId());

    // Crawling result corresponds to new document
    assertEquals(result.getId(), documentNew.getId());

    // Downloads are created
    final Collection<Download> downloadsNew =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), documentNew.getId());
    assertEquals(2, downloadsNew.size());
    assertEqDownload(getDownload(downloadsNew, urlNew), urlNew, source1.getId(), false);
    assertEqDownload(getDownload(downloadsNew, originalUrlNew), originalUrlNew, source1.getId(),
        false);

    // No content is created
    final RawContent rawContentNew =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), documentNew.getId());
    assertNull(rawContentNew);

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(documentNew.getSourceId(), documentNew.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadSuccess_repeatedDownloadSuccess() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String originalUrl1Old = "http://www.originalUrl1.de/";
    final String url1Old = "http://www.url1.de/";

    final String title1New = "title1New";
    final String originalUrl1New = "http://www.additionalUrl1.de/";
    final String url1New = url1Old;
    final String description1New = "description1New";
    final LocalDateTime published1New = LocalDateTime.now();
    final LocalDateTime downloaded1New = LocalDateTime.now();
    final Integer statusCode1New = 200;
    final String rawContent1New = "rawContent1New";
    final DownloadResult downloadResult1New = new DownloadResult(originalUrl1New, url1New,
        rawContent1New, null, downloaded1New, statusCode1New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title1New,
            description1New, published1New, downloadResult1New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document metadata is modified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(title1New, document1.getTitle());
    assertEquals(url1New, document1.getUrl());
    assertEquals(description1New, document1.getDescription());
    assertEquals(published1New, document1.getPublished());
    assertEquals(downloaded1New, document1.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, document1.getState());
    assertEquals(statusCode1New, document1.getStatusCode());

    // Crawling result corresponds to existing document
    assertEquals(result.getId(), document1.getId());

    // Additional downloads are created
    final Collection<Download> downloads1 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(3, downloads1.size());
    assertEqDownload(getDownload(downloads1, originalUrl1Old), originalUrl1Old, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url1Old), url1Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads1, originalUrl1New), originalUrl1New, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url1New), url1New, source1.getId(), true);

    // Content is modified
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(rawContent1New, rawContent1.getContent());

    // Statistics are not rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadSuccess_repeatedDownloadFailed() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String title1Old = document1.getTitle();
    final String originalUrl1Old = "http://www.originalUrl1.de/";
    final String url1Old = "http://www.url1.de/";
    final String description1Old = document1.getDescription();
    final LocalDateTime published1Old = document1.getPublished();
    final LocalDateTime downloaded1Old = document1.getDownloaded();
    final Integer statusCode1Old = document1.getStatusCode();
    final String rawContent1Old = rawContent1.getContent();

    final String title1New = "title1New";
    final String originalUrl1New = "http://www.additionalUrl1.de/";
    final String url1New = url1Old;
    final String description1New = "description1New";
    final LocalDateTime published1New = LocalDateTime.now();
    final LocalDateTime downloaded1New = LocalDateTime.now();
    final Integer statusCode1New = 400;
    final String rawContent1New = null;
    final DownloadResult downloadResult1New = new DownloadResult(originalUrl1New, url1New,
        rawContent1New, null, downloaded1New, statusCode1New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title1New,
            description1New, published1New, downloadResult1New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document metadata is not modified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(title1Old, document1.getTitle());
    assertEquals(url1Old, document1.getUrl());
    assertEquals(description1Old, document1.getDescription());
    assertEquals(published1Old, document1.getPublished());
    assertEquals(downloaded1Old, document1.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, document1.getState());
    assertEquals(statusCode1Old, document1.getStatusCode());

    // Crawling result is empty
    assertNull(result);

    // Additional downloads are created
    final Collection<Download> downloads1 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(3, downloads1.size());
    assertEqDownload(getDownload(downloads1, originalUrl1Old), originalUrl1Old, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url1Old), url1Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads1, originalUrl1New), originalUrl1New, source1.getId(),
        false);
    assertEqDownload(getDownload(downloads1, url1New), url1New, source1.getId(), true);

    // Content is not modified
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(rawContent1Old, rawContent1.getContent());

    // Statistics are not rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertNull(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadFailed_repeatedDownloadSuccess() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);

    final String originalUrl2Old = "http://www.originalUrl2.de/";
    final String url2Old = "http://www.url2.de/";

    final String title2New = "title2New";
    final String originalUrl2New = "http://www.additionalUrl2.de/";
    final String url2New = url2Old;
    final String description2New = "description2New";
    final LocalDateTime published2New = LocalDateTime.now();
    final LocalDateTime downloaded2New = LocalDateTime.now();
    final Integer statusCode2New = 200;
    final String rawContent2New = "rawContent2New";
    final DownloadResult downloadResult2New = new DownloadResult(originalUrl2New, url2New,
        rawContent2New, null, downloaded2New, statusCode2New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title2New,
            description2New, published2New, downloadResult2New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document metadata is modified
    document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals(title2New, document2.getTitle());
    assertEquals(url2New, document2.getUrl());
    assertEquals(description2New, document2.getDescription());
    assertEquals(published2New, document2.getPublished());
    assertEquals(downloaded2New, document2.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, document2.getState());
    assertEquals(statusCode2New, document2.getStatusCode());

    // Crawling result corresponds to existing document
    assertEquals(result.getId(), document2.getId());

    // Additional downloads are created
    final Collection<Download> downloads2 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document2.getId());
    assertEquals(3, downloads2.size());
    assertEqDownload(getDownload(downloads2, originalUrl2Old), originalUrl2Old, source1.getId(),
        false);
    assertEqDownload(getDownload(downloads2, url2Old), url2Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads2, originalUrl2New), originalUrl2New, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads2, url2New), url2New, source1.getId(), true);

    // Content is created
    final RawContent rawContent2 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document2.getId());
    assertEquals(rawContent2New, rawContent2.getContent());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document2.getSourceId(), document2.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadFailed_repeatedDownloadFailed() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);

    final String originalUrl2Old = "http://www.originalUrl2.de/";
    final String url2Old = "http://www.url2.de/";

    final String title2New = "title2New";
    final String originalUrl2New = "http://www.additionalUrl2.de/";
    final String url2New = url2Old;
    final String description2New = "description2New";
    final LocalDateTime published2New = LocalDateTime.now();
    final LocalDateTime downloaded2New = LocalDateTime.now();
    final Integer statusCode2New = 400;
    final String rawContent2New = null;
    final DownloadResult downloadResult2New = new DownloadResult(originalUrl2New, url2New,
        rawContent2New, null, downloaded2New, statusCode2New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title2New,
            description2New, published2New, downloadResult2New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document metadata is modified
    document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals(title2New, document2.getTitle());
    assertEquals(url2New, document2.getUrl());
    assertEquals(description2New, document2.getDescription());
    assertEquals(published2New, document2.getPublished());
    assertEquals(downloaded2New, document2.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOAD_ERROR, document2.getState());
    assertEquals(statusCode2New, document2.getStatusCode());

    // Crawling result corresponds to existing document
    assertEquals(result.getId(), document2.getId());

    // Additional downloads are created
    final Collection<Download> downloads2 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document2.getId());
    assertEquals(3, downloads2.size());
    assertEqDownload(getDownload(downloads2, originalUrl2Old), originalUrl2Old, source1.getId(),
        false);
    assertEqDownload(getDownload(downloads2, url2Old), url2Old, source1.getId(), false);
    assertEqDownload(getDownload(downloads2, originalUrl2New), originalUrl2New, source1.getId(),
        false);
    assertEqDownload(getDownload(downloads2, url2New), url2New, source1.getId(), false);

    // No content is created
    final RawContent rawContent2 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document2.getId());
    assertNull(rawContent2);

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document2.getSourceId(), document2.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadsSuccessAndFail_repeatedDownloadMerging() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawCntent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);

    final String originalUrl1Old = "http://www.originalUrl1.de/";
    final String url1Old = "http://www.url1.de/";

    final String title2Old = document2.getTitle();
    final String originalUrl2Old = "http://www.originalUrl2.de/";
    final String url2Old = "http://www.url2.de/";
    final String description2Old = document2.getDescription();
    final LocalDateTime published2Old = document2.getPublished();
    final LocalDateTime downloaded2Old = document2.getDownloaded();
    final Integer statusCode2Old = document2.getStatusCode();

    final String title1New = "title1New";
    final String originalUrl1New = url1Old;
    final String url1New = url2Old;
    final String description1New = "description1New";
    final LocalDateTime published1New = LocalDateTime.now();
    final LocalDateTime downloaded1New = LocalDateTime.now();
    final Integer statusCode1New = 200;
    final String rawContent1New = "rawContent1New";
    final DownloadResult downloadResult1New = new DownloadResult(originalUrl1New, url1New,
        rawContent1New, null, downloaded1New, statusCode1New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title1New,
            description1New, published1New, downloadResult1New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document 1 metadata is modified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawCntent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(title1New, document1.getTitle());
    assertEquals(url1New, document1.getUrl());
    assertEquals(description1New, document1.getDescription());
    assertEquals(published1New, document1.getPublished());
    assertEquals(downloaded1New, document1.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, document1.getState());
    assertEquals(statusCode1New, document1.getStatusCode());

    // Document 2 metadata is not modified (except state)
    document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals(title2Old, document2.getTitle());
    assertEquals(url2Old, document2.getUrl());
    assertEquals(description2Old, document2.getDescription());
    assertEquals(published2Old, document2.getPublished());
    assertEquals(downloaded2Old, document2.getDownloaded());
    assertEquals(statusCode2Old, document2.getStatusCode());

    // Document 2 is marked as deleted
    document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals(DocumentProcessingState.TO_BE_DELETED, document2.getState());

    // Crawling result corresponds to existing document with download success
    assertEquals(result.getId(), document1.getId());

    // Downloads are reassigned from document 2 to document 1
    final Collection<Download> downloads1 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(4, downloads1.size());
    assertEqDownload(getDownload(downloads1, originalUrl1Old), originalUrl1Old, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url1Old), url1Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads1, originalUrl2Old), originalUrl2Old, source1.getId(),
        false);
    assertEqDownload(getDownload(downloads1, url2Old), url2Old, source1.getId(), true);
    final Collection<Download> downloads2 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document2.getId());
    assertEquals(0, downloads2.size());

    // Content 1 is modified
    rawCntent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(rawContent1New, rawCntent1.getContent());

    // Statistics are not rebuilt for documents 1 and 2
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
    assertTrue(
        statisticsToBeRebuilt.getValue(document2.getSourceId(), document2.getPublishedDate()));
  }

  @Test
  public void performCrawling_originalDownloadsSuccessAndRedirectionFail_repeatedDownloadMerging() {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    RawContent rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    Document document3 = documentDao.getEntity(sessionProvider.getStatelessSession(), 3);

    final String originalUrl1Old = "http://www.originalUrl1.de/";
    final String url1Old = "http://www.url1.de/";

    final String title3Old = document3.getTitle();
    final String originalUrl3Old = "http://www.originalUrl3.de/";
    final String url3Old = "http://www.url3.de/";
    final String redirectedOriginalUrl3Old = "http://www.redirectedOriginalUrl3.de/";
    final String redirectedUrl3Old = "http://www.redirectedUrl3.de/";
    final String description3Old = document3.getDescription();
    final LocalDateTime published3Old = document3.getPublished();
    final LocalDateTime downloaded3Old = document3.getDownloaded();
    final Integer statusCode3Old = document3.getStatusCode();

    final String title1New = "title1New";
    final String originalUrl1New = url1Old;
    final String url1New = url3Old;
    final String description1New = "description1New";
    final LocalDateTime published1New = LocalDateTime.now();
    final LocalDateTime downloaded1New = LocalDateTime.now();
    final Integer statusCode1New = 200;
    final String rawContent1New = "rawContent1New";
    final DownloadResult downloadResult1New = new DownloadResult(originalUrl1New, url1New,
        rawContent1New, null, downloaded1New, statusCode1New);

    // Crawl
    final Document result =
        documentCrawler.performCrawling(sessionProvider.getStatelessSession(), title1New,
            description1New, published1New, downloadResult1New, crawling1.getId(), source1.getId());

    // No new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Document 1 metadata is modified
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(title1New, document1.getTitle());
    assertEquals(url1New, document1.getUrl());
    assertEquals(description1New, document1.getDescription());
    assertEquals(published1New, document1.getPublished());
    assertEquals(downloaded1New, document1.getDownloaded());
    assertEquals(DocumentProcessingState.DOWNLOADED, document1.getState());
    assertEquals(statusCode1New, document1.getStatusCode());

    // Document 3 metadata is not modified (except state)
    document3 = documentDao.getEntity(sessionProvider.getStatelessSession(), 3);
    assertEquals(title3Old, document3.getTitle());
    assertEquals(url3Old, document3.getUrl());
    assertEquals(description3Old, document3.getDescription());
    assertEquals(published3Old, document3.getPublished());
    assertEquals(downloaded3Old, document3.getDownloaded());
    assertEquals(statusCode3Old, document3.getStatusCode());

    // Document 3 is marked as deleted
    document3 = documentDao.getEntity(sessionProvider.getStatelessSession(), 3);
    assertEquals(DocumentProcessingState.TO_BE_DELETED, document3.getState());

    // Crawling result corresponds to existing document with download success
    assertEquals(result.getId(), document1.getId());

    // Downloads are reassigned from document 3 to document 1
    final Collection<Download> downloads1 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(6, downloads1.size());
    assertEqDownload(getDownload(downloads1, originalUrl1Old), originalUrl1Old, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url1Old), url1Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads1, originalUrl3Old), originalUrl3Old, source1.getId(),
        true);
    assertEqDownload(getDownload(downloads1, url3Old), url3Old, source1.getId(), true);
    assertEqDownload(getDownload(downloads1, redirectedOriginalUrl3Old), redirectedOriginalUrl3Old,
        source1.getId(), false);
    assertEqDownload(getDownload(downloads1, redirectedUrl3Old), redirectedUrl3Old, source1.getId(),
        false);
    final Collection<Download> downloads3 =
        downloadDao.getForDocument(sessionProvider.getStatelessSession(), document3.getId());
    assertEquals(0, downloads3.size());

    // Content 1 is modified
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(rawContent1New, rawContent1.getContent());

    // Statistics are rebuilt for documents 1 and 2
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
    assertTrue(
        statisticsToBeRebuilt.getValue(document3.getSourceId(), document3.getPublishedDate()));
  }

  private Download getDownload(final Collection<Download> downloads, final String url) {
    for (final Download download : downloads) {
      if (download.getUrl().equals(url)) {
        return download;
      }
    }

    return null;
  }

  private void assertEqDownload(final Download download, final String url, final long sourceId,
      final boolean success) {
    assertNotNull(download);
    assertEquals(url, download.getUrl());
    assertEquals(sourceId, download.getSourceId());
    assertEquals(success, download.getSuccess());
  }

}
