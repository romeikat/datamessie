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

import java.time.Duration;

public class DurationConverter extends AbstractConverter<Duration> {

  public static final DurationConverter INSTANCE = new DurationConverter();

  private static final long serialVersionUID = 1L;

  private DurationConverter() {}

  @Override
  protected Duration toObject(final String value) {
    if (value == null) {
      return null;
    }

    return Duration.parse(value);
  }

  @Override
  protected String toString(final Duration value) {
    if (value == null) {
      return null;
    }

    final long hours = value.toHours();
    final long minutes = value.toMinutes() - (hours * 60);
    final long seconds = value.getSeconds() - (hours * 3600) - (minutes * 60);
    final String secondsSuffix = seconds < 10 ? "0" : "";
    return minutes + ":" + secondsSuffix + seconds;
  }

}
