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
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.XmlUtil;

@Service
public class ContentDownloader extends AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(ContentDownloader.class);

  @Autowired
  private XmlUtil xmlUtil;

  public DownloadResult downloadContent(String url) {
    LOG.debug("Downloading content from {}", url);
    // In case of a new redirection for that source, use redirected URL
    URLConnection urlConnection = null;
    String originalUrl = null;
    org.jsoup.nodes.Document jsoupDocument = null;
    Integer statusCode = null;
    final LocalDateTime downloaded = LocalDateTime.now();
    try {
      urlConnection = getConnection(url);
      // Server-side redirection
      final String responseUrl = getResponseUrl(urlConnection);
      if (responseUrl != null) {
        final String redirectedUrl = getRedirectedUrl(url, responseUrl);
        if (isValidRedirection(url, redirectedUrl)) {
          originalUrl = url;
          url = redirectedUrl;
          closeUrlConnection(urlConnection);
          urlConnection = getConnection(url);
          LOG.debug("Redirection (server): {} -> {}", originalUrl, url);
        }
      }
      // Download content for further redirects
      final InputStream urlInputStream = asInputStream(urlConnection, true, false);
      final Charset charset = getCharset(urlConnection);
      jsoupDocument = Jsoup.parse(urlInputStream, charset.name(), url);
      final Elements metaTagsHtmlHeadLink;
      Elements metaTagsHtmlHeadMeta = null;
      // Meta redirection (<link rel="canonical" .../>)
      if (originalUrl == null) {
        metaTagsHtmlHeadLink = jsoupDocument.select("html head link");
        for (final Element metaTag : metaTagsHtmlHeadLink) {
          final Attributes metaTagAttributes = metaTag.attributes();
          if (metaTagAttributes.hasKey("rel")
              && metaTagAttributes.get("rel").equalsIgnoreCase("canonical")
              && metaTagAttributes.hasKey("href")) {
            final String redirectedUrl = metaTagAttributes.get("href").trim();
            if (isValidRedirection(url, redirectedUrl)) {
              originalUrl = url;
              url = redirectedUrl;
              jsoupDocument = null;
              LOG.debug("Redirection (<link rel=\"canonical\" .../>): {} -> {}", originalUrl, url);
              break;
            }
          }
        }
      }
      // Meta redirection (<meta http-equiv="refresh" .../>)
      if (originalUrl == null) {
        metaTagsHtmlHeadMeta = jsoupDocument.select("html head meta");
        for (final Element metaTag : metaTagsHtmlHeadMeta) {
          final Attributes metaTagAttributes = metaTag.attributes();
          if (metaTagAttributes.hasKey("http-equiv")
              && metaTagAttributes.get("http-equiv").equalsIgnoreCase("refresh")
              && metaTagAttributes.hasKey("content")) {
            final String[] parts = metaTagAttributes.get("content").replace(" ", "").split("=", 2);
            if (parts.length > 1) {
              final String redirectedUrl = parts[1];
              if (isValidRedirection(url, redirectedUrl)) {
                originalUrl = url;
                url = redirectedUrl;
                jsoupDocument = null;
                LOG.debug("Redirection (<meta http-equiv=\"refresh\" .../>): {} -> {}", originalUrl,
                    url);
                break;
              }
            }
          }
        }
      }
      // Meta redirection (<meta property="og:url" .../>)
      if (originalUrl == null) {
        for (final Element metaTag : metaTagsHtmlHeadMeta) {
          final Attributes metaTagAttributes = metaTag.attributes();
          if (metaTagAttributes.hasKey("property")
              && metaTagAttributes.get("property").equalsIgnoreCase("og:url")
              && metaTagAttributes.hasKey("content")) {
            final String redirectedUrl = metaTagAttributes.get("content").trim();
            if (isValidRedirection(url, redirectedUrl)) {
              originalUrl = url;
              url = redirectedUrl;
              jsoupDocument = null;
              LOG.debug("Redirection (<meta property=\"og:url\" .../>): {} -> {}", originalUrl,
                  url);
              break;
            }
          }
        }
      }
    } catch (final Exception e) {
      if (e instanceof HttpStatusException) {
        statusCode = ((HttpStatusException) e).getStatusCode();
      }
      LOG.warn("Could not determine redirected URL for " + url, e);
    } finally {
      closeUrlConnection(urlConnection);
    }
    // Download content (if not yet done)
    String content = null;
    try {
      if (jsoupDocument == null) {
        LOG.debug("Downloading content from {}", url);
        urlConnection = getConnection(url);
        final InputStream urlInputStream = asInputStream(urlConnection, true, false);
        final Charset charset = getCharset(urlConnection);
        jsoupDocument = Jsoup.parse(urlInputStream, charset.name(), url);
      }
    } catch (final Exception e) {
      if (e instanceof HttpStatusException) {
        statusCode = ((HttpStatusException) e).getStatusCode();
      }
      // If the redirected URL does not exist, use the original URL instead
      if (originalUrl == null) {
        LOG.warn("Could not download content from " + url, e);
      }
      // If the redirected URL does not exist and a original URL is available, use the
      // original URL instead
      else {
        try {
          LOG.debug(
              "Could not download content from redirected URL {}, downloading content from original URL {} instead",
              url, originalUrl);
          urlConnection = getConnection(originalUrl);
          final InputStream urlInputStream = asInputStream(urlConnection, true, false);
          final Charset charset = getCharset(urlConnection);
          jsoupDocument = Jsoup.parse(urlInputStream, charset.name(), url);
          url = originalUrl;
          originalUrl = null;
          statusCode = null;
        } catch (final Exception e2) {
          LOG.warn("Could not download content from original URL " + url, e);
        }
      }
    } finally {
      closeUrlConnection(urlConnection);
    }
    if (jsoupDocument != null) {
      content = jsoupDocument.html();
    }
    // Strip non-valid characters as specified by the XML 1.0 standard
    final String validContent = xmlUtil.stripNonValidXMLCharacters(content);
    // Unescape HTML characters
    final String unescapedContent = StringEscapeUtils.unescapeHtml4(validContent);
    // Done
    final DownloadResult downloadResult =
        new DownloadResult(originalUrl, url, unescapedContent, downloaded, statusCode);
    return downloadResult;
  }

}
