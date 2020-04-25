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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.domain.enums.TagSelectingRuleMode;
import com.romeikat.datamessie.core.domain.util.TagSelector;

@Service
public class TagExctractor {

  private static final Logger LOG = LoggerFactory.getLogger(TagExctractor.class);

  public String extractContent(final List<TagSelectingRule> tagSelectingRules, final String content,
      final Document document) {
    if (content == null) {
      return null;
    }

    // Determine active rules
    final LocalDateTime documentDownloaded = document.getDownloaded();
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
      final String selector = activeTagSelectingRule.getSelector();
      final TagSelectingRuleMode mode = activeTagSelectingRule.getMode();
      final String extractedContent = extractContent(content, document, selector, mode);

      // If successful, done
      if (extractedContent != null) {
        return extractedContent;
      }
    }

    // No match(es) found
    final List<String> selectors = new LinkedList<String>();
    for (final TagSelectingRule activeTagSelectingRule : activeTagSelectingRules) {
      selectors.add(activeTagSelectingRule.getSelector());
    }
    LOG.warn(
        "Could not extract content of document {} ({}) as none of the tag selectors {} of source {} resulted in a unique match",
        document.getId(), document.getUrl(), StringUtils.join(selectors, ", "),
        document.getSourceId());
    return null;
  }

  private String extractContent(final String content, final Document document,
      final String selector, final TagSelectingRuleMode mode) {
    if (selector == null || selector.isEmpty()) {
      return null;
    }

    final String warningMessage = "Could not apply tag selecting rule on document "
        + document.getId() + " (" + document.getUrl() + ") due to malformed tag selector "
        + selector + " of source " + document.getSourceId();

    // Parse selector
    try {
      final TagSelector tagSelector = TagSelector.fromTextualRepresentation(selector);
      if (!tagSelector.isValid()) {
        LOG.warn(warningMessage);
        return null;
      }

      // With selector, search for appropriate element
      final org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(content);
      final List<Element> matchingElements = new ArrayList<Element>();
      final Elements elementsWithTagName = jsoupDocument.getElementsByTag(tagSelector.getTagName());
      for (final Element elementWithTagName : elementsWithTagName) {
        final boolean idNameMatches = tagSelector.checkForIdNameMatch(elementWithTagName.id());
        if (!idNameMatches) {
          continue;
        }

        final boolean classNamesMatch =
            tagSelector.checkForClassNamesMatch(elementWithTagName.classNames());
        if (!classNamesMatch) {
          continue;
        }

        // Match found
        matchingElements.add(elementWithTagName);
      }

      // Match(es) found
      boolean matchFound = false;
      if (mode == TagSelectingRuleMode.EXACTLY_ONCE) {
        matchFound = matchingElements.size() == 1;
      } else if (mode == TagSelectingRuleMode.AT_LEAST_ONCE) {
        matchFound = matchingElements.size() >= 1;
      }
      if (matchFound) {
        final String html =
            matchingElements.stream().map(e -> e.html()).collect(Collectors.joining("<br>"));
        return html;
      }

      // No match(es) found
      return null;
    } catch (final Exception e) {
      LOG.warn(warningMessage, e);
      return null;
    }
  }

}
