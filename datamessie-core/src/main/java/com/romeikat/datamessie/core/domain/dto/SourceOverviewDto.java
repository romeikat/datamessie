package com.romeikat.datamessie.core.domain.dto;

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

import java.io.Serializable;
import java.util.List;
import com.romeikat.datamessie.core.domain.enums.Language;

public class SourceOverviewDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROTOCOL1 = "http://";
  private static final String PROTOCOL2 = "https://";

  private Long id;

  private String name;

  private Language language;

  private List<SourceTypeDto> types;

  private String url;

  private String userAgent;

  private String cookie;

  private Integer numberOfRedirectingRules;

  private Integer numberOfDeletingRules;

  private Integer numberOfTagSelectingRules;

  private boolean crawlingEnabled;

  private boolean visible;

  private boolean statisticsChecking;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(final Language language) {
    this.language = language;
  }

  public List<SourceTypeDto> getTypes() {
    return types;
  }

  public void setTypes(final List<SourceTypeDto> types) {
    this.types = types;
  }

  public String getUrl() {
    if (url != null && !url.startsWith(PROTOCOL1) && !url.startsWith(PROTOCOL2)) {
      return PROTOCOL1 + url;
    }
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(final String userAgent) {
    this.userAgent = userAgent;
  }

  public String getCookie() {
    return cookie;
  }

  public void setCookie(final String cookie) {
    this.cookie = cookie;
  }

  public Integer getNumberOfRedirectingRules() {
    return numberOfRedirectingRules;
  }

  public void setNumberOfRedirectingRules(final Integer numberOfRedirectingRules) {
    this.numberOfRedirectingRules = numberOfRedirectingRules;
  }

  public Integer getNumberOfDeletingRules() {
    return numberOfDeletingRules;
  }

  public void setNumberOfDeletingRules(final Integer numberOfDeletingRules) {
    this.numberOfDeletingRules = numberOfDeletingRules;
  }

  public Integer getNumberOfTagSelectingRules() {
    return numberOfTagSelectingRules;
  }

  public void setNumberOfTagSelectingRules(final Integer numberOfTagSelectingRules) {
    this.numberOfTagSelectingRules = numberOfTagSelectingRules;
  }

  public boolean getCrawlingEnabled() {
    return crawlingEnabled;
  }

  public void setCrawlingEnabled(final boolean crawlingEnabled) {
    this.crawlingEnabled = crawlingEnabled;
  }

  public boolean getVisible() {
    return visible;
  }

  public void setVisible(final boolean visible) {
    this.visible = visible;
  }

  public boolean getStatisticsChecking() {
    return statisticsChecking;
  }

  public void setStatisticsChecking(final boolean statisticsChecking) {
    this.statisticsChecking = statisticsChecking;
  }

}
