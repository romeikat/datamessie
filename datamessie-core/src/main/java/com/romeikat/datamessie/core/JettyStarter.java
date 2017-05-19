package com.romeikat.datamessie.core;

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

import java.io.File;

import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyStarter {

  private static final String CONTEXT_PATH = "/datamessie";
  private static final int PORT = 8080;
  private static final int SECURE_PORT = 8443;
  private static final int OUTOUT_BUFFER_SIZE = 32768;
  private static final long TIMEOUT = (int) Duration.ONE_HOUR.getMilliseconds();

  public static void main(final String[] args) throws Exception {
    final Server server = new Server();

    final HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecureScheme("https");
    httpConfig.setSecurePort(SECURE_PORT);
    httpConfig.setOutputBufferSize(OUTOUT_BUFFER_SIZE);

    final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    http.setPort(PORT);
    http.setIdleTimeout(TIMEOUT);

    server.setConnectors(new Connector[] {http});

    final WebAppContext webapp = new WebAppContext();
    webapp.setContextPath(CONTEXT_PATH);
    final File warFile = new File("src/main/webapp/");
    webapp.setWar(warFile.getAbsolutePath());
    webapp.addAliasCheck(new AllowSymLinkAliasChecker());
    server.setHandler(webapp);

    server.start();
    server.join();
  }

}
