package com.romeikat.datamessie.core.statistics.service;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

  private static final Logger LOG = LoggerFactory.getLogger(SmsService.class);

  private static final String OUTPUT_SUCCESS = "100";

  @Value("${statistics.checking.sms.apikey}")
  private String statisticsCheckingSmsApikey;

  @Value("${statistics.checking.sms.from}")
  private String statisticsCheckingSmsFrom;

  @Value("${statistics.checking.sms.to}")
  private String statisticsCheckingSmsTo;

  private SmsService() {}

  public String sendSms(final String msg) {
    try {
      final String text = URLEncoder.encode(msg, "UTF-8");
      final String urlString = String.format(
          "https://gateway.sms77.io/api/sms?p=%s&from=%s&to=%s&text=%s",
          statisticsCheckingSmsApikey, statisticsCheckingSmsFrom, statisticsCheckingSmsTo, text);

      final String output = getOutput(urlString);
      if (StringUtils.startsWith(output, OUTPUT_SUCCESS)) {
        LOG.debug("SMS sent successfully with output {}", output);
      } else {
        LOG.warn("SMS sent with error code {}", output);
      }
      return output;
    } catch (final Exception e) {
      LOG.error("Could not send SMS", e);
      return null;
    }
  }

  private static String getOutput(final String urlString) throws IOException {
    // Connect
    final URL url = new URL(urlString);
    final URLConnection urlConnection = url.openConnection();

    // Read
    final BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    final StringBuilder result = new StringBuilder();
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      result.append(line + "\n");
    }
    bufferedReader.close();

    // Done
    return result.toString();
  }

}
