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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadSession implements AutoCloseable {

  private static final int MAX_CONNECTIONS_TOTAL = 200;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 20;

  private final static Logger LOG = LoggerFactory.getLogger(DownloadSession.class);

  private final BasicCookieStore cookieStore;
  private final CloseableHttpClient client;

  private DownloadSession(final String userAgent, final int timeout) {
    cookieStore = createCookieStore();
    client = createClient(userAgent, timeout, cookieStore);
  }

  public static DownloadSession create(final String userAgent, final int timeout) {
    return new DownloadSession(userAgent, timeout);
  }

  public void addCookie(final String name, final String value, final String domain) {
    final BasicClientCookie cookie = createCookie(name, value, domain);
    cookieStore.addCookie(cookie);
  }

  public void addCookie(final String cookie) {
    if (StringUtils.isBlank(cookie)) {
      return;
    }

    final String[] parts = cookie.split("#");

    // Name
    String name = null;
    if (StringUtils.isNotBlank(parts[0])) {
      name = parts[0];
    }

    // Value
    String value = null;
    if (parts.length >= 2) {
      if (StringUtils.isNotBlank(parts[1])) {
        value = parts[1];
      }
    }

    // Domain
    String domain = null;
    if (parts.length >= 3) {
      if (StringUtils.isNotBlank(parts[2])) {
        domain = parts[2];
      }
    }

    addCookie(name, value, domain);
  }

  @Override
  public void close() {
    if (client != null) {
      try {
        client.close();
      } catch (final IOException e) {
        LOG.error("Could not close the HTTP client", e);
      }
    }
  }

  private static CloseableHttpClient createClient(final String userAgent, final int timeout,
      final BasicCookieStore cookieStore) {
    final HttpClientConnectionManager connectionManager = createConnectionManager();
    final RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout)
        .setConnectionRequestTimeout(timeout).setSocketTimeout(timeout).build();
    final CloseableHttpClient client =
        HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(config)
            .setUserAgent(userAgent).setDefaultCookieStore(cookieStore).build();
    return client;
  }

  private static HttpClientConnectionManager createConnectionManager() {
    final PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(MAX_CONNECTIONS_TOTAL);
    connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
    return connectionManager;
  }

  private static BasicCookieStore createCookieStore() {
    final BasicCookieStore cookieStore = new BasicCookieStore();
    return cookieStore;
  }

  private static BasicClientCookie createCookie(final String name, final String value,
      final String domain) {
    final BasicClientCookie cookie = new BasicClientCookie(name, value);

    // Set effective domain
    cookie.setDomain(domain);
    // Set domain exactly as sent by the server
    cookie.setAttribute(ClientCookie.DOMAIN_ATTR, domain);

    return cookie;
  }

  public CloseableHttpClient getClient() {
    return client;
  }

}
