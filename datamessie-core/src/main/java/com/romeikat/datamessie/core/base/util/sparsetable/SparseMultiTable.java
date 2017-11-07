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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class SparseMultiTable<X, Y, Z> implements IMultiTable<X, Y, Z>, Serializable {

  private static final long serialVersionUID = 1L;

  private final SparseSingleTable<X, Y, List<Z>> innerTable;

  public SparseMultiTable() {
    innerTable = new SparseSingleTable<X, Y, List<Z>>();
  }

  protected List<Z> createInnerList() {
    return Lists.newLinkedList();
  }

  @Override
  public synchronized void putValue(final X rowHeader, final Y columnHeader, final Z value) {
    List<Z> innerValue = innerTable.getValue(rowHeader, columnHeader);
    if (innerValue == null) {
      innerValue = createInnerList();
      innerTable.putValue(rowHeader, columnHeader, innerValue);
    }
    innerValue.add(value);
  }

  @Override
  public synchronized void putValues(final ISingleTable<X, Y, Z> otherTable) {
    for (final Cell<X, Y, Z> cell : otherTable.getCells()) {
      putValue(cell.getRowHeader(), cell.getColumnHeader(), cell.getValue());
    }
  }

  @Override
  public synchronized void removeValues(final X rowHeader, final Y columnHeader) {
    innerTable.removeValue(rowHeader, columnHeader);
  }

  @Override
  public synchronized void addRowHeader(final X rowHeader) {
    innerTable.addRowHeader(rowHeader);
  }

  @Override
  public void addRowHeaders(final Collection<X> rowHeaders) {
    innerTable.addRowHeaders(rowHeaders);
  }

  @Override
  public synchronized void removeRow(final X rowHeader) {
    innerTable.removeRow(rowHeader);
  }

  @Override
  public synchronized void removeRows(final Collection<X> rowHeaders) {
    innerTable.removeRows(rowHeaders);
  }

  @Override
  public synchronized void addColumnHeader(final Y columnHeader) {
    innerTable.addColumnHeader(columnHeader);
  }

  @Override
  public void addColumnHeaders(final Collection<Y> columnHeaders) {
    innerTable.addColumnHeaders(columnHeaders);
  }

  @Override
  public synchronized void removeColumn(final Y columnHeader) {
    innerTable.removeColumn(columnHeader);
  }

  @Override
  public synchronized void removeColumns(final Collection<Y> columnHeaders) {
    innerTable.removeColumns(columnHeaders);
  }

  @Override
  public synchronized void clear() {
    innerTable.clear();
  }

  @Override
  public synchronized List<Z> getValues(final X rowHeader, final Y columnHeader) {
    return innerTable.getValue(rowHeader, columnHeader);
  }

  @Override
  public synchronized List<Z> getAllValues() {
    final List<Z> values = new ArrayList<Z>();

    final List<List<Z>> innerValues = innerTable.getAllValues();
    for (final List<Z> innerValue : innerValues) {
      values.addAll(innerValue);
    }

    return values;
  }

  @Override
  public Z mergeAllValues(final Function<Pair<Z, Z>, Z> mergeFunction) {
    final List<Z> allValues = new ArrayList<Z>();

    final List<List<Z>> innerValues = innerTable.getAllValues();
    for (final List<Z> innerValue : innerValues) {
      allValues.addAll(innerValue);
    }

    if (allValues.isEmpty()) {
      return null;
    }

    Z mergedValue = allValues.get(0);
    for (int i = 1; i < allValues.size(); i++) {
      final Z previousValue = mergedValue;
      final Z currentValue = allValues.get(i);
      final Pair<Z, Z> previousAndCurrentValue =
          new ImmutablePair<Z, Z>(previousValue, currentValue);
      mergedValue = mergeFunction.apply(previousAndCurrentValue);
    }

    return mergedValue;
  }

  @Override
  public synchronized Set<Cell<X, Y, List<Z>>> getCells() {
    return innerTable.getCells();
  }

  @Override
  public TableRow<X, Y, List<Z>> getTableRow(final X rowHeader) {
    return innerTable.getTableRow(rowHeader);
  }

  @Override
  public List<TableRow<X, Y, List<Z>>> getTableRows(
      final Comparator<? super X> rowHeaderComparator) {
    return innerTable.getTableRows(rowHeaderComparator);
  }

  @Override
  public List<TableRow<X, Y, List<Z>>> getTableRows(final List<X> rowHeaders) {
    return innerTable.getTableRows(rowHeaders);
  }

  @Override
  public synchronized Map<Y, List<Z>> getRow(final X rowHeader) {
    return innerTable.getRow(rowHeader);
  }

  @Override
  public synchronized SortedMap<Y, List<Z>> getRowSorted(final X rowHeader,
      final Comparator<? super Y> rowHeaderComparator,
      final Comparator<? super List<Z>> rowValueComparator) {
    return innerTable.getRowSorted(rowHeader, rowHeaderComparator, rowValueComparator);
  }

  @Override
  public synchronized Set<X> getRowHeaders() {
    return innerTable.getRowHeaders();
  }

  @Override
  public synchronized List<X> getRowHeadersSorted(final Comparator<? super X> comparator) {
    return innerTable.getRowHeadersSorted(comparator);
  }

  @Override
  public synchronized Set<X> getRowHeaders(final Y columnHeader) {
    return innerTable.getRowHeaders(columnHeader);
  }

  @Override
  public synchronized List<X> getRowHeadersSorted(final Y columnHeader,
      final Comparator<? super X> rowHeaderComparator) {
    return innerTable.getRowHeadersSorted(columnHeader, rowHeaderComparator);
  }

  @Override
  public List<X> getRowHeadersSorted(final Y columnHeader,
      final Comparator<? super X> rowHeaderComparator,
      final Comparator<? super List<Z>> columnValueComparator) {
    return innerTable.getRowHeadersSorted(columnHeader, rowHeaderComparator, columnValueComparator);
  }

  @Override
  public synchronized Collection<Z> getRowValues(final Y columnHeader) {
    final Collection<Z> collectedRowValues = Lists.newLinkedList();

    final Collection<List<Z>> rowValues = innerTable.getRowValues(columnHeader);
    for (final List<Z> rowValue : rowValues) {
      collectedRowValues.addAll(rowValue);
    }

    return collectedRowValues;
  }

  @Override
  public synchronized List<Z> getRowValuesSorted(final Y columnHeader,
      final Comparator<? super Z> rowValueComparator) {
    final List<Z> collectedRowValuesSorted = Lists.newLinkedList();

    final Collection<List<Z>> rowValues = innerTable.getRowValues(columnHeader);
    for (final List<Z> rowValue : rowValues) {
      collectedRowValuesSorted.addAll(rowValue);
    }

    Collections.sort(collectedRowValuesSorted, rowValueComparator);

    return collectedRowValuesSorted;
  }

  @Override
  public TableColumn<X, Y, List<Z>> getTableColumn(final Y columnHeader) {
    return innerTable.getTableColumn(columnHeader);
  }

  @Override
  public List<TableColumn<X, Y, List<Z>>> getTableColumns(
      final Comparator<? super Y> columnHeaderComparator) {
    return innerTable.getTableColumns(columnHeaderComparator);
  }

  @Override
  public List<TableColumn<X, Y, List<Z>>> getTableColumns(final List<Y> columnHeaders) {
    return innerTable.getTableColumns(columnHeaders);
  }

  @Override
  public synchronized Map<X, List<Z>> getColumn(final Y columnHeader) {
    return innerTable.getColumn(columnHeader);
  }

  @Override
  public synchronized SortedMap<X, List<Z>> getColumnSorted(final Y columnHeader,
      final Comparator<? super X> columnHeaderComparator,
      final Comparator<? super List<Z>> columnValueComparator) {
    return innerTable.getColumnSorted(columnHeader, columnHeaderComparator, columnValueComparator);
  }

  @Override
  public synchronized Set<Y> getColumnHeaders() {
    return innerTable.getColumnHeaders();
  }

  @Override
  public synchronized List<Y> getColumnHeadersSorted(final Comparator<? super Y> comparator) {
    return innerTable.getColumnHeadersSorted(comparator);
  }

  @Override
  public synchronized Set<Y> getColumnHeaders(final X rowHeader) {
    return innerTable.getColumnHeaders(rowHeader);
  }

  @Override
  public synchronized List<Y> getColumnHeadersSorted(final X rowHeader,
      final Comparator<? super Y> columnHeaderComparator) {
    return innerTable.getColumnHeadersSorted(rowHeader, columnHeaderComparator);
  }

  @Override
  public List<Y> getColumnHeadersSorted(final X rowHeader,
      final Comparator<? super Y> columnHeaderComparator,
      final Comparator<? super List<Z>> rowValueComparator) {
    return innerTable.getColumnHeadersSorted(rowHeader, columnHeaderComparator, rowValueComparator);
  }

  @Override
  public synchronized Collection<Z> getColumnValues(final X rowHeader) {
    final Collection<Z> collectedColumnValues = Lists.newLinkedList();

    final Collection<List<Z>> columnValues = innerTable.getColumnValues(rowHeader);
    for (final List<Z> columnValue : columnValues) {
      collectedColumnValues.addAll(columnValue);
    }

    return collectedColumnValues;
  }

  @Override
  public synchronized List<Z> getColumnValuesSorted(final X rowHeader,
      final Comparator<? super Z> columnValueComparator) {
    final List<Z> collectedColumnValuesSorted = Lists.newLinkedList();

    final Collection<List<Z>> columnValues = innerTable.getColumnValues(rowHeader);
    for (final List<Z> columnValue : columnValues) {
      collectedColumnValuesSorted.addAll(columnValue);
    }

    Collections.sort(collectedColumnValuesSorted, columnValueComparator);

    return collectedColumnValuesSorted;
  }

  @Override
  public synchronized boolean containsRowHeader(final X rowHeader) {
    return innerTable.containsRowHeader(rowHeader);
  }

  @Override
  public synchronized boolean containsColumnHeader(final Y columnHeader) {
    return innerTable.containsColumnHeader(columnHeader);
  }

  @Override
  public synchronized int getNumberOfRows() {
    return innerTable.getNumberOfRows();
  }

  @Override
  public synchronized int getNumberOfColumns() {
    return innerTable.getNumberOfColumns();
  }

  @Override
  public synchronized void transformValues(final IValueTransformer<X, Y, List<Z>> transformer) {
    innerTable.transformValues(transformer);
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final SparseMultiTable<?, ?, ?> otherTable = (SparseMultiTable<?, ?, ?>) other;
    final boolean equals = new EqualsBuilder().append(innerTable, otherTable.innerTable).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(innerTable).toHashCode();
    return hashCode;
  }

}
