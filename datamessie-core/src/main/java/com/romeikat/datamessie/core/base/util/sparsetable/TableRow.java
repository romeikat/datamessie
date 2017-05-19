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

import java.util.Map;

public class TableRow<X, Y, Z> {

  private final X rowHeader;

  private final Map<Y, Z> values;

  public TableRow(final X rowHeader, final Map<Y, Z> values) {
    this.rowHeader = rowHeader;
    this.values = values;
  }

  public X getRowHeader() {
    return rowHeader;
  }

  public Map<Y, Z> getValues() {
    return values;
  }

}
