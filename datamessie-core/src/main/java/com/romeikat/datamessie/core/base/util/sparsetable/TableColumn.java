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

public class TableColumn<X, Y, Z> {

  private final Y columnHeader;

  private final Map<X, Z> values;

  public TableColumn(final Y columnHeader, final Map<X, Z> values) {
    this.columnHeader = columnHeader;
    this.values = values;
  }

  public Y getColumnHeader() {
    return columnHeader;
  }

  public Map<X, Z> getValues() {
    return values;
  }

}
