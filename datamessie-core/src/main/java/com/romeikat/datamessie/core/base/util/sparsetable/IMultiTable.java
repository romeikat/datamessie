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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public interface IMultiTable<X, Y, Z> extends ITable<X, Y, Z> {

  void removeValues(X rowHeader, Y columnHeader);

  List<Z> getValues(final X rowHeader, final Y columnHeader);

  List<Z> getAllValues();

  TableRow<X, Y, List<Z>> getTableRow(X rowHeader);

  List<TableRow<X, Y, List<Z>>> getTableRows(Comparator<? super X> rowHeaderComparator);

  List<TableRow<X, Y, List<Z>>> getTableRows(List<X> rowHeaders);

  Map<Y, List<Z>> getRow(X rowHeader);

  List<X> getRowHeadersSorted(Y columnHeader, Comparator<? super X> rowHeaderComparator,
      Comparator<? super List<Z>> columnValueComparator);

  SortedMap<Y, List<Z>> getRowSorted(X rowHeader, Comparator<? super Y> rowHeaderComparator,
      Comparator<? super List<Z>> rowValueComparator);

  TableColumn<X, Y, List<Z>> getTableColumn(Y columnHeader);

  List<TableColumn<X, Y, List<Z>>> getTableColumns(Comparator<? super Y> columnHeaderComparator);

  List<TableColumn<X, Y, List<Z>>> getTableColumns(List<Y> columnHeaders);

  Map<X, List<Z>> getColumn(Y columnHeader);

  List<Y> getColumnHeadersSorted(X rowHeader, Comparator<? super Y> columnHeaderComparator,
      Comparator<? super List<Z>> rowValueComparator);

  SortedMap<X, List<Z>> getColumnSorted(Y columnHeader,
      Comparator<? super X> columnHeaderComparator,
      Comparator<? super List<Z>> columnValueComparator);

  Set<Cell<X, Y, List<Z>>> getCells();

  void transformValues(IValueTransformer<X, Y, List<Z>> transformer);

}
