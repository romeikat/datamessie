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
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.romeikat.datamessie.core.AbstractTest;

@Ignore("Testing should not depend on Internet connectivity")
public class ContentDownloaderTest extends AbstractTest {

  @Autowired
  private ContentDownloader contentDownloader;

  @Test
  public void downloadContent_success() throws DecoderException {
    final String url = "https://www.google.de";

    final DownloadResult downloadResult = contentDownloader.downloadContent(url);
    assertEquals(url, downloadResult.getUrl());
    assertNotNull(downloadResult.getContent());
    assertNull(downloadResult.getStatusCode());
  }

  @Test
  public void downloadContent_failure() throws DecoderException {
    final String url = "https://www.romeikat.com/this_url_does_not_exist";

    final DownloadResult downloadResult = contentDownloader.downloadContent(url);
    assertEquals(url, downloadResult.getUrl());
    assertNull(downloadResult.getContent());
    assertNull(downloadResult.getStatusCode());
  }

}
