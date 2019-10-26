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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.DownloadService;
import com.romeikat.datamessie.core.base.service.download.ContentDownloader;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.Crawling;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.Project;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.impl.CrawlingImpl;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.ProjectImpl;
import com.romeikat.datamessie.core.domain.entity.impl.RawContentImpl;
import com.romeikat.datamessie.core.domain.entity.impl.SourceImpl;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.rss.dao.CrawlingDao;

public class DocumentsCrawlerTest extends AbstractDbSetupBasedTest {

  private DocumentsCrawler documentsCrawler;

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private DownloadService downloadService;

  @Autowired
  private ContentDownloader contentDownloader;

  @Mock
  private TaskExecution taskExecution;

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  @Qualifier("sourceDao")
  private SourceDao sourceDao;

  @Autowired
  private DocumentDao documentDao;

  @Autowired
  private RawContentDao rawContentDao;

  @Override
  protected Operation initDb() {
    final Project project1 = new ProjectImpl(1, "Project1", false, false);
    final Source source1 = new SourceImpl(1, "Source1", "http://www.source1.de/", true, false);
    final Crawling crawling1 = new CrawlingImpl(1, project1.getId());
    final LocalDateTime now = LocalDateTime.now();
    // Document1 with download success
    final LocalDateTime published1 = now.minusDays(1);
    final Document document1 = new DocumentImpl(1, crawling1.getId(), source1.getId())
        .setTitle("Title1").setUrl("http://www.document1.de/").setDescription("Description1")
        .setPublished(published1).setDownloaded(now).setState(DocumentProcessingState.DOWNLOADED)
        .setStatusCode(200);
    final Download download1 = new Download(1, 1, 1, true).setUrl("http://www.url1.de/");
    final RawContent rawContent1 = new RawContentImpl(document1.getId(), "RawContent1");
    // Document2 without download success
    final LocalDateTime published2 = now.minusDays(2);
    final Document document2 = new DocumentImpl(2, crawling1.getId(), source1.getId())
        .setTitle("Title2").setUrl("http://www.document2.de/").setDescription("Description2")
        .setPublished(published2).setDownloaded(now)
        .setState(DocumentProcessingState.DOWNLOAD_ERROR).setStatusCode(400);
    final Download download2 = new Download(2, 1, 2, false).setUrl("http://www.url2.de/");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        sequenceOf(CommonOperations.insertIntoProject(project1),
            CommonOperations.insertIntoSource(source1),
            CommonOperations.insertIntoCrawling(crawling1),
            CommonOperations.insertIntoDocument(document1),
            CommonOperations.insertIntoDocument(document2),
            CommonOperations.insertIntoDownload(download1),
            CommonOperations.insertIntoDownload(download2),
            CommonOperations.insertIntoRawContent(rawContent1)));
  }

  @Test
  public void performCrawling_newDocument() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url3 = "http://www.url3.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished3 = now.minusDays(3);
    final LocalDateTime newDownloaded3 = now;
    final String newRawContent3 = "Content3-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url3)) {
          return newPublished3;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download result
    final DownloadResult downloadResult3 =
        new DownloadResult(null, url3, newRawContent3, newDownloaded3, null);
    doReturn(downloadResult3).when(contentDownloaderSpy).downloadContent(eq(url3));

    // Crawl
    final List<String> urls = Lists.newArrayList(url3);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // New document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Download date is set
    final Document document3 = documentDao.getForUrlAndSource(sessionProvider.getStatelessSession(),
        url3, source1.getId());
    assertEquals(newDownloaded3, document3.getDownloaded());
    // Content is set
    final RawContent rawContent3 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document3.getId());
    assertEquals(newRawContent3, rawContent3.getContent());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document3.getSourceId(), document3.getPublishedDate()));
  }

  @Test
  public void performCrawling_newDocumentManyTimes() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url3 = "http://www.url3.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished3 = now.minusDays(3);
    final LocalDateTime newDownloaded3 = now;
    final String newRawContent3 = "Content3-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url3)) {
          return newPublished3;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download result
    final DownloadResult downloadResult3 =
        new DownloadResult(null, url3, newRawContent3, newDownloaded3, null);
    doReturn(downloadResult3).when(contentDownloaderSpy).downloadContent(eq(url3));

    // Crawl
    final int numberOfUrls = 1000;
    final List<String> urls = Lists.newArrayListWithExpectedSize(numberOfUrls);
    for (int i = 0; i < numberOfUrls; i++) {
      urls.add(url3);
    }
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), Lists.newArrayList(urls));

    // Only one new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Download date is set
    final Document document3 = documentDao.getForUrlAndSource(sessionProvider.getStatelessSession(),
        url3, source1.getId());
    assertEquals(newDownloaded3, document3.getDownloaded());
    // Content is set
    final RawContent rawContent3 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document3.getId());
    assertEquals(newRawContent3, rawContent3.getContent());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document3.getSourceId(), document3.getPublishedDate()));
  }

  @Test
  public void performCrawling_existingDocumentWithContent() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    Document document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final LocalDateTime oldDownloaded1 = document1.getDownloaded();
    RawContent rawContent1 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    final String oldContent1 = rawContent1.getContent();

    final String url1 = "http://www.url1.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished1 = now.minusDays(1);
    final LocalDateTime newDownloaded1 = now;
    final String newRawContent1 = "Content1-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url1)) {
          return newPublished1;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download result
    final DownloadResult downloadResult1 =
        new DownloadResult(null, url1, newRawContent1, newDownloaded1, null);
    doReturn(downloadResult1).when(contentDownloaderSpy).downloadContent(eq(url1));

    // Crawl
    final List<String> urls = Lists.newArrayList(url1);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // No document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(2, count);

    // Download date remains the same
    document1 = documentDao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(oldDownloaded1, document1.getDownloaded());
    // Content remains the same
    rawContent1 = rawContentDao.getEntity(sessionProvider.getStatelessSession(), document1.getId());
    assertEquals(oldContent1, rawContent1.getContent());

    // Statistics are not rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertNull(
        statisticsToBeRebuilt.getValue(document1.getSourceId(), document1.getPublishedDate()));
  }

  @Test
  public void performCrawling_existingDocumentWithoutContent() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url2 = "http://www.url2.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished2 = now.minusDays(2);
    final LocalDateTime newDownloaded2 = now;
    final String newRawContent2 = "Content2-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url2)) {
          return newPublished2;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download result
    final DownloadResult downloadResult2 =
        new DownloadResult(null, url2, newRawContent2, newDownloaded2, null);
    doReturn(downloadResult2).when(contentDownloaderSpy).downloadContent(eq(url2));

    // Crawl
    final List<String> urls = Lists.newArrayList(url2);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // No document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(2, count);

    // Download date is updated
    final Document document2 = documentDao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals(newDownloaded2, document2.getDownloaded());
    // Content is set
    final RawContent rawContent2 =
        rawContentDao.getEntity(sessionProvider.getStatelessSession(), document2.getId());
    assertEquals(newRawContent2, rawContent2.getContent());

    // Statistics are rebuilt
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertTrue(
        statisticsToBeRebuilt.getValue(document2.getSourceId(), document2.getPublishedDate()));
  }

  @Test
  public void performCrawling_exceptionWhileCheckingWhetherUrlExistsWithDownloadSuccess()
      throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url3 = "http://www.url3.de/";
    final String url4 = "http://www.url4.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished3 = now.minusDays(3);
    final LocalDateTime newPublished4 = now.minusDays(4);
    final LocalDateTime newDownloaded4 = now;
    final String newRawContent4 = "Content4-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url3)) {
          return newPublished3;
        } else if (StringUtils.equals(url, url4)) {
          return newPublished4;
        } else {
          return null;
        }
      }
    };
    final DownloadService downloadServiceSpy =
        createAndInjectSpy(downloadService, documentsCrawler, "downloadService");
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download results (3 fails, 4 works)
    doThrow(Exception.class).when(downloadServiceSpy)
        .existsWithDownloadSuccess(any(StatelessSession.class), eq(url3), any(Long.class));
    final DownloadResult downloadResult4 =
        new DownloadResult(null, url4, newRawContent4, newDownloaded4, null);
    doReturn(downloadResult4).when(contentDownloaderSpy).downloadContent(eq(url4));

    // Crawl
    final List<String> urls = Lists.newArrayList(url3, url4);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // Only one new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Statistics are not rebuilt for document 3, but for document 4
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertNull(statisticsToBeRebuilt.getValue(source1.getId(), newPublished3.toLocalDate()));
    assertTrue(statisticsToBeRebuilt.getValue(source1.getId(), newPublished4.toLocalDate()));
  }

  @Test
  public void performCrawling_exceptionWhileDownloadingContent() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url3 = "http://www.url3.de/";
    final String url4 = "http://www.url4.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished3 = now.minusDays(3);
    final LocalDateTime newPublished4 = now.minusDays(4);

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url3)) {
          return newPublished3;
        } else if (StringUtils.equals(url, url4)) {
          return newPublished4;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");

    // Mock download results (3 fails, 4 works)
    doThrow(Exception.class).when(contentDownloaderSpy).downloadContent(eq(url3));
    final LocalDateTime newDownloaded4 = LocalDateTime.now();
    final String newRawContent4 = "Content4-new";
    final DownloadResult downloadResult4 =
        new DownloadResult(null, url4, newRawContent4, newDownloaded4, null);
    doReturn(downloadResult4).when(contentDownloaderSpy).downloadContent(eq(url4));

    // Crawl
    final List<String> urls = Lists.newArrayList(url3, url4);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // Only one new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Statistics are not rebuilt for document 3, but for document 4
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertNull(statisticsToBeRebuilt.getValue(source1.getId(), newPublished3.toLocalDate()));
    assertTrue(statisticsToBeRebuilt.getValue(source1.getId(), newPublished4.toLocalDate()));
  }

  @Test
  public void performCrawling_exceptionWhileCrawlingDocument() throws Exception {
    final Crawling crawling1 = crawlingDao.getEntity(sessionProvider.getStatelessSession(), 1);
    final Source source1 = sourceDao.getEntity(sessionProvider.getStatelessSession(), 1);

    final String url3 = "http://www.url3.de/";
    final String url4 = "http://www.url4.de/";
    final LocalDateTime now = LocalDateTime.now();
    final LocalDateTime newPublished3 = now.minusDays(3);
    final LocalDateTime newPublished4 = now.minusDays(4);
    final LocalDateTime newDownloaded3 = now;
    final LocalDateTime newDownloaded4 = now;
    final String newRawContent3 = "Content3-new";
    final String newRawContent4 = "Content4-new";

    documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        return null;
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        if (StringUtils.equals(url, url3)) {
          return newPublished3;
        } else if (StringUtils.equals(url, url4)) {
          return newPublished4;
        } else {
          return null;
        }
      }
    };
    final ContentDownloader contentDownloaderSpy =
        createAndInjectSpy(contentDownloader, documentsCrawler, "contentDownloader");
    final DocumentCrawler documentCrawler = new DocumentCrawler(ctx);
    final DocumentCrawler documentCrawlerSpy =
        createAndInjectSpy(documentCrawler, documentsCrawler, "documentCrawler");

    // Mock download results
    final DownloadResult downloadResult3 =
        new DownloadResult(null, url3, newRawContent3, newDownloaded3, null);
    doReturn(downloadResult3).when(contentDownloaderSpy).downloadContent(eq(url3));
    final DownloadResult downloadResult4 =
        new DownloadResult(null, url4, newRawContent4, newDownloaded4, null);
    doReturn(downloadResult4).when(contentDownloaderSpy).downloadContent(eq(url4));

    // Crawl (3 fails, 4 works)
    doThrow(Exception.class).when(documentCrawlerSpy).performCrawling(any(StatelessSession.class),
        anyString(), anyString(), any(LocalDateTime.class), eq(downloadResult3), any(Long.class),
        any(Long.class));
    final List<String> urls = Lists.newArrayList(url3, url4);
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawling1.getId(),
        source1.getId(), urls);

    // Only one new document is created
    final long count = documentDao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    // Statistics are not rebuilt for document 3, but for document 4
    final StatisticsRebuildingSparseTable statisticsToBeRebuilt =
        documentsCrawler.getStatisticsToBeRebuilt();
    assertNull(statisticsToBeRebuilt.getValue(source1.getId(), newPublished3.toLocalDate()));
    assertTrue(statisticsToBeRebuilt.getValue(source1.getId(), newPublished4.toLocalDate()));
  }

}
