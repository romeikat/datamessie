package com.romeikat.datamessie.core.base.util;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.google.common.collect.Lists;

public class DateUtil {

  public static Date fromLocalDate(final LocalDate ld) {
    if (ld == null) {
      return null;
    }

    return Date.from(ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
  }

  public static LocalDate toLocalDate(final Date d) {
    if (d == null) {
      return null;
    }

    return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).toLocalDate();
  }

  public static LocalDate toLocalDate(final LocalDateTime d) {
    if (d == null) {
      return null;
    }

    return d.toLocalDate();
  }

  public static LocalDate toLocalDate(final java.sql.Date d) {
    if (d == null) {
      return null;
    }

    return d.toLocalDate();
  }

  public static Date fromLocalDateTime(final LocalDateTime ldt) {
    if (ldt == null) {
      return null;
    }

    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
  }

  public static LocalDateTime toLocalDateTime(final Date d) {
    if (d == null) {
      return null;
    }

    return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
  }

  public static long getDelayUntil(final LocalDateTime ldt) {
    final LocalDateTime now = LocalDateTime.now();
    long delayInMillis = Duration.between(now, ldt).getSeconds() * 1000;
    if (delayInMillis <= 0) {
      delayInMillis = 0;
    }
    return delayInMillis;
  }

  public static List<LocalDate> getLocalDatesBetween(final LocalDate localDate1,
      final LocalDate localDate2) {
    if (localDate1 == null || localDate2 == null) {
      return Collections.emptyList();
    }

    if (localDate1.equals(localDate2)) {
      return Lists.newArrayList(localDate1);
    }

    final int daysBetween = (int) Math.abs(ChronoUnit.DAYS.between(localDate1, localDate2));
    final List<LocalDate> localDatesBetween = Lists.newArrayListWithExpectedSize(daysBetween + 1);

    // Add first date
    localDatesBetween.add(localDate1);
    // Add further dates
    final boolean ascending = localDate1.isBefore(localDate2);
    for (int i = 1; i <= daysBetween; i++) {
      final int offset = ascending ? i : -i;
      final LocalDate localDate = localDate1.plusDays(offset);
      localDatesBetween.add(localDate);
    }

    return localDatesBetween;
  }

}
