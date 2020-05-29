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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.XmlUtil;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class RssFeedDownloader extends AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(RssFeedDownloader.class);

  private static final boolean ALLOW_DOCTYPES = true;

  @Autowired
  private XmlUtil xmlUtil;

  @Value("${crawling.feed.download.attempts}")
  private int downloadAttempts;

  public SyndFeed downloadRssFeed(final String sourceUrl, final DownloadSession downloadSession) {
    LOG.debug("Downloading feed from {}", sourceUrl);

    // Download feed
    final DownloadResult downloadResultFeed =
        download(sourceUrl, downloadAttempts, downloadSession);
    final boolean feedDownloadSuccess = downloadResultFeed.getContent() != null;
    if (!feedDownloadSuccess) {
      LOG.info("Could not download feed from " + sourceUrl);
      return null;
    }

    // Process feed
    postProcessContent(downloadResultFeed);

    // Parse feed
    try (final InputStream feedInputStream = IOUtils.toInputStream(downloadResultFeed.getContent());
        final XmlReader xmlReader = new XmlReader(feedInputStream);) {
      final SyndFeedInput syndFeedInput = new SyndFeedInput();
      syndFeedInput.setAllowDoctypes(ALLOW_DOCTYPES);
      final SyndFeed syndFeed = syndFeedInput.build(xmlReader);
      return syndFeed;
    } catch (final Exception e) {
      LOG.info("Could not parse feed from " + sourceUrl, e);
      return null;
    }
  }

  private void postProcessContent(final DownloadResult downloadResult) {
    final String content = downloadResult.getContent();
    if (content == null) {
      return;
    }

    // Strip non-valid characters as specified by the XML 1.0 standard
    final String validContent = xmlUtil.stripNonValidXMLCharacters(content);

    downloadResult.setContent(validContent);
  }

}
