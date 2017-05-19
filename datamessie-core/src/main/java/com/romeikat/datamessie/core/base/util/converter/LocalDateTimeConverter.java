package com.romeikat.datamessie.core.base.util.converter;

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
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter extends AbstractConverter<LocalDateTime> {

  public static final LocalDateTimeConverter INSTANCE_UI = new LocalDateTimeConverter("dd.MM.yy HH:mm:ss");
  public static final LocalDateTimeConverter INSTANCE_SYS = new LocalDateTimeConverter("yyyy-MM-dd_HH:mm:ss");

  private static final long serialVersionUID = 1L;

  private final String pattern;

  private LocalDateTimeConverter(final String pattern) {
    this.pattern = pattern;
  }

  @Override
  protected LocalDateTime toObject(final String value) {
    if (value == null) {
      return null;
    }

    return LocalDateTime.parse(value);
  }

  @Override
  protected String toString(final LocalDateTime value) {
    if (value == null) {
      return null;
    }

    return value.format(DateTimeFormatter.ofPattern(pattern));
  }

}
