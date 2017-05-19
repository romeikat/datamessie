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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;

public interface ITable<X, Y, Z> {

  Z mergeAllValues(Function<Pair<Z, Z>, Z> mergeFunction);

  void putValue(X rowHeader, Y columnHeader, Z value);

  void putValues(ISingleTable<X, Y, Z> otherTable);

  void addRowHeader(X rowHeader);

  void addRowHeaders(Collection<X> rowHeaders);

  void removeRow(X rowHeader);

  void removeRows(Collection<X> rowHeaders);

  void addColumnHeader(Y columnHeader);

  void addColumnHeaders(Collection<Y> columnHeaders);

  void removeColumn(Y columnHeader);

  void removeColumns(Collection<Y> columnHeaders);

  void clear();

  Set<X> getRowHeaders();

  List<X> getRowHeadersSorted(Comparator<? super X> comparator);

  Set<X> getRowHeaders(Y columnHeader);

  List<X> getRowHeadersSorted(Y columnHeader, Comparator<? super X> rowHeaderComparator);

  Collection<Z> getRowValues(Y columnHeader);

  List<Z> getRowValuesSorted(Y columnHeader, Comparator<? super Z> rowValueComparator);

  Set<Y> getColumnHeaders();

  List<Y> getColumnHeadersSorted(Comparator<? super Y> comparator);

  Set<Y> getColumnHeaders(X rowHeader);

  List<Y> getColumnHeadersSorted(X rowHeader, Comparator<? super Y> columnHeaderComparator);

  Collection<Z> getColumnValues(X rowHeader);

  List<Z> getColumnValuesSorted(X rowHeader, Comparator<? super Z> colummValueComparator);

  boolean containsRowHeader(X rowHeader);

  boolean containsColumnHeader(Y columnHeader);

  int getNumberOfRows();

  int getNumberOfColumns();

}
