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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter extends AbstractConverter<Date> {

  public static final DateConverter INSTANCE_UI = new DateConverter("dd.MM.yy HH:mm:ss");
  public static final DateConverter INSTANCE_SYS = new DateConverter("yyyy-MM-dd_HH:mm:ss");

  private static final long serialVersionUID = 1L;

  private final DateFormat dateFormat;

  private DateConverter(final String pattern) {
    dateFormat = new SimpleDateFormat(pattern);
  }

  @Override
  protected Date toObject(final String value) {
    if (value == null) {
      return null;
    }

    try {
      return dateFormat.parse(value);
    } catch (final ParseException e) {
      return null;
    }
  }

  @Override
  protected String toString(final Date value) {
    if (value == null) {
      return null;
    }

    return dateFormat.format(value);
  }

}
