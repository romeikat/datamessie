package com.romeikat.datamessie.core.base.service.download;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.XmlUtil;

@Service
public class ContentDownloader extends AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(ContentDownloader.class);

  @Value("${crawling.documents.download.attempts}")
  private int downloadAttempts;

  @Autowired
  private XmlUtil xmlUtil;

  public DownloadResult downloadContent(final String url, final boolean followMetaBasedRedirects,
      final DownloadSession downloadSession) {
    LOG.debug("Downloading content from {}", url);

    DownloadResult downloadResultOriginal = null;
    DownloadResult downloadResultRedirected = null;

    // Download original URL, following server-side redirects
    downloadResultOriginal = download(url, downloadAttempts, downloadSession);

    // Also follow meta-based redirects, if content available and following desired
    final boolean originalDownloadSuccess = downloadResultOriginal.getContent() != null;
    if (originalDownloadSuccess && followMetaBasedRedirects) {
      // Parse HTML
      final org.jsoup.nodes.Document jsoupDocument = parseJsoupDocument(downloadResultOriginal);

      // Download redirected URL, if a redirection is found
      downloadResultRedirected = followMetaBasedRedirects(url, jsoupDocument, downloadSession);
    }

    // Decide for which download result to use
    final DownloadResult downloadResult =
        buildDownloadResultOverall(url, downloadResultOriginal, downloadResultRedirected);
    final boolean downloadSuccess = downloadResult.getContent() != null;
    if (!downloadSuccess) {
      LOG.warn("Could not download content from " + url);
    }

    // Process content
    postProcessContent(downloadResult);

    return downloadResult;
  }

  private org.jsoup.nodes.Document parseJsoupDocument(final DownloadResult downloadResult) {
    try {
      return Jsoup.parse(downloadResult.getContent(), downloadResult.getUrl());
    } catch (final Exception e) {
      LOG.warn("Could not parse content of " + downloadResult.getUrl(), e);
      return null;
    }
  }

  private DownloadResult followMetaBasedRedirects(final String url,
      final org.jsoup.nodes.Document jsoupDocument, final DownloadSession downloadSession) {
    if (jsoupDocument == null) {
      return null;
    }

    // Meta redirection (<link rel="canonical" .../>)
    String redirectedUrl = checkForMetaRedirectionCanonical(url, jsoupDocument);
    if (redirectedUrl != null) {
      final DownloadResult downloadResult =
          download(redirectedUrl, downloadAttempts, downloadSession);
      return downloadResult;
    }

    // Meta redirection (<meta http-equiv="refresh" .../>)
    redirectedUrl = checkForMetaRedirectionRefresh(url, jsoupDocument);
    if (redirectedUrl != null) {
      final DownloadResult downloadResult =
          download(redirectedUrl, downloadAttempts, downloadSession);
      return downloadResult;
    }

    // Meta redirection (<meta property="og:url" .../>)
    redirectedUrl = checkForMetaRedirectionOgUrl(url, jsoupDocument);
    if (redirectedUrl != null) {
      final DownloadResult downloadResult =
          download(redirectedUrl, downloadAttempts, downloadSession);
      return downloadResult;
    }

    return null;
  }

  private String checkForMetaRedirectionCanonical(final String url,
      final org.jsoup.nodes.Document jsoupDocument) {
    final Elements metaTagsHtmlHeadLink = jsoupDocument.select("html head link");

    for (final Element metaTag : metaTagsHtmlHeadLink) {
      final Attributes metaTagAttributes = metaTag.attributes();
      if (metaTagAttributes.hasKey("rel")
          && metaTagAttributes.get("rel").equalsIgnoreCase("canonical")
          && metaTagAttributes.hasKey("href")) {
        final String redirectedUrl = metaTagAttributes.get("href").trim();
        if (isValidRedirection(url, redirectedUrl)) {
          LOG.debug("Redirection (<link rel=\"canonical\" .../>): {} -> {}", url, redirectedUrl,
              redirectedUrl);
          return redirectedUrl;
        }
      }
    }

    return null;
  }

  private String checkForMetaRedirectionRefresh(final String url,
      final org.jsoup.nodes.Document jsoupDocument) {
    final Elements metaTagsHtmlHeadMeta = jsoupDocument.select("html head meta");

    for (final Element metaTag : metaTagsHtmlHeadMeta) {
      final Attributes metaTagAttributes = metaTag.attributes();
      if (metaTagAttributes.hasKey("http-equiv")
          && metaTagAttributes.get("http-equiv").equalsIgnoreCase("refresh")
          && metaTagAttributes.hasKey("content")) {
        final String[] parts = metaTagAttributes.get("content").replace(" ", "").split("=", 2);
        if (parts.length > 1) {
          final String redirectedUrl = parts[1];
          if (isValidRedirection(url, redirectedUrl)) {
            LOG.debug("Redirection (<meta http-equiv=\"refresh\" .../>): {} -> {}", url,
                redirectedUrl);
            return redirectedUrl;
          }
        }
      }
    }

    return null;
  }

  private String checkForMetaRedirectionOgUrl(final String url,
      final org.jsoup.nodes.Document jsoupDocument) {
    final Elements metaTagsHtmlHeadMeta = jsoupDocument.select("html head meta");

    for (final Element metaTag : metaTagsHtmlHeadMeta) {
      final Attributes metaTagAttributes = metaTag.attributes();
      if (metaTagAttributes.hasKey("property")
          && metaTagAttributes.get("property").equalsIgnoreCase("og:url")
          && metaTagAttributes.hasKey("content")) {
        final String redirectedUrl = metaTagAttributes.get("content").trim();
        if (isValidRedirection(url, redirectedUrl)) {
          LOG.debug("Redirection (<meta property=\"og:url\" .../>): {} -> {}", url, redirectedUrl);
          return redirectedUrl;
        }
      }
    }

    return null;
  }

  private DownloadResult buildDownloadResultOverall(final String url,
      final DownloadResult downloadResultOriginal, final DownloadResult downloadResultRedirected) {
    // Decide for redirected download result, if successful
    final boolean redirectedDownloadSuccess =
        downloadResultRedirected != null && downloadResultRedirected.getContent() != null;
    if (redirectedDownloadSuccess) {
      // Remember original URL
      downloadResultRedirected.setOriginalUrl(url);
      return downloadResultRedirected;
    }

    // Otherwise, decide for original download result
    return downloadResultOriginal;
  }

  private void postProcessContent(final DownloadResult downloadResult) {
    final String content = downloadResult.getContent();
    if (content == null) {
      return;
    }

    // Strip non-valid characters as specified by the XML 1.0 standard
    final String validContent = xmlUtil.stripNonValidXMLCharacters(content);

    // Unescape HTML characters
    final String unescapedContent = StringEscapeUtils.unescapeHtml4(validContent);

    downloadResult.setContent(unescapedContent);
  }

}
