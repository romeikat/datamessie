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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SparseSingleTable<X, Y, Z> implements ISingleTable<X, Y, Z>, Serializable {

  private static final long serialVersionUID = 1L;

  protected final Map<X, Map<Y, Z>> rows;

  protected final Map<Y, Map<X, Z>> columns;

  private static final boolean REMOVE_ROWS_COLUMNS_IF_EMPTY = false;

  public SparseSingleTable() {
    rows = createMap();
    columns = createMap();
  }

  public SparseSingleTable(final Set<Cell<X, Y, Z>> cells) {
    this();
    for (final Cell<X, Y, Z> cell : cells) {
      putValue(cell.getRowHeader(), cell.getColumnHeader(), cell.getValue());
    }
  }

  private <K, V> Map<K, V> createMap() {
    return new HashMap<K, V>();
  }

  public synchronized <Z2> SparseSingleTable<X, Y, Z2> extract(final ITableExtractor<X, Y, Z, Z2> tableExtractor) {
    // Merge
    final SparseSingleTable<X, Y, Z> merged = new SparseSingleTable<X, Y, Z>();
    for (final Cell<X, Y, Z> cell : getCells()) {
      final X rowHeader = cell.getRowHeader();
      final X extractedRowHeader = tableExtractor.getExtractedRowHeader(rowHeader);
      if (extractedRowHeader == null) {
        continue;
      }

      final Y columnHeader = cell.getColumnHeader();
      final Y extractedColumnHeader = tableExtractor.getExtractedColumnHeader(columnHeader);
      if (extractedColumnHeader == null) {
        continue;
      }

      final Z oldValue = merged.getValue(extractedRowHeader, extractedColumnHeader);
      final Z newValue = cell.getValue();
      Z mergedValue;
      if (oldValue == null) {
        mergedValue = newValue;
      } else {
        mergedValue = tableExtractor.mergeValues(oldValue, newValue);
      }
      merged.putValue(extractedRowHeader, extractedColumnHeader, mergedValue);
    }

    // Transform
    final SparseSingleTable<X, Y, Z2> transformed = new SparseSingleTable<X, Y, Z2>();
    for (final Cell<X, Y, Z> cell : merged.getCells()) {
      final X rowHeader = cell.getRowHeader();
      final Y columnHeader = cell.getColumnHeader();
      final Z value = cell.getValue();
      final Z2 transformedValue = tableExtractor.getExtractedValue(value);
      if (transformedValue != null) {
        transformed.putValue(rowHeader, columnHeader, transformedValue);
      }
    }
    return transformed;
  }

  protected X overrideRowHeader(final X desiredRowHeader) {
    return desiredRowHeader;
  }

  /**
   * Determines the column for value insertion by {@link #putValue}. For regular insertion into the
   * desired column, just return the desired column header. For insertion into a different column,
   * return the respective column header. To prevent insertion, return <code>null</code>.
   *
   * @param desiredColumnHeader The header of the desired column into which the new value should be
   *        inserted.
   * @return The header of the actual column into which the new value will be inserted;
   *         <code>null</code>, if the value should not be inserted at all.
   */
  protected Y overrideColumnHeader(final Y desiredColumnHeader) {
    return desiredColumnHeader;
  }

  /**
   * Determines the value to be inserted using {@link #putValue}. For regular insertion, just return
   * the new value. For insertion of a different value, return the respective value.
   *
   * @param existingValue The value before insertion.
   * @param newValue The desired value to be inserted.
   * @return The actual value to be inserted.
   */
  protected Z overrideValue(final Z existingValue, final Z newValue) {
    return newValue;
  }

  @Override
  public synchronized void putValue(X rowHeader, Y columnHeader, Z value) {
    // Apply overriding
    rowHeader = overrideRowHeader(rowHeader);
    columnHeader = overrideColumnHeader(columnHeader);
    if (rowHeader == null || columnHeader == null) {
      return;
    }
    final Z existingValue = getValue(rowHeader, columnHeader);
    value = overrideValue(existingValue, value);
    // Put
    internalPutValue(rowHeader, columnHeader, value);
  }

  @Override
  public synchronized void putValues(final ISingleTable<X, Y, Z> otherTable) {
    for (final Cell<X, Y, Z> cell : otherTable.getCells()) {
      putValue(cell.getRowHeader(), cell.getColumnHeader(), cell.getValue());
    }
  }

  private void internalPutValue(final X rowHeader, final Y columnHeader, final Z value) {
    // If null, remove
    if (value == null) {
      removeValue(rowHeader, columnHeader);
      return;
    }
    // Otherwise, add to row...
    Map<Y, Z> row = rows.get(rowHeader);
    if (row == null) {
      row = createMap();
      rows.put(rowHeader, row);
    }
    row.put(columnHeader, value);
    // ...and to column
    Map<X, Z> column = columns.get(columnHeader);
    if (column == null) {
      column = createMap();
      columns.put(columnHeader, column);
    }
    column.put(rowHeader, value);
  }

  @Override
  public synchronized void removeValue(final X rowHeader, final Y columnHeader) {
    // Remove value from respective row
    final Map<Y, Z> row = rows.get(rowHeader);
    if (row != null) {
      row.remove(columnHeader);
      // Remove row, if empty
      if (REMOVE_ROWS_COLUMNS_IF_EMPTY) {
        if (row.isEmpty()) {
          rows.remove(rowHeader);
        }
      }
    }
    // Remove value from respective column
    final Map<X, Z> column = columns.get(columnHeader);
    if (column != null) {
      column.remove(rowHeader);
      // Remove column, if empty
      if (REMOVE_ROWS_COLUMNS_IF_EMPTY) {
        if (column.isEmpty()) {
          columns.remove(columnHeader);
        }
      }
    }
  }

  @Override
  public synchronized void addRowHeader(final X rowHeader) {
    // Add column, if not yet exists
    Map<Y, Z> row = rows.get(rowHeader);
    if (row == null) {
      row = createMap();
      rows.put(rowHeader, row);
    }
  }

  @Override
  public synchronized void addRowHeaders(final Collection<X> rowHeaders) {
    for (final X rowHeader : rowHeaders) {
      addRowHeader(rowHeader);
    }
  }

  @Override
  public synchronized void removeRow(final X rowHeader) {
    // Remove row
    final Map<Y, Z> row = rows.remove(rowHeader);
    // Remove values from columns
    if (row != null) {
      final List<Y> columnHeadersToRemove = new LinkedList<Y>();
      for (final Y columnHeader : columns.keySet()) {
        final Map<X, Z> column = columns.get(columnHeader);
        column.remove(rowHeader);
        // Remove column, if empty
        if (column.isEmpty()) {
          columnHeadersToRemove.add(columnHeader);
        }
      }
      for (final Y columnHeader : columnHeadersToRemove) {
        columns.remove(columnHeader);
      }
    }
  }

  @Override
  public synchronized void removeRows(final Collection<X> rowHeaders) {
    for (final X rowHeader : rowHeaders) {
      removeRow(rowHeader);
    }
  }

  @Override
  public synchronized void addColumnHeader(final Y columnHeader) {
    // Add column, if not yet exists
    Map<X, Z> column = columns.get(columnHeader);
    if (column == null) {
      column = createMap();
      columns.put(columnHeader, column);
    }
  }

  @Override
  public synchronized void addColumnHeaders(final Collection<Y> columnHeaders) {
    for (final Y columnHeader : columnHeaders) {
      addColumnHeader(columnHeader);
    }
  }

  @Override
  public synchronized void removeColumn(final Y columnHeader) {
    // Remove column
    final Map<X, Z> column = columns.remove(columnHeader);
    // Remove values from rows
    if (column != null) {
      final List<X> rowHeadersToRemove = new LinkedList<X>();
      for (final X rowHeader : rows.keySet()) {
        final Map<Y, Z> row = rows.get(rowHeader);
        row.remove(columnHeader);
        // Remove row, if empty
        if (row.isEmpty()) {
          rowHeadersToRemove.add(rowHeader);
        }
      }
      for (final X rowHeader : rowHeadersToRemove) {
        rows.remove(rowHeader);
      }
    }
  }

  @Override
  public synchronized void removeColumns(final Collection<Y> columnHeaders) {
    for (final Y columnHeader : columnHeaders) {
      removeColumn(columnHeader);
    }
  }

  @Override
  public synchronized void clear() {
    rows.clear();
    columns.clear();
  }

  @Override
  public synchronized Z getValue(final X rowHeader, final Y columnHeader) {
    Z value = null;
    final Map<Y, Z> row = rows.get(rowHeader);
    if (row != null) {
      value = row.get(columnHeader);
    }
    return value;
  }

  @Override
  public synchronized List<Z> getAllValues() {
    final List<Z> values = new ArrayList<Z>();
    for (final X rowHeader : rows.keySet()) {
      final Map<Y, Z> row = rows.get(rowHeader);
      for (final Y columnHeader : row.keySet()) {
        final Z value = row.get(columnHeader);
        values.add(value);
      }
    }
    return values;
  }

  @Override
  public synchronized Z mergeAllValues(final Function<Pair<Z, Z>, Z> mergeFunction) {
    final List<Z> allValues = getAllValues();

    if (allValues.isEmpty()) {
      return null;
    }

    Z mergedValue = allValues.get(0);
    for (int i = 1; i < allValues.size(); i++) {
      final Z previousValue = mergedValue;
      final Z currentValue = allValues.get(i);
      final Pair<Z, Z> previousAndCurrentValue = new ImmutablePair<Z, Z>(previousValue, currentValue);
      mergedValue = mergeFunction.apply(previousAndCurrentValue);
    }

    return mergedValue;
  }

  @Override
  public synchronized Set<Cell<X, Y, Z>> getCells() {
    final Set<Cell<X, Y, Z>> cells = new HashSet<Cell<X, Y, Z>>();
    for (final X rowHeader : rows.keySet()) {
      final Map<Y, Z> row = rows.get(rowHeader);
      for (final Y columnHeader : row.keySet()) {
        final Z value = row.get(columnHeader);
        if (value != null) {
          final Cell<X, Y, Z> cell = new Cell<X, Y, Z>(rowHeader, columnHeader, value);
          cells.add(cell);
        }
      }
    }
    return cells;
  }

  @Override
  public synchronized TableRow<X, Y, Z> getTableRow(final X rowHeader) {
    final Map<Y, Z> row = getRow(rowHeader);
    final TableRow<X, Y, Z> tableRow = new TableRow<X, Y, Z>(rowHeader, row);
    return tableRow;
  }

  @Override
  public synchronized List<TableRow<X, Y, Z>> getTableRows(final Comparator<? super X> rowHeaderComparator) {
    final List<X> rowHeaders = getRowHeadersSorted(rowHeaderComparator);
    return getTableRows(rowHeaders);
  }

  @Override
  public synchronized List<TableRow<X, Y, Z>> getTableRows(final List<X> rowHeaders) {
    final List<TableRow<X, Y, Z>> tableRows = Lists.newArrayListWithExpectedSize(getNumberOfRows());
    // Get table rows
    for (final X rowHeader : rowHeaders) {
      final Map<Y, Z> row = getRow(rowHeader);
      final TableRow<X, Y, Z> tableRow = new TableRow<X, Y, Z>(rowHeader, row);
      tableRows.add(tableRow);
    }
    // Done
    return tableRows;
  }

  @Override
  public synchronized Map<Y, Z> getRow(final X rowHeader) {
    final Map<Y, Z> row = rows.get(rowHeader);
    if (row == null) {
      return createMap();
    }
    return row;
  }

  @Override
  public synchronized SortedMap<Y, Z> getRowSorted(final X rowHeader,
      final Comparator<? super Y> columnHeaderComparator, final Comparator<? super Z> rowValueComparator) {
    final Map<Y, Z> row = rows.get(rowHeader);
    // Sort column headers by values
    final Comparator<Y> valueComparator =
        new MapValueKeyComparator<Y, Z>(row, columnHeaderComparator, rowValueComparator);
    final SortedMap<Y, Z> valuesSorted = new TreeMap<Y, Z>(valueComparator);
    valuesSorted.putAll(row);
    // Done
    return valuesSorted;
  }

  @Override
  public synchronized Set<X> getRowHeaders() {
    final Set<X> rowHeaders = rows.keySet();
    return Sets.newHashSet(rowHeaders);
  }

  @Override
  public synchronized List<X> getRowHeadersSorted(final Comparator<? super X> comparator) {
    // Determine row headers
    final Set<X> rowHeaders = rows.keySet();
    // Sort row headers
    final List<X> rowHeadersSorted = new ArrayList<X>(rowHeaders);
    Collections.sort(rowHeadersSorted, comparator);
    // Done
    return rowHeadersSorted;
  }

  @Override
  public synchronized Set<X> getRowHeaders(final Y columnHeader) {
    final Map<X, Z> column = columns.get(columnHeader);
    // If column does not exist, return empty list
    if (column == null) {
      return new HashSet<X>();
    }
    // Determine row headers
    final Set<X> rowHeaders = column.keySet();
    // Done
    return Sets.newHashSet(rowHeaders);
  }

  @Override
  public synchronized List<X> getRowHeadersSorted(final Y columnHeader,
      final Comparator<? super X> rowHeaderComparator) {
    final Map<X, Z> column = columns.get(columnHeader);
    // If column does not exist, return empty list
    if (column == null) {
      return new ArrayList<X>();
    }
    // Determine row headers
    final Set<X> rowHeaders = column.keySet();
    // Sort row headers
    final List<X> rowHeadersSorted = new ArrayList<X>(rowHeaders);
    Collections.sort(rowHeadersSorted, rowHeaderComparator);
    // Done
    return rowHeadersSorted;
  }

  @Override
  public synchronized List<X> getRowHeadersSorted(final Y columnHeader, final Comparator<? super X> rowHeaderComparator,
      final Comparator<? super Z> columnValueComparator) {
    final SortedMap<X, Z> columnSorted = getColumnSorted(columnHeader, rowHeaderComparator, columnValueComparator);
    final List<X> rowHeadersSorted = Lists.newArrayList(columnSorted.keySet());
    // Done
    return rowHeadersSorted;
  }

  @Override
  public synchronized Collection<Z> getRowValues(final Y columnHeader) {
    final Map<X, Z> column = columns.get(columnHeader);
    // If column does not exist, return empty list
    if (column == null) {
      return new HashSet<Z>();
    }
    // Determine row values
    final Collection<Z> rowValues = column.values();
    // Done
    return Lists.newArrayList(rowValues);
  }

  @Override
  public synchronized List<Z> getRowValuesSorted(final Y columnHeader, final Comparator<? super Z> rowValueComparator) {
    final Map<X, Z> column = columns.get(columnHeader);
    // If column does not exist, return empty list
    if (column == null) {
      return new ArrayList<Z>();
    }
    // Determine row values
    final Collection<Z> rowValues = column.values();
    // Sort row values
    final List<Z> rowValuesSorted = new ArrayList<Z>(rowValues);
    Collections.sort(rowValuesSorted, rowValueComparator);
    // Done
    return rowValuesSorted;
  }

  @Override
  public synchronized TableColumn<X, Y, Z> getTableColumn(final Y columnHeader) {
    final Map<X, Z> column = getColumn(columnHeader);
    final TableColumn<X, Y, Z> tableColumn = new TableColumn<X, Y, Z>(columnHeader, column);
    return tableColumn;
  }

  @Override
  public synchronized List<TableColumn<X, Y, Z>> getTableColumns(final Comparator<? super Y> columnHeaderComparator) {
    final List<Y> columnHeaders = getColumnHeadersSorted(columnHeaderComparator);
    return getTableColumns(columnHeaders);
  }

  @Override
  public synchronized List<TableColumn<X, Y, Z>> getTableColumns(final List<Y> columnHeaders) {
    final List<TableColumn<X, Y, Z>> tableColumns = Lists.newArrayListWithExpectedSize(getNumberOfColumns());
    // Get table columns
    for (final Y columnHeader : columnHeaders) {
      final Map<X, Z> column = getColumn(columnHeader);
      final TableColumn<X, Y, Z> tableColumn = new TableColumn<X, Y, Z>(columnHeader, column);
      tableColumns.add(tableColumn);
    }
    // Done
    return tableColumns;
  }

  @Override
  public synchronized Map<X, Z> getColumn(final Y columnHeader) {
    final Map<X, Z> column = columns.get(columnHeader);
    if (column == null) {
      return createMap();
    }
    return column;
  }

  @Override
  public synchronized SortedMap<X, Z> getColumnSorted(final Y columnHeader,
      final Comparator<? super X> rowHeaderComparator, final Comparator<? super Z> columnValueComparator) {
    final Map<X, Z> column = columns.get(columnHeader);
    // Sort row headers by values
    final Comparator<X> valueComparator =
        new MapValueKeyComparator<X, Z>(column, rowHeaderComparator, columnValueComparator);
    final SortedMap<X, Z> valuesSorted = new TreeMap<X, Z>(valueComparator);
    valuesSorted.putAll(column);
    // Done
    return valuesSorted;
  }

  @Override
  public synchronized Set<Y> getColumnHeaders() {
    final Set<Y> columnHeaders = columns.keySet();
    return Sets.newHashSet(columnHeaders);
  }

  @Override
  public synchronized List<Y> getColumnHeadersSorted(final Comparator<? super Y> comparator) {
    // Determine column headers
    final Set<Y> columnHeaders = columns.keySet();
    // Sort column headers
    final List<Y> columnHeadersSorted = new ArrayList<Y>(columnHeaders);
    Collections.sort(columnHeadersSorted, comparator);
    // Done
    return columnHeadersSorted;
  }

  @Override
  public synchronized Set<Y> getColumnHeaders(final X rowHeader) {
    final Map<Y, Z> row = rows.get(rowHeader);
    // If row does not exist, return empty list
    if (row == null) {
      return new HashSet<Y>();
    }
    // Determine column headers
    final Set<Y> columnHeaders = row.keySet();
    // Done
    return Sets.newHashSet(columnHeaders);
  }

  @Override
  public synchronized List<Y> getColumnHeadersSorted(final X rowHeader,
      final Comparator<? super Y> columnHeaderComparator) {
    final Map<Y, Z> row = rows.get(rowHeader);
    // If row does not exist, return empty list
    if (row == null) {
      return new ArrayList<Y>();
    }
    // Determine column headers
    final Set<Y> columnHeaders = row.keySet();
    // Sort column headers
    final List<Y> columnHeadersSorted = new ArrayList<Y>(columnHeaders);
    Collections.sort(columnHeadersSorted, columnHeaderComparator);
    // Done
    return columnHeadersSorted;
  }

  @Override
  public synchronized List<Y> getColumnHeadersSorted(final X rowHeader,
      final Comparator<? super Y> columnHeaderComparator, final Comparator<? super Z> rowValueComparator) {
    final SortedMap<Y, Z> rowSorted = getRowSorted(rowHeader, columnHeaderComparator, rowValueComparator);
    final List<Y> columnHeadersSorted = Lists.newArrayList(rowSorted.keySet());
    // Done
    return columnHeadersSorted;
  }

  @Override
  public synchronized Collection<Z> getColumnValues(final X rowHeader) {
    final Map<Y, Z> row = rows.get(rowHeader);
    // If row does not exist, return empty list
    if (row == null) {
      return new HashSet<Z>();
    }
    // Determine column values
    final Collection<Z> columnValues = row.values();
    // Done
    return Lists.newArrayList(columnValues);
  }

  @Override
  public synchronized List<Z> getColumnValuesSorted(final X rowHeader,
      final Comparator<? super Z> colummValueComparator) {
    final Map<Y, Z> row = rows.get(rowHeader);
    // If row does not exist, return empty list
    if (row == null) {
      return new ArrayList<Z>();
    }
    // Determine column values
    final Collection<Z> columnValues = row.values();
    // Sort column values
    final List<Z> columnValuesSorted = new ArrayList<Z>(columnValues);
    Collections.sort(columnValuesSorted, colummValueComparator);
    // Done
    return columnValuesSorted;
  }

  @Override
  public synchronized boolean containsRowHeader(final X rowHeader) {
    return rows.containsKey(rowHeader);
  }

  @Override
  public synchronized boolean containsColumnHeader(final Y columnHeader) {
    return columns.containsKey(columnHeader);
  }

  @Override
  public synchronized int getNumberOfRows() {
    return rows.size();
  }

  @Override
  public synchronized int getNumberOfColumns() {
    return columns.size();
  }

  @Override
  public synchronized void transformValues(final IValueTransformer<X, Y, Z> transformer) {
    final Set<Cell<X, Y, Z>> cells = getCells();
    for (final Cell<X, Y, Z> cell : cells) {
      final Z oldValue = cell.getValue();
      final Z newValue = transformer.transform(cell.getRowHeader(), cell.getColumnHeader(), oldValue);
      internalPutValue(cell.getRowHeader(), cell.getColumnHeader(), newValue);
    }
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
    final SparseSingleTable<?, ?, ?> otherTable = (SparseSingleTable<?, ?, ?>) other;
    final boolean equals =
        new EqualsBuilder().append(rows, otherTable.rows).append(columns, otherTable.columns).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(rows).append(columns).toHashCode();
    return hashCode;
  }

}
