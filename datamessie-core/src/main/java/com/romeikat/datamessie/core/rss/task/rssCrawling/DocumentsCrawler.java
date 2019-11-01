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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.service.DownloadService;
import com.romeikat.datamessie.core.base.service.download.ContentDownloader;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.model.core.Document;

public abstract class DocumentsCrawler {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsCrawler.class);

  private final DownloadService downloadService;
  private final ContentDownloader contentDownloader;
  private final IStatisticsManager statisticsManager;

  private final DocumentCrawler documentCrawler;

  private final double documentsParallelismFactor;
  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;

  public DocumentsCrawler(final ApplicationContext ctx) {
    downloadService = ctx.getBean(DownloadService.class);
    contentDownloader = ctx.getBean(ContentDownloader.class);
    statisticsManager =
        ctx.getBean(SharedBeanProvider.class).getSharedBean(IStatisticsManager.class);

    documentCrawler = new DocumentCrawler(ctx);

    documentsParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "crawling.documents.parallelism.factor"));
    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();
  }

  public void performCrawling(final HibernateSessionProvider sessionProvider,
      final TaskExecution taskExecution, final long crawlingId, final long sourceId,
      final Collection<String> urls) {
    final TaskExecutionWork work =
        taskExecution.reportWorkStart(String.format("Source %s: performing crawling", sourceId));

    // Determine URLs to be crawled
    final Set<String> urlsToBeCrawled =
        getUrlsToBeCrawled(sessionProvider.getStatelessSession(), sourceId, urls);

    // Process URLs
    processUrls(sessionProvider, crawlingId, sourceId, urlsToBeCrawled);

    // Close session
    sessionProvider.closeStatelessSession();

    // Rebuild statistics
    if (statisticsManager != null) {
      statisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    }

    // Done
    taskExecution.reportWorkEnd(work);
  }

  private Set<String> getUrlsToBeCrawled(final StatelessSession statelessSession,
      final long sourceId, final Collection<String> urls) {
    return urls.stream().filter(u -> shouldUrlBeDownloaded(statelessSession, sourceId, u))
        .collect(Collectors.toSet());
  }

  private boolean shouldUrlBeDownloaded(final StatelessSession statelessSession,
      final long sourceId, final String url) {
    try {
      // Validate URL
      final boolean isUrlValid = isUrlValid(sourceId, url);
      if (!isUrlValid) {
        LOG.debug("Source {}: invalid URL {} provided", sourceId, url);
        return false;
      }

      // Skip if URL has already been successfully downloaded
      final boolean existsWithDownloadSuccess =
          downloadService.existsWithDownloadSuccess(statelessSession, url, sourceId);
      if (existsWithDownloadSuccess) {
        LOG.debug("Source {}: URL {} has already been downloaded", sourceId, url);
        return false;
      }

      LOG.debug("Source {}: URL {} should be downloaded", sourceId, url);
      return true;
    } catch (final Exception e) {
      LOG.error(
          String.format("Source %s: could not determine whether to download URL %s", sourceId, url),
          e);
      return false;
    }
  }

  private boolean isUrlValid(final long sourceId, final String url) {
    if (url != null && !url.startsWith("http")) {
      LOG.warn("Source {}: skipping document due to malformed URL {}", sourceId, url);
      return false;
    }

    return true;
  }

  private void processUrls(final HibernateSessionProvider sessionProvider, final long crawlingId,
      final long sourceId, final Set<String> urls) {
    // Download contents for new URLs
    final Map<String, DownloadResult> downloadResultsPerUrl = downloadEntries(sourceId, urls);

    // Process entries and download results
    for (final String url : urls) {
      final DownloadResult downloadResult = downloadResultsPerUrl.get(url);
      if (downloadResult == null) {
        LOG.error("Source {}: no download result found for URL {}", sourceId, url);
        continue;
      }

      processUrl(sessionProvider, url, downloadResult, crawlingId, sourceId);
    }
  }

  private void processUrl(final HibernateSessionProvider sessionProvider, final String url,
      final DownloadResult downloadResult, final long crawlingId, final long sourceId) {
    new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        final boolean shouldProcess = onBeforeCrawlingUrl(url, downloadResult);
        if (!shouldProcess) {
          return;
        }

        final String title = getTitle(url, downloadResult);
        final String description = getDescription(url, downloadResult);
        final LocalDateTime published = getPublished(url, downloadResult);

        final Document result = documentCrawler.performCrawling(statelessSession, title,
            description, published, downloadResult, crawlingId, sourceId);

        onAfterCrawlingUrl(url, result);

        statisticsToBeRebuilt.putValues(documentCrawler.getStatisticsToBeRebuilt());
      }

      @Override
      protected void onException(final Exception e) {
        LOG.error("Source " + sourceId + ": could not perform crawling for " + url, e);
        sessionProvider.closeStatelessSession();
      }
    }.execute();
  }

  private ConcurrentMap<String, DownloadResult> downloadEntries(final long sourceId,
      final Set<String> urls) {
    final ConcurrentMap<String, DownloadResult> downloadResults =
        new ConcurrentHashMap<String, DownloadResult>();

    final List<String> urlsList = Lists.newArrayList(urls);
    new ParallelProcessing<String>(null, urlsList, documentsParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final String url) {
        try {
          LOG.debug("Source {}: downloading {}", sourceId, url);

          final DownloadResult downloadResult = contentDownloader.downloadContent(url);
          downloadResults.put(url, downloadResult);
        } catch (final Exception e) {
          LOG.error(String.format("Source %s: could not download URL %s", sourceId, url), e);
        }
      }
    };

    return downloadResults;
  }

  protected boolean onBeforeCrawlingUrl(final String url, final DownloadResult downloadResult) {
    return true;
  }

  protected void onAfterCrawlingUrl(final String url, final Document result) {}

  protected abstract String getTitle(String url, DownloadResult downloadResult);

  protected abstract String getDescription(String url, DownloadResult downloadResult);

  protected abstract LocalDateTime getPublished(String url, DownloadResult downloadResult);

  public StatisticsRebuildingSparseTable getStatisticsToBeRebuilt() {
    return statisticsToBeRebuilt;
  }

}
