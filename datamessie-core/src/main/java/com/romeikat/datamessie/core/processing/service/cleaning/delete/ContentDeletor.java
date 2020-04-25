package com.romeikat.datamessie.core.processing.service.cleaning.delete;

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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DeletingRuleMode;
import com.romeikat.datamessie.core.domain.util.TagSelector;

@Service
public class ContentDeletor {

  public String deleteContent(final List<DeletingRule> deletingRules, final String content,
      final Document document) {
    if (content == null) {
      return null;
    }

    // Apply deleting rules
    final LocalDateTime documentDownloaded = document.getDownloaded();
    // Determine active rules
    final List<DeletingRule> activeDeletingRules = new LinkedList<DeletingRule>();
    for (final DeletingRule deletingRule : deletingRules) {
      if (deletingRule.isActive(documentDownloaded.toLocalDate())) {
        activeDeletingRules.add(deletingRule);
      }
    }

    // Without active rules, use whole document
    if (activeDeletingRules.isEmpty()) {
      return content;
    }

    // Process active rules one after another
    String currentContent = content;
    for (final DeletingRule activeDeletingRule : activeDeletingRules) {
      final String selector = activeDeletingRule.getSelector();
      final DeletingRuleMode deletingRuleMode = activeDeletingRule.getMode();
      if (deletingRuleMode == DeletingRuleMode.REGEX) {
        currentContent = deleteContentWithRegex(currentContent, selector);
      } else if (deletingRuleMode == DeletingRuleMode.TAG) {
        currentContent = deleteContentWithTag(currentContent, selector);
      }
    }

    // Done
    return currentContent;
  }

  private String deleteContentWithRegex(final String content, final String selector) {
    if (StringUtils.isBlank(selector)) {
      return content;
    }

    final Pattern pattern = Pattern.compile(selector, Pattern.DOTALL);
    final Matcher matcher = pattern.matcher(content);
    final String result = matcher.replaceAll("");
    return result;
  }

  private String deleteContentWithTag(final String content, final String selector) {
    if (StringUtils.isBlank(selector)) {
      return content;
    }

    final TagSelector tagSelector = TagSelector.fromTextualRepresentation(selector);
    if (!tagSelector.isValid()) {
      return content;
    }

    // With selector, search for appropriate element
    final org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(content);
    final Elements elementsWithTagName = jsoupDocument.getElementsByTag(tagSelector.getTagName());
    boolean modified = false;
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

      // Remove tag
      elementWithTagName.remove();
      modified = true;
    }

    if (modified) {
      return jsoupDocument.outerHtml();
    } else {
      return content;
    }
  }

}
