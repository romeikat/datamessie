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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.apache.commons.codec.DecoderException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.ExecutionTimeLogger;

public class ContentDownloaderTest extends AbstractTest {

  @Autowired
  private ContentDownloader contentDownloader;

  private ExecutionTimeLogger executionTimeLogger;

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    executionTimeLogger = new ExecutionTimeLogger(getClass());
  }

  @Test
  @Ignore("Can be used to actually download")
  public void downloadContent_success() throws DecoderException {
    final String url = "https://www.google.de/";

    executionTimeLogger.start();
    final DownloadResult downloadResult = contentDownloader.downloadContent(url, null);
    executionTimeLogger.stop();
    executionTimeLogger.log("Downloading");

    assertEquals(url, downloadResult.getUrl());
    assertNotNull(downloadResult.getContent());
    assertNull(downloadResult.getStatusCode());
  }

  @Test
  public void downloadContent_failure() throws DecoderException {
    final String url = "https://www.romeikat.com/this_url_does_not_exist";

    final DownloadResult downloadResult = contentDownloader.downloadContent(url, null);
    assertEquals(url, downloadResult.getUrl());
    assertNull(downloadResult.getContent());
    assertEquals(Integer.valueOf(404), downloadResult.getStatusCode());
  }

  @Test
  @Ignore("Can be used to actually download")
  public void downloadContent_withCookie_success() throws Exception {
    final DownloadSession downloadSession = DownloadSession.create(null, 10000);
    downloadSession.addCookie("zonconsent", null, ".zeit.de");

    final String url =
        "https://www.zeit.de/2020/18/lebensmittelversorgung-landwirtschaft-nahrungsmittel-hungerkrisen-coronavirus-pandemie";
    executionTimeLogger.start();
    final DownloadResult downloadResult = contentDownloader.download(url, 1, downloadSession);
    executionTimeLogger.stop();
    executionTimeLogger.log("Downloading");

    assertNotNull(downloadResult.getContent());
  }

}
