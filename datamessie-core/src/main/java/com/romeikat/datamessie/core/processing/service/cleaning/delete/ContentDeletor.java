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
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Document;

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
      final String regex = activeDeletingRule.getRegex();
      currentContent = deleteContent(currentContent, regex);
    }

    // Done
    return currentContent;
  }

  private String deleteContent(final String content, final String regex) {
    if (StringUtils.isBlank(regex)) {
      return content;
    }

    final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    final Matcher matcher = pattern.matcher(content);
    final String result = matcher.replaceAll("");
    return result;
  }

}
