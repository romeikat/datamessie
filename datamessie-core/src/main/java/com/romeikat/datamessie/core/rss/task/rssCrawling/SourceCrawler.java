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
import java.util.Set;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.service.download.RssFeedDownloader;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.HtmlUtil;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.model.core.Source;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

public class SourceCrawler {

  private static final Logger LOG = LoggerFactory.getLogger(SourceCrawler.class);

  private final ApplicationContext ctx;

  private final RssFeedDownloader rssFeedDownloader;
  private final HtmlUtil htmlUtil;
  private final StringUtil stringUtil;

  private final int feedDownloadAttempts;

  public SourceCrawler(final ApplicationContext ctx) {
    this.ctx = ctx;

    rssFeedDownloader = ctx.getBean(RssFeedDownloader.class);
    htmlUtil = ctx.getBean(HtmlUtil.class);
    stringUtil = ctx.getBean(StringUtil.class);

    feedDownloadAttempts =
        Integer.parseInt(SpringUtil.getPropertyValue(ctx, "crawling.feed.download.attempts"));
  }

  public void performCrawling(final HibernateSessionProvider sessionProvider,
      final TaskExecution taskExecution, final long crawlingId, final Source source) {
    final TaskExecutionWork work = taskExecution
        .reportWorkStart(String.format("Source %s: performing crawling", source.getId()));

    // Download RSS feed
    final SyndFeed syndFeed = downloadFeed(source);
    if (syndFeed == null) {
      LOG.debug("Source {}: RSS feed could not be downloaded", source.getId());
      return;
    }

    // Process RSS feed
    processFeed(sessionProvider, taskExecution, crawlingId, source.getId(), syndFeed);

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
    final SyndFeed syndFeed = rssFeedDownloader.downloadRssFeed(rssFeedUrl, feedDownloadAttempts);
    return syndFeed;
  }

  private void processFeed(final HibernateSessionProvider sessionProvider,
      final TaskExecution taskExecution, final long crawlingId, final long sourceId,
      final SyndFeed feed) {
    LOG.debug("Source {}: crawling documents", sourceId);

    // Determine entries to be processed
    final Map<String, SyndEntry> entriesPerUrl = getUniqueEntriesPerUrl(sourceId, feed);

    // Process entries of feed
    final DocumentsCrawler documentsCrawler = new DocumentsCrawler(ctx) {
      @Override
      protected String getTitle(final String url, final DownloadResult downloadResult) {
        final SyndEntry syndEntry = entriesPerUrl.get(url);
        if (syndEntry == null) {
          return null;
        }

        return StringEscapeUtils.unescapeHtml4(syndEntry.getTitle());
      }

      @Override
      protected String getDescription(final String url, final DownloadResult downloadResult) {
        final SyndEntry syndEntry = entriesPerUrl.get(url);
        if (syndEntry == null) {
          return null;
        }

        final String description =
            syndEntry.getDescription() == null ? null : syndEntry.getDescription().getValue();
        return StringEscapeUtils.unescapeHtml4(description);
      }

      @Override
      protected LocalDateTime getPublished(final String url, final DownloadResult downloadResult) {
        final SyndEntry syndEntry = entriesPerUrl.get(url);
        if (syndEntry == null) {
          return null;
        }

        final Date publishedDate = syndEntry.getPublishedDate();
        if (publishedDate == null) {
          return null;
        }

        return DateUtil.toLocalDateTime(publishedDate);
      }
    };
    final Set<String> urls = entriesPerUrl.keySet();
    documentsCrawler.performCrawling(sessionProvider, taskExecution, crawlingId, sourceId, urls);
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

}
