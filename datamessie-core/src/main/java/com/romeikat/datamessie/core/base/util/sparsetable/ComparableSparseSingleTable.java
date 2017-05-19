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

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;

public class ComparableSparseSingleTable<X extends Comparable<? super X>, Y extends Comparable<? super Y>, Z extends Comparable<? super Z>>
    extends SparseSingleTable<X, Y, Z> {

  private static final long serialVersionUID = 1L;

  private static final String lineSeparator = System.getProperty("line.separator");

  public synchronized SortedMap<Y, Z> getRowSorted(final X rowHeader) {
    return getRowSorted(rowHeader, new AscendingComparator<Y>(), new AscendingComparator<Z>());
  }

  public synchronized List<X> getRowHeadersSorted() {
    return getRowHeadersSorted(new AscendingComparator<X>());
  }

  public synchronized List<X> getRowHeadersSorted(final Y columnHeader) {
    return getRowHeadersSorted(columnHeader, new AscendingComparator<X>());
  }

  public synchronized List<Z> getRowValuesSorted(final Y columnHeader) {
    return getRowValuesSorted(columnHeader, new AscendingComparator<Z>());
  }

  public synchronized SortedMap<X, Z> getColumnSorted(final Y columnHeader) {
    return getColumnSorted(columnHeader, new AscendingComparator<X>(), new AscendingComparator<Z>());
  }

  public synchronized List<Y> getColumnHeadersSorted() {
    return getColumnHeadersSorted(new AscendingComparator<Y>());
  }

  public synchronized List<Y> getColumnHeadersSorted(final X rowHeader) {
    return getColumnHeadersSorted(rowHeader, new AscendingComparator<Y>());
  }

  public synchronized List<Z> getColumnValuesSorted(final X rowHeader) {
    return getColumnValuesSorted(rowHeader, new AscendingComparator<Z>());
  }

  public String printAsCsv() {
    final SparseTablePrinter<X, Y, Z> sparseTablePrinter = new DefaultSparseTablePrinter<X, Y, Z>();
    return printAsCsv(sparseTablePrinter);
  }

  public String printAsCsv(final SparseTablePrinter<X, Y, Z> sparseTablePrinter) {
    final StringBuilder talbeContent = new StringBuilder();
    final String topLeftCornerString = sparseTablePrinter.printTopLeftCorner();
    if (topLeftCornerString != null) {
      talbeContent.append(topLeftCornerString);
    }
    final String separator = "\t";
    // Headline
    final List<Y> columnHeaders = getColumnHeadersSorted();
    for (final Y columnHeader : columnHeaders) {
      talbeContent.append(separator);
      final String columnHeaderString = sparseTablePrinter.printColumnHeader(columnHeader);
      if (columnHeaderString != null) {
        talbeContent.append(columnHeaderString);
      }
    }
    talbeContent.append(lineSeparator);
    // One line per row
    final List<X> rowHeaders = getRowHeadersSorted();
    for (final X rowHeader : rowHeaders) {
      final Map<Y, Z> row = rows.get(rowHeader);
      // Row header
      final String rowHeaderString = sparseTablePrinter.printRowHeader(rowHeader);
      if (rowHeaderString != null) {
        talbeContent.append(rowHeaderString);
      }
      // Column values
      for (final Y columnHeader : columnHeaders) {
        final Z value = row.get(columnHeader);
        talbeContent.append(separator);
        if (value != null) {
          final String valueString = sparseTablePrinter.printValue(value);
          if (valueString != null) {
            talbeContent.append(valueString);
          }
        }
      }
      talbeContent.append(lineSeparator);
    }
    // Done
    return talbeContent.toString();
  }

  @Override
  public String toString() {
    return printAsCsv();
  }

}
