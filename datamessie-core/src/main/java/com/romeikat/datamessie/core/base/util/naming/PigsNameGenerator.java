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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import jersey.repackaged.com.google.common.collect.Lists;

@Service
public class PigsNameGenerator implements NameGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  private static final String URL = "https://petpress.net/pet-pig-names/";

  private List<String> nameSequence;
  private int currentIndex = 0;

  @PostConstruct
  private void initialize() {
    LOG.info("Initializing pig names");

    final Set<String> names = extractNames(URL);
    nameSequence = shuffleNames(names);
  }

  private Set<String> extractNames(final String url) {
    Document d;
    try {
      d = Jsoup.connect(url).get();
    } catch (final IOException e) {
      LOG.warn("Cannot extract pig names", e);
      return null;
    }

    final Elements listItems = d.select("div[itemprop=articleBody] > ul > li");

    final Set<String> names = Sets.newHashSetWithExpectedSize(listItems.size());
    for (final Element listItem : listItems) {
      final String name = listItem.text();
      names.add(name);
    }

    return names;
  }

  private List<String> shuffleNames(final Set<String> names) {
    if (names == null) {
      return null;
    }

    final List<String> result = Lists.newArrayList(names);
    Collections.shuffle(result);
    return result;
  }

  @Override
  public synchronized String generateName() {
    if (CollectionUtils.isEmpty(nameSequence)) {
      return null;
    }

    final String name = chooseNextName();
    return name;
  }

  private String chooseNextName() {
    if (currentIndex >= nameSequence.size()) {
      currentIndex = 0;
    }

    return nameSequence.get(currentIndex++);
  }

}
