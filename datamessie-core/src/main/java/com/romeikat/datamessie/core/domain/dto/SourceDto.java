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

public class SourceDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROTOCOL1 = "http://";
  private static final String PROTOCOL2 = "https://";

  private long id;

  private String name;

  private Language language;

  private List<SourceTypeDto> types;

  private String url;

  private List<RedirectingRuleDto> redirectingRules;

  private List<TagSelectingRuleDto> tagSelectingRules;

  private Integer numberOfRedirectingRules;

  private Integer numberOfTagSelectingRules;

  private boolean visible;

  private boolean statisticsChecking;

  public long getId() {
    return id;
  }

  public void setId(final long id) {
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

  public List<RedirectingRuleDto> getRedirectingRules() {
    return redirectingRules;
  }

  public void setRedirectingRules(final List<RedirectingRuleDto> redirectingRules) {
    this.redirectingRules = redirectingRules;
  }

  public List<TagSelectingRuleDto> getTagSelectingRules() {
    return tagSelectingRules;
  }

  public void setTagSelectingRules(final List<TagSelectingRuleDto> tagSelectingRules) {
    this.tagSelectingRules = tagSelectingRules;
  }

  public Integer getNumberOfRedirectingRules() {
    return numberOfRedirectingRules;
  }

  public void setNumberOfRedirectingRules(final Integer numberOfRedirectingRules) {
    this.numberOfRedirectingRules = numberOfRedirectingRules;
  }

  public Integer getNumberOfTagSelectingRules() {
    return numberOfTagSelectingRules;
  }

  public void setNumberOfTagSelectingRules(final Integer numberOfTagSelectingRules) {
    this.numberOfTagSelectingRules = numberOfTagSelectingRules;
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
