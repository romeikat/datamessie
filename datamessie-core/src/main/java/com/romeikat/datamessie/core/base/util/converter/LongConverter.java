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

import java.text.NumberFormat;
import java.text.ParseException;

public class LongConverter extends AbstractConverter<Long> {

  public static final LongConverter INSTANCE = new LongConverter();

  private static final long serialVersionUID = 1L;

  private final NumberFormat numberFormat;

  private LongConverter() {
    numberFormat = NumberFormat.getIntegerInstance();
    numberFormat.setGroupingUsed(true);
  }

  @Override
  protected Long toObject(final String value) {
    if (value == null) {
      return null;
    }

    try {
      final Number n = numberFormat.parse(value);
      return n.longValue();
    } catch (final ParseException e) {
      return null;
    }
  }

  @Override
  protected String toString(final Long value) {
    if (value == null) {
      return null;
    }

    final String s = numberFormat.format(value);
    return s;
  }

}
