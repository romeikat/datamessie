package com.romeikat.datamessie.core.base.service.download;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2018 Dr. Raphael Romeikat
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

import java.io.InputStream;
import java.net.URLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssFeedDownloader extends AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(RssFeedDownloader.class);

  private static final boolean ALLOW_DOCTYPES = true;

  public SyndFeed downloadRssFeed(final String sourceUrl) {
    return downloadRssFeed(sourceUrl, 1);
  }

  public SyndFeed downloadRssFeed(final String sourceUrl, final int maxAttempts) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      final SyndFeed syndFeed = download(sourceUrl);
      if (syndFeed != null) {
        return syndFeed;
      }

      waitMillis(getTimeout());
    }

    return null;
  }

  private SyndFeed download(final String sourceUrl) {
    LOG.debug("Downloading content from {}", sourceUrl);
    // Download source
    XmlReader xmlReader = null;
    URLConnection urlConnection = null;
    SyndFeed syndFeed = null;
    try {
      urlConnection = getConnection(sourceUrl);
      final String responseUrl = getResponseUrl(urlConnection);
      if (responseUrl != null) {
        closeUrlConnection(urlConnection);
        urlConnection = getConnection(responseUrl);
        LOG.debug("Redirection (server): {} -> {}", sourceUrl, responseUrl);
      }
      final InputStream urlInputStream = asInputStream(urlConnection, true, false);
      xmlReader = new XmlReader(urlInputStream);
      final SyndFeedInput syndFeedInput = new SyndFeedInput();
      syndFeedInput.setAllowDoctypes(ALLOW_DOCTYPES);
      syndFeed = syndFeedInput.build(xmlReader);
    } catch (final Exception e) {
      LOG.warn("Could not download feed from " + sourceUrl, e);
    } finally {
      try {
        xmlReader.close();
        closeUrlConnection(urlConnection);
      } catch (final Exception e) {
      }
    }
    return syndFeed;
  }

  private void waitMillis(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
    }
  }

}
