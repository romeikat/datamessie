package com.romeikat.datamessie.core.processing.app.shared;

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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;

@Path("/rest/fullTextResult")
public class RemoteFullTextProvider {

  private static final int RESPONSE_STATUS = 200;

  @Autowired
  private LocalFullTextSearcher localFullTextSearcher;

  @GET
  @Path("/cleanedContent/{query}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response getContentIds(@PathParam("query") final String luceneQueryString) {
    final FullTextResult fullTextResult =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    final Response response = buildResponse(fullTextResult);
    return response;
  }

  private Response buildResponse(final FullTextResult fullTextResult) {
    final Response response = Response.status(RESPONSE_STATUS).entity(fullTextResult).build();
    return response;
  }

}
