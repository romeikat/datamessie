package com.romeikat.datamessie.core.base.util.sparsetable;

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

import java.time.LocalDate;

import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;

public class StatisticsSparseTable extends SparseSingleTable<Long, LocalDate, DocumentsPerState> {

  private static final long serialVersionUID = 1L;

  @Override
  protected DocumentsPerState overrideValue(final DocumentsPerState existingValue, final DocumentsPerState newValue) {
    if (existingValue == null) {
      return newValue;
    }
    if (newValue == null) {
      return existingValue;
    }

    existingValue.addAll(newValue);
    return existingValue;
  }

}
