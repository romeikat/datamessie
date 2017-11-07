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

import java.util.Comparator;
import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;

public class DefaultSparseTablePrinter<X extends Comparable<? super X>, Y extends Comparable<? super Y>, Z>
    implements SparseTablePrinter<X, Y, Z> {

  @Override
  public String printTopLeftCorner() {
    return null;
  }

  @Override
  public String printRowHeader(final X rowHeader) {
    return rowHeader.toString();
  }

  @Override
  public String printColumnHeader(final Y columnHeader) {
    return columnHeader.toString();
  }

  @Override
  public String printValue(final Z value) {
    return value.toString();
  }

  @Override
  public Comparator<X> getRowHeaderComparator() {
    return new AscendingComparator<X>();
  }

  @Override
  public Comparator<Y> getColumnHeaderComparator() {
    return new AscendingComparator<Y>();
  }

}
