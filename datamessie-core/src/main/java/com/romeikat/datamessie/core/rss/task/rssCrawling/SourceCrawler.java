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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.service.DownloadService;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.service.download.Downloader;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.HtmlUtil;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

public class SourceCrawler {

  private static final Logger LOG = LoggerFactory.getLogger(SourceCrawler.class);

  private final DownloadService downloadService;
  private final Downloader downloader;
  private final HtmlUtil htmlUtil;
  private final IStatisticsManager statisticsManager;
  private final StringUtil stringUtil;

  private final DocumentCrawler documentCrawler;

  private final Double documentsParallelismFactor;
  private final StatisticsRebuildingSparseTable statisticsToBeRebuilt;

  public SourceCrawler(final ApplicationContext ctx) {
    downloadService = ctx.getBean(DownloadService.class);
    downloader = ctx.getBean(Downloader.class);
    htmlUtil = ctx.getBean(HtmlUtil.class);
    statisticsManager =
        ctx.getBean(SharedBeanProvider.class).getSharedBean(IStatisticsManager.class);
    stringUtil = ctx.getBean(StringUtil.class);

    documentCrawler = new DocumentCrawler(ctx);

    documentsParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "crawling.documents.parallelism.factor"));

    statisticsToBeRebuilt = new StatisticsRebuildingSparseTable();
  }

  public void performCrawling(final HibernateSessionProvider sessionProvider,
      final TaskExecution taskExecution, final Crawling crawling, final Source source) {
    final TaskExecutionWork work = taskExecution
        .reportWorkStart(String.format("Source %s: performing crawling", source.getId()));

    // Download RSS feed
    final SyndFeed syndFeed = downloadFeed(source);
    if (syndFeed == null) {
      return;
    }

    // Process RSS feed
    processFeed(sessionProvider, crawling, source, syndFeed);

    // Rebuild statistics
    if (statisticsManager != null) {
      statisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    }

    // Done
    taskExecution.reportWorkEnd(work);
  }

  private SyndFeed downloadFeed(final Source source) {
    LOG.debug("Source {}: downloading RSS feed", source.getId());

    final String url = source.getUrl();
    if (StringUtils.isBlank(url)) {
      LOG.warn("Source {}: missing URL for RSS feed", source.getId());
      return null;
    }
    final String rssFeedUrl = htmlUtil.addProtocolIfNecessary(url);
    final SyndFeed syndFeed = downloader.downloadRssFeed(rssFeedUrl);
    return syndFeed;
  }

  private void processFeed(final HibernateSessionProvider sessionProvider, final Crawling crawling,
      final Source source, final SyndFeed feed) {
    LOG.debug("Source {}: crawling documents", source.getId());

    // Determine which URLs to download
    final Map<String, SyndEntry> entriesPerUrlToBeDownloaded =
        getEntriesToBeDownloaded(sessionProvider, source.getId(), feed);

    // Download contents for new URLs
    final Set<String> entriesUrls = entriesPerUrlToBeDownloaded.keySet();
    final Map<String, DownloadResult> downloadResultsPerUrl =
        downloadEntries(source.getId(), entriesUrls);

    // Process entries and download results
    for (final Entry<String, SyndEntry> entry : entriesPerUrlToBeDownloaded.entrySet()) {
      final String url = entry.getKey();
      final SyndEntry syndEntry = entry.getValue();

      final DownloadResult downloadResult = downloadResultsPerUrl.get(url);
      if (downloadResult == null) {
        LOG.error("Source {}: no download result found for URL {}", source.getId(), url);
        continue;
      }

      processUrl(sessionProvider.getStatelessSession(), url, syndEntry, downloadResult,
          crawling.getId(), source.getId());
    }

    sessionProvider.closeStatelessSession();
  }

  private void processUrl(final StatelessSession statelessSession, final String url,
      final SyndEntry entry, final DownloadResult downloadResult, final long crawlingId,
      final long sourceId) {
    new ExecuteWithTransaction(statelessSession) {
      @Override
      protected void execute(final StatelessSession statelessSession) {
        final String title = getTitle(entry);
        final String description = getDescription(entry);
        final LocalDateTime published = getPublished(entry);

        documentCrawler.performCrawling(statelessSession, title, description, published,
            downloadResult, crawlingId, sourceId);
        statisticsToBeRebuilt.putValues(documentCrawler.getStatisticsToBeRebuilt());
      }

      @Override
      protected void onException(final Exception e) {
        LOG.error("Source " + sourceId + ": could not perform crawling for " + url, e);
      };
    }.execute();
  }

  private Map<String, SyndEntry> getEntriesToBeDownloaded(
      final HibernateSessionProvider sessionProvider, final long sourceId,
      final SyndFeed syndFeed) {
    final Map<String, SyndEntry> entriesPerUrl = getUniqueEntriesPerUrl(sourceId, syndFeed);

    // Filter URLs that should be downloaded
    final Predicate<String> shouldUrlBeDownloadedPredicate = new Predicate<String>() {
      @Override
      public boolean apply(final String url) {
        return shouldUrlBeDownloaded(sessionProvider, sourceId, url);
      }
    };
    final Map<String, SyndEntry> syndEntriesPerUrlToBeDownloaded =
        Maps.filterKeys(entriesPerUrl, shouldUrlBeDownloadedPredicate);
    return Maps.newLinkedHashMap(syndEntriesPerUrlToBeDownloaded);
  }

  private Map<String, SyndEntry> getUniqueEntriesPerUrl(final long sourceId, final SyndFeed feed) {
    final List<SyndEntry> entries = feed.getEntries();

    final Map<String, SyndEntry> entriesPerUrl = Maps.newLinkedHashMap();
    int numberOfMissingUrls = 0;
    for (final SyndEntry entry : entries) {
      String url = entry.getLink();
      if (StringUtils.isBlank(url)) {
        numberOfMissingUrls++;
        continue;
      }
      url = url.trim();

      final boolean doesUrlAlreadyExist = entriesPerUrl.containsKey(url);
      if (doesUrlAlreadyExist) {
        LOG.info("Source {}: ignoring duplicate URL {}", sourceId, url);
        continue;
      }

      entriesPerUrl.put(url, entry);
    }

    if (numberOfMissingUrls > 0) {
      final String singularPlural = stringUtil.getSingularOrPluralTerm("URL", numberOfMissingUrls);
      LOG.info("Source {}: {} missing {} in RSS feed", sourceId, numberOfMissingUrls,
          singularPlural);
    }

    return entriesPerUrl;
  }

  private boolean shouldUrlBeDownloaded(final HibernateSessionProvider sessionProvider,
      final long sourceId, final String url) {
    try {
      // Validate URL
      final boolean isUrlValid = isUrlValid(sourceId, url);
      if (!isUrlValid) {
        LOG.debug("Source {}: invalid URL {} provided", sourceId, url);
        return false;
      }

      // Skip if URL has already been successfully downloaded
      final boolean existsWithDownloadSuccess = downloadService
          .existsWithDownloadSuccess(sessionProvider.getStatelessSession(), url, sourceId);
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
      sessionProvider.closeStatelessSession();
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

          final DownloadResult downloadResult = downloader.downloadContent(url);
          downloadResults.put(url, downloadResult);
        } catch (final Exception e) {
          LOG.error(String.format("Source %s: could not download URL %s", sourceId, url), e);
        }
      }
    };

    return downloadResults;
  }

  private String getTitle(final SyndEntry entry) {
    String title = entry.getTitle();
    title = StringEscapeUtils.unescapeHtml4(title);
    return title;
  }

  private String getDescription(final SyndEntry entry) {
    String description = entry.getDescription() == null ? null : entry.getDescription().getValue();
    description = StringEscapeUtils.unescapeHtml4(description);
    return description;
  }

  private LocalDateTime getPublished(final SyndEntry entry) {
    final Date publishedDate = entry.getPublishedDate();
    if (publishedDate == null) {
      return null;
    }

    return DateUtil.toLocalDateTime(publishedDate);
  }

}
