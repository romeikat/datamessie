package com.romeikat.datamessie.core.domain.entity.impl;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.enums.Language;

@Entity
@Table(name = Source.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "source_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "source_name_url", columnNames = {"name", "url"})})
public class Source extends AbstractEntityWithGeneratedIdAndVersion {

  public static final String TABLE_NAME = "source";

  private String name;

  private Language language;

  private String url;

  private String cookie;

  private boolean crawlingEnabled;

  private boolean visible;

  private boolean statisticsChecking;

  private String notes;

  public Source() {}

  public Source(final long id, final String name, final String url, final String cookie,
      final boolean crawlingEnabled, final boolean visible, final boolean statisticsChecking) {
    super(id);
    this.name = name;
    this.url = url;
    this.cookie = cookie;
    this.crawlingEnabled = crawlingEnabled;
    this.visible = visible;
    this.statisticsChecking = statisticsChecking;
  }

  @Column(nullable = false)
  public String getName() {
    return name;
  }

  public Source setName(final String name) {
    this.name = name;
    return this;
  }

  @Column(length = 2)
  @Enumerated(value = EnumType.STRING)
  public Language getLanguage() {
    return language;
  }

  public Source setLanguage(final Language language) {
    this.language = language;
    return this;
  }

  @Column(nullable = false, length = 511)
  public String getUrl() {
    return url;
  }

  public Source setUrl(final String url) {
    this.url = url;
    return this;
  }

  public String getCookie() {
    return cookie;
  }

  public Source setCookie(final String cookie) {
    this.cookie = cookie;
    return this;
  }

  @Column(nullable = false)
  public boolean getCrawlingEnabled() {
    return crawlingEnabled;
  }

  public Source setCrawlingEnabled(final boolean crawlingEnabled) {
    this.crawlingEnabled = crawlingEnabled;
    return this;
  }

  @Column(nullable = false)
  public boolean getVisible() {
    return visible;
  }

  public Source setVisible(final boolean visible) {
    this.visible = visible;
    return this;
  }

  @Column(nullable = false)
  public boolean getStatisticsChecking() {
    return statisticsChecking;
  }

  public Source setStatisticsChecking(final boolean statisticsChecking) {
    this.statisticsChecking = statisticsChecking;
    return this;
  }

  public String getNotes() {
    return notes;
  }

  public Source setNotes(final String notes) {
    this.notes = notes;
    return this;
  }

}
