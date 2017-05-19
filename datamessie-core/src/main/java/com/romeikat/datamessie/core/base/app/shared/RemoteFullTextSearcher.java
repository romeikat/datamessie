package com.romeikat.datamessie.core.base.app.shared;

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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;

@Service
public class RemoteFullTextSearcher implements IFullTextSearcher {

  private final static String QUERY_PATH = "/rest/fullTextResult";

  @Value("${fullText.provider.url}")
  private String fullTextProviderUrl;

  @Override
  public int getOrder() {
    return 2;
  }

  @Override
  public FullTextResult searchForCleanedContent(final String luceneQueryString) {
    final String restUrl = getRestUrl();
    if (StringUtils.isBlank(restUrl)) {
      return new FullTextResult();
    }

    final Client client = ClientBuilder.newClient();
    final String fullTextQueryUrl = restUrl + "/cleanedContent/";
    final WebTarget webTarget = client.target(fullTextQueryUrl).path(luceneQueryString);
    final FullTextResult fullTextResult = webTarget.request().get(FullTextResult.class);
    return fullTextResult;
  }

  private String getRestUrl() {
    if (StringUtils.isBlank(fullTextProviderUrl)) {
      return null;
    }

    final StringBuilder restUrl = new StringBuilder();

    // Application path
    restUrl.append(fullTextProviderUrl);

    // Query path
    if (fullTextProviderUrl.endsWith("/")) {
      restUrl.deleteCharAt(restUrl.length() - 1);
    }
    restUrl.append(QUERY_PATH);

    return restUrl.toString();
  }

}
