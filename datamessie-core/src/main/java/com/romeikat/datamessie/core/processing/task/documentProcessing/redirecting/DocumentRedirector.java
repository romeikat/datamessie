package com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting;

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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.dao.impl.RedirectingRuleDao;
import com.romeikat.datamessie.core.base.service.download.DownloadResult;
import com.romeikat.datamessie.core.base.service.download.Downloader;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;

@Service
public class DocumentRedirector {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentRedirector.class);

  @Autowired
  private Downloader downloader;

  @Autowired
  @Qualifier("redirectingRuleDao")
  private RedirectingRuleDao redirectingRuleDao;

  private DocumentRedirector() {}

  public DocumentRedirectingResult redirect(final StatelessSession statelessSession, final Document document,
      final RawContent rawContent) throws Exception {
    // Prio 1: Use hard-coded redirecting rule
    String redirectedUrl = applyHardCodedRedirectingRule(rawContent);

    // Prio 2: Use redirecting rules specified by the user
    if (redirectedUrl == null) {
      redirectedUrl = applyUserDefinedRedirectingRules(statelessSession, document, rawContent);
    }

    // Download redirected URL, if one was found
    final boolean wasRedirectingUrlFound = redirectedUrl != null;
    final DownloadResult redirectedDownloadResult =
        wasRedirectingUrlFound ? downloader.downloadContent(redirectedUrl) : null;

    // Done
    return new DocumentRedirectingResult(redirectedUrl, redirectedDownloadResult);
  }

  private String applyHardCodedRedirectingRule(final RawContent rawContent) {
    // Parse raw content
    final org.jsoup.nodes.Document jsoupDocument = Jsoup.parse(rawContent.getContent());

    final String title = jsoupDocument.title();
    final boolean documentTitleMatches = title.equalsIgnoreCase("advertisment");
    if (documentTitleMatches) {
      // Map link target -> number of occurrence
      final Map<String, Integer> linkCounts = new HashMap<String, Integer>();
      // Count link occurrences
      final Elements links = jsoupDocument.select("a[href]");
      for (final Element link : links) {
        final String linkTarget = link.attr("href");
        Integer linkCount = linkCounts.get(linkTarget);
        if (linkCount == null) {
          linkCount = 0;
        }
        linkCount++;
        linkCounts.put(linkTarget, linkCount);
      }
      // Get most frequent link (for multiple highest link counts, use the "lower" link URL)
      String mostFrequentLinkTarget = null;
      int mostFrequentLinkCount = 0;
      for (final String linkTarget : linkCounts.keySet()) {
        final int linkCount = linkCounts.get(linkTarget);
        if (linkCount > mostFrequentLinkCount || linkCount == mostFrequentLinkCount
            && linkTarget.toLowerCase().compareTo(mostFrequentLinkTarget.toLowerCase()) < 0) {
          mostFrequentLinkTarget = linkTarget;
          mostFrequentLinkCount = linkCount;
        }
      }
      // Use most frequent link, if one was found
      if (mostFrequentLinkTarget != null) {
        return mostFrequentLinkTarget;
      }
    }

    // No redirecting
    return null;
  }

  private String applyUserDefinedRedirectingRules(final StatelessSession statelessSession, final Document document,
      final RawContent rawContent) {
    // Determine rules
    final List<RedirectingRule> redirectingRules = getRedirectingRules(statelessSession, document);

    // Process rules one after another, until URL for redirection is found
    for (final RedirectingRule redirectingRule : redirectingRules) {
      final String redirectedUrl = getRedirectedUrl(rawContent.getContent(), redirectingRule);
      if (StringUtils.isNotBlank(redirectedUrl)) {
        return redirectedUrl;
      }
    }

    // No URL was found
    return null;
  }

  private List<RedirectingRule> getRedirectingRules(final StatelessSession statelessSession, final Document document) {
    // Rules of source
    final List<RedirectingRule> redirectingRules =
        redirectingRuleDao.getOfSource(statelessSession, document.getSourceId());

    // Active rules
    final LocalDate downloadDate = document.getDownloaded().toLocalDate();
    final List<RedirectingRule> activeRedirectingRules = new LinkedList<RedirectingRule>();
    for (final RedirectingRule redirectingRule : redirectingRules) {
      if (redirectingRule.isActive(downloadDate)) {
        activeRedirectingRules.add(redirectingRule);
      }
    }

    return activeRedirectingRules;
  }

  private String getRedirectedUrl(final String content, final RedirectingRule redirectingRule) {
    // Extract URL with regex
    final String regex = redirectingRule.getRegex();
    if (StringUtils.isBlank(regex)) {
      return null;
    }

    final Integer group = redirectingRule.getRegexGroup();
    if (group == null) {
      return null;
    }

    final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    final Matcher matcher = pattern.matcher(content);
    // If an URL can be extracted, use the extracted URL instead
    if (matcher.find()) {
      try {
        final String redirectedUrl = matcher.group(group);
        return redirectedUrl;
      } catch (final Exception e) {
        LOG.warn("Could not extradt redirected URL with regex {} and group {}", regex, group);
      }
    }

    return null;
  }

}
