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
import java.time.LocalDate;
import com.romeikat.datamessie.core.domain.enums.DeletingRuleMode;

public class DeletingRuleDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;

  private String selector;

  private LocalDate activeFrom;

  private LocalDate activeTo;

  private DeletingRuleMode mode = DeletingRuleMode.REGEX;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getSelector() {
    return selector;
  }

  public void setSelector(final String selector) {
    this.selector = selector;
  }

  public LocalDate getActiveFrom() {
    return activeFrom;
  }

  public void setActiveFrom(final LocalDate activeFrom) {
    this.activeFrom = activeFrom;
  }

  public LocalDate getActiveTo() {
    return activeTo;
  }

  public void setActiveTo(final LocalDate activeTo) {
    this.activeTo = activeTo;
  }

  public DeletingRuleMode getMode() {
    return mode;
  }

  public void setMode(final DeletingRuleMode mode) {
    this.mode = mode;
  }

}
