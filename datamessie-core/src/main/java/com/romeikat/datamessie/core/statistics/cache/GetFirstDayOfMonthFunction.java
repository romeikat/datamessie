package com.romeikat.datamessie.core.statistics.cache;

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
import java.time.temporal.TemporalAdjusters;

import com.google.common.base.Function;

public class GetFirstDayOfMonthFunction implements Function<LocalDate, LocalDate>, Serializable {

  private static final long serialVersionUID = 1L;

  @Override
  public LocalDate apply(final LocalDate from) {
    final LocalDate firstDayOfMonth = from.with(TemporalAdjusters.firstDayOfMonth());
    return firstDayOfMonth;
  }

}
