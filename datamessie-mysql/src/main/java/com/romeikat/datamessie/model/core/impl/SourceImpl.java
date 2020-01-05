package com.romeikat.datamessie.model.core.impl;

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
import com.romeikat.datamessie.model.core.Source;
import com.romeikat.datamessie.model.enums.Language;
import com.romeikat.datamessie.model.impl.AbstractEntityWithGeneratedIdAndVersion;

@Entity
@Table(name = SourceImpl.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "source_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "source_name_url", columnNames = {"name", "url"})})
public class SourceImpl extends AbstractEntityWithGeneratedIdAndVersion implements Source {

  public static final String TABLE_NAME = "source";

  private String name;

  private Language language;

  private String url;

  private boolean visible;

  private boolean statisticsChecking;

  public SourceImpl() {}

  public SourceImpl(final long id, final String name, final String url, final boolean visible,
      final boolean statisticsChecking) {
    super(id);
    this.name = name;
    this.url = url;
    this.visible = visible;
    this.statisticsChecking = statisticsChecking;
  }

  @Override
  @Column(nullable = false)
  public String getName() {
    return name;
  }

  @Override
  public Source setName(final String name) {
    this.name = name;
    return this;
  }

  @Override
  @Column(length = 2)
  @Enumerated(value = EnumType.STRING)
  public Language getLanguage() {
    return language;
  }

  @Override
  public Source setLanguage(final Language language) {
    this.language = language;
    return this;
  }

  @Override
  @Column(nullable = false, length = 511)
  public String getUrl() {
    return url;
  }

  @Override
  public Source setUrl(final String url) {
    this.url = url;
    return this;
  }

  @Override
  @Column(nullable = false)
  public boolean getVisible() {
    return visible;
  }

  @Override
  public Source setVisible(final boolean visible) {
    this.visible = visible;
    return this;
  }

  @Override
  @Column(nullable = false)
  public boolean getStatisticsChecking() {
    return statisticsChecking;
  }

  @Override
  public void setStatisticsChecking(final boolean statisticsChecking) {
    this.statisticsChecking = statisticsChecking;
  }

}
