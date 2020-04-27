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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.rometools.utils.Strings;

public abstract class AbstractDownloader {

  private final static Logger LOG = LoggerFactory.getLogger(AbstractDownloader.class);

  @Value("${crawling.userAgent}")
  private String userAgent;

  @Value("${crawling.timeout}")
  private int timeout;

  protected DownloadResult download(final String url, final int attempts,
      final DownloadSession downloadSession) {
    // Try as many times as desired
    for (int attempt = 0; attempt < attempts; attempt++) {
      final DownloadResult downloadResult = download(url, downloadSession);
      final boolean downloadSuccess = downloadResult.getContent() != null;
      // Return, if successful or last attempt
      if (downloadSuccess || attempt >= attempts - 1) {
        return downloadResult;
      }
    }

    // Fallback
    return new DownloadResult(null, url, null, null, LocalDateTime.now(), null);
  }

  private DownloadResult download(String url, DownloadSession downloadSession) {
    // Resulting information
    String originalUrl = null;
    String content = null;
    Charset charset = null;
    final LocalDateTime downloaded = LocalDateTime.now();
    Integer statusCode = null;

    // HTTP client
    final boolean downloadSessionProvided = downloadSession != null;
    final CloseableHttpClient client;
    if (downloadSessionProvided) {
      client = downloadSession.getClient();
    } else {
      downloadSession = DownloadSession.create(userAgent, timeout);
      client = downloadSession.getClient();
    }
    final HttpClientContext context = HttpClientContext.create();

    // Request
    final HttpGet request = new HttpGet(url);

    // Response
    boolean downloadSuccess = false;
    try (final CloseableHttpResponse response = client.execute(request, context);) {
      final int responseStatusCode = response.getStatusLine().getStatusCode();
      statusCode = responseStatusCode == HttpStatus.SC_OK ? null : responseStatusCode;

      // Content
      final HttpEntity entity = response.getEntity();
      downloadSuccess = responseStatusCode < 400 && entity != null;
      if (downloadSuccess) {
        try (final InputStream urlInputStream = entity.getContent();) {
          content = IOUtils.toString(urlInputStream, StandardCharsets.UTF_8.name());
        }

        // Charset
        final ContentType contentType = ContentType.getOrDefault(entity);
        charset = contentType.getCharset();
      }
    } catch (final IOException e) {
      LOG.debug("Could not download " + url, e);
    }

    // Server-side redirects
    final URI originalUri = request.getURI();
    final HttpHost targetHost = context.getTargetHost();
    final List<URI> redirectLocations = context.getRedirectLocations();
    try {
      final URI finalUri = URIUtils.resolve(originalUri, targetHost, redirectLocations);
      final String responseUrl = finalUri.toASCIIString();
      // Original URL vs. URL
      final String redirectedUrl = getRedirectedUrl(url, responseUrl);
      if (isValidRedirection(url, redirectedUrl)) {
        originalUrl = url;
        url = redirectedUrl;
        LOG.debug("Redirection (server): {} -> {}", originalUrl, url);
      }
    } catch (final URISyntaxException | MalformedURLException e) {
      LOG.debug("Could not download " + url, e);
    }

    // Done
    if (!downloadSuccess) {
      LOG.info("Could not download " + url);
    }
    if (!downloadSessionProvided) {
      downloadSession.close();
    }
    return new DownloadResult(originalUrl, url, content, charset, downloaded, statusCode);
  }

  protected boolean isValidRedirection(final String originalUrl, final String redirectedUrl) {
    return Strings.isNotEmpty(redirectedUrl) && !StringUtils.equals(originalUrl, redirectedUrl);
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

}
