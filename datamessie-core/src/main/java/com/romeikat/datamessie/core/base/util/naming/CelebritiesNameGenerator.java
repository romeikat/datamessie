package com.romeikat.datamessie.core.base.util.naming;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.task.management.TaskManager;

@Service
public class CelebritiesNameGenerator implements NameGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private static final String URL = "https://www.randomlists.com/data/celebrities.json";

  @Override
  public String generateName() {
    final JsonObject json = getJsonObject();
    if (json == null) {
      return null;
    }

    final JsonArray celebrities = getJsonCelebrities(json);
    final JsonValue celebrity = getRandomCelebrity(celebrities);
    final String celebrityName = getCelebrityName(celebrity);
    return celebrityName;
  }

  private JsonObject getJsonObject() {
    final URL urlAsUrl;
    try {
      urlAsUrl = new URL(URL);
    } catch (final MalformedURLException e) {
      LOG.error("Could not create URL", e);
      return null;
    }

    try (BufferedReader in = new BufferedReader(new InputStreamReader(urlAsUrl.openStream()));
        JsonReader jsonReader = Json.createReader(in);) {
      final JsonObject jsonObject = jsonReader.readObject();
      return jsonObject;
    } catch (final IOException e) {
      LOG.error("Could not read JSON object", e);
      return null;
    }
  }

  private JsonArray getJsonCelebrities(final JsonObject celebrities) {
    final JsonObject randL = celebrities.getJsonObject("RandL");
    final JsonArray items = randL.getJsonArray("items");
    return items;
  }

  private JsonValue getRandomCelebrity(final JsonArray items) {
    final int size = items.size();

    final Random r = new Random();
    final int randomIndex = r.nextInt(size);

    final JsonValue jsonValue = items.get(randomIndex);
    return jsonValue;
  }

  private String getCelebrityName(final JsonValue celebrity) {
    String celebrityContent = celebrity.toString();

    if (celebrityContent.startsWith("\"")) {
      celebrityContent = celebrityContent.substring(1);
    }
    if (celebrityContent.endsWith("\"")) {
      celebrityContent = celebrityContent.substring(0, celebrityContent.length() - 1);
    }

    return celebrityContent;
  }

}
