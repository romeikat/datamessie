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
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.romeikat.datamessie.core.base.util.XmlUtil;

public abstract class AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(AbstractDownloader.class);

  @Value("${crawling.userAgent}")
  private String userAgent;

  @Value("${crawling.timeout}")
  private int timeout;

  @Autowired
  private XmlUtil xmlUtil;

  protected boolean isValidRedirection(final String originalUrl, final String redirectedUrl) {
    return redirectedUrl != null && !redirectedUrl.isEmpty() && !originalUrl.contains(redirectedUrl)
        && !redirectedUrl.contains(originalUrl);
  }

  protected URLConnection getConnection(final String url) throws IOException {
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

  protected InputStream asInputStream(final URLConnection urlConnection,
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

  protected Charset getCharset(final URLConnection urlConnection) {
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

  protected String getResponseUrl(final URLConnection urlConnection) throws IOException {
    if (!(urlConnection instanceof HttpURLConnection)) {
      return urlConnection.getURL().toExternalForm();
    }
    final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
    httpUrlConnection.setInstanceFollowRedirects(false);
    final String responseUrl = httpUrlConnection.getHeaderField("Location");
    return responseUrl;
  }

  protected String getRedirectedUrl(final String url, final String responseUrl)
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

  protected String getHost(final String url) {
    final URL urlAsUrl;
    try {
      urlAsUrl = new URL(url);
    } catch (final Exception e) {
      return null;
    }

    final String host = urlAsUrl.getHost();
    return host;
  }

  protected String getProtocol(final String url) {
    final URL urlAsUrl;
    try {
      urlAsUrl = new URL(url);
    } catch (final Exception e) {
      return null;
    }

    final String protocol = urlAsUrl.getProtocol();
    return protocol;
  }

  protected void closeUrlConnection(final URLConnection urlConnection) {
    if (urlConnection == null) {
      return;
    }
    if (urlConnection instanceof HttpURLConnection) {
      final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnection;
      httpUrlConnection.disconnect();
    }
  }

}
