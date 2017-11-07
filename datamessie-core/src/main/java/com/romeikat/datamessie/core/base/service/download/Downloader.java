package com.romeikat.datamessie.core.base.service.download;

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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
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
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Service
public class Downloader {

  private final static Logger LOG = LoggerFactory.getLogger(Downloader.class);

  private static final boolean ALLOW_DOCTYPES = true;

  @Value("${crawling.userAgent}")
  private String userAgent;

  @Value("${crawling.timeout}")
  private int timeout;

  @Autowired
  private XmlUtil xmlUtil;

  public SyndFeed downloadRssFeed(final String sourceUrl) {
    LOG.debug("Downloading content from {}", sourceUrl);
    // Download source
    XmlReader xmlReader = null;
    URLConnection urlConnection = null;
    SyndFeed syndFeed = null;
    try {
      urlConnection = getConnection(sourceUrl);
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

  private boolean isValidRedirection(final String originalUrl, final String redirectedUrl) {
    return redirectedUrl != null && !redirectedUrl.isEmpty() && !originalUrl.contains(redirectedUrl)
        && !redirectedUrl.contains(originalUrl);
  }

  private URLConnection getConnection(final String url) throws IOException {
    final URLConnection urlConnection = new URL(url).openConnection();
    if (urlConnection instanceof HttpURLConnection) {
      final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
      httpUrlConnection.setInstanceFollowRedirects(true);
    }
    urlConnection.setConnectTimeout(timeout);
    urlConnection.setReadTimeout(timeout);
    urlConnection.setRequestProperty("User-Agent", userAgent);
    return urlConnection;
  }

  private InputStream asInputStream(final URLConnection urlConnection,
      final boolean stripNonValidXMLCharacters, final boolean unescapeHtml4) throws Exception {
    final InputStream urlInputStream = urlConnection.getInputStream();
    final Charset urlCharset = getCharset(urlConnection);
    final InputStreamReader urlInputStreamReader =
        new InputStreamReader(urlInputStream, urlCharset);
    final BufferedReader urlBufferedReader = new BufferedReader(urlInputStreamReader);
    // Read lines
    final StringBuilder sb = new StringBuilder();
    String line;
    while ((line = urlBufferedReader.readLine()) != null) {
      sb.append(line + "\n");
    }
    urlBufferedReader.close();
    // Strip non-valid characters as specified by the XML 1.0 standard
    String content = sb.toString();
    if (stripNonValidXMLCharacters) {
      content = xmlUtil.stripNonValidXMLCharacters(content);
    }
    // Unescape HTML characters
    if (unescapeHtml4) {
      content = StringEscapeUtils.unescapeHtml4(content);
    }
    // Return as stream
    return new ByteArrayInputStream(content.getBytes(urlCharset.name()));
  }

  private Charset getCharset(final URLConnection urlConnection) {
    Charset charset = null;
    // Prio 1: content encoding specified in header
    String encoding = urlConnection.getContentEncoding();
    // Prio 2: charset specified within content type (specified in header)
    if (encoding == null) {
      final String contentType = urlConnection.getHeaderField("Content-Type");
      if (contentType != null) {
        for (final String metaTagParameter : contentType.replace(" ", "").split(";")) {
          if (metaTagParameter.startsWith("charset=")) {
            encoding = metaTagParameter.split("=", 2)[1];
            break;
          }
        }
      }
    }
    // Prio 3: default charset
    if (encoding == null) {
      return Charset.defaultCharset();
    }
    // Create charset
    try {
      charset = Charset.forName(encoding);
    } catch (final Exception ex) {
      LOG.warn(
          "Unsupported encoding " + encoding + " by " + urlConnection.getURL().toExternalForm(),
          ex);
    }
    // Fallback
    if (charset == null) {
      charset = Charset.defaultCharset();
    }
    // Done
    return charset;
  }

  private String getResponseUrl(final URLConnection urlConnection) throws IOException {
    if (!(urlConnection instanceof HttpURLConnection)) {
      return urlConnection.getURL().toExternalForm();
    }
    final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
    httpUrlConnection.setInstanceFollowRedirects(false);
    final String responseUrl = httpUrlConnection.getHeaderField("Location");
    return responseUrl;
  }

  private String getRedirectedUrl(final String url, final String responseUrl)
      throws MalformedURLException {
    String result = responseUrl;

    // Prepend host, if necessary
    final String responseUrlHost = getHost(responseUrl);
    if (StringUtils.isBlank(responseUrlHost)) {
      final String urlHost = getHost(url);
      if (StringUtils.isNotBlank(urlHost)) {
        result = urlHost + result;
      }
    }

    // Prepend protocol, if necessary
    final String responseUrlProtocol = getProtocol(responseUrl);
    if (StringUtils.isBlank(responseUrlProtocol)) {
      final String urlProtocol = getProtocol(url);
      if (StringUtils.isNotBlank(urlProtocol)) {
        result = urlProtocol + "://" + result;
      }
    }

    return result;
  }

  private String getHost(final String url) {
    final URL urlAsUrl;
    try {
      urlAsUrl = new URL(url);
    } catch (final Exception e) {
      return null;
    }

    final String host = urlAsUrl.getHost();
    return host;
  }

  private String getProtocol(final String url) {
    final URL urlAsUrl;
    try {
      urlAsUrl = new URL(url);
    } catch (final Exception e) {
      return null;
    }

    final String protocol = urlAsUrl.getProtocol();
    return protocol;
  }

  private void closeUrlConnection(final URLConnection urlConnection) {
    if (urlConnection == null) {
      return;
    }
    if (urlConnection instanceof HttpURLConnection) {
      final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
      httpUrlConnection.disconnect();
    }
  }

}
