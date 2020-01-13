package com.romeikat.datamessie.core.processing.service.cleaning.extract;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;

@Service
public class TagExctractor {

  private static final Logger LOG = LoggerFactory.getLogger(TagExctractor.class);

  public String extractContent(final List<TagSelectingRule> tagSelectingRules, final String content,
      final Document document) {
    if (content == null) {
      return null;
    }
    // Apply tag selecting rules
    final LocalDateTime documentDownloaded = document.getDownloaded();
    // Determine active rules
    final List<TagSelectingRule> activeTagSelectingRules = new LinkedList<TagSelectingRule>();
    for (final TagSelectingRule tagSelectingRule : tagSelectingRules) {
      if (tagSelectingRule.isActive(documentDownloaded.toLocalDate())) {
        activeTagSelectingRules.add(tagSelectingRule);
      }
    }
    // Without active rules, use whole document
    if (activeTagSelectingRules.isEmpty()) {
      return content;
    }
    // Process active rules one after another, until tag selection is successful
    for (final TagSelectingRule activeTagSelectingRule : activeTagSelectingRules) {
      // Extract content with tag selector
      final String tagSelector = activeTagSelectingRule.getTagSelector();
      final String extractedContent = extractContent(content, document, tagSelector);
      // If successful, done
      if (extractedContent != null) {
        return extractedContent;
      }
    }
    // No unique tag found
    final List<String> tagSelectors = new LinkedList<String>();
    for (final TagSelectingRule activeTagSelectingRule : activeTagSelectingRules) {
      tagSelectors.add(activeTagSelectingRule.getTagSelector());
    }
    LOG.warn(
        "Could not extract content of document {} ({}) as none of the tag selectors {} of source {} resulted in a unique match",
        document.getId(), document.getUrl(), StringUtils.join(tagSelectors, ", "),
        document.getSourceId());
    return null;
  }

  private String extractContent(final String content, final Document document,
      final String tagSelector) {
    if (tagSelector == null || tagSelector.isEmpty()) {
      return null;
    }
    // Parse tag selector
    String tagName = null;
    String idName = null;
    Set<String> classNames = null;
    boolean exactClassNamesMatch = false;
    final String warningMessage = "Could not apply tag selecting rule on document "
        + document.getId() + " (" + document.getUrl() + ") due to malformed tag selector "
        + tagSelector + " of source " + document.getSourceId();
    try {
      final String[] parts = tagSelector.split("#");
      tagName = parts[0];
      if (tagName.isEmpty()) {
        tagName = null;
      }
      if (parts.length >= 2) {
        idName = parts[1];
        if (idName.isEmpty()) {
          idName = null;
        }
      }
      if (parts.length >= 3) {
        exactClassNamesMatch = parts[2].startsWith("\"") && parts[2].endsWith("\"");
        final String classDefinition =
            exactClassNamesMatch ? parts[2].substring(1, parts[2].length() - 1) : parts[2];
        classNames = Sets.newHashSet(classDefinition.split("\\s+"));
      }
      if (tagName == null || idName == null && classNames == null) {
        LOG.warn(warningMessage);
        return null;
      }
      // With tag selector, search for appropriate element
      final org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(content);
      final List<Element> matchingElements = new ArrayList<Element>();
      final Elements elementsWithTagName = jsoupDocument.getElementsByTag(tagName);
      for (final Element elementWithTagName : elementsWithTagName) {
        final boolean idNameMatches = idName == null || elementWithTagName.id().equals(idName);
        final boolean classNamesMatch;
        if (exactClassNamesMatch) {
          classNamesMatch =
              classNames == null || elementWithTagName.classNames().equals(classNames);
        } else {
          classNamesMatch =
              classNames == null || elementWithTagName.classNames().containsAll(classNames);
        }
        if (idNameMatches && classNamesMatch) {
          matchingElements.add(elementWithTagName);
        }
      }
      // Unique match found
      if (matchingElements.size() == 1) {
        final Element matchingElement = matchingElements.get(0);
        return matchingElement.html();
      }
      // No unique match found
      return null;
    } catch (final Exception e) {
      LOG.warn(warningMessage, e);
      return null;
    }
  }

}
