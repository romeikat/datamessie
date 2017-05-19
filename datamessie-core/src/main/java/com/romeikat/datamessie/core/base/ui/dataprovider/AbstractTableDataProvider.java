package com.romeikat.datamessie.core.base.ui.dataprovider;

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
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;
import com.romeikat.datamessie.core.base.util.comparator.DescendingComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.ISingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.TableRow;

public abstract class AbstractTableDataProvider<X extends Comparable<? super X>, Y, Z extends Comparable<? super Z>>
    extends SortableDataProvider<TableRow<X, Y, Z>, Y> {

  private static final long serialVersionUID = 1L;

  private final IModel<ISingleTable<X, Y, Z>> tableModel;

  private final IModel<List<TableRow<X, Y, Z>>> tableRowsModel;

  public AbstractTableDataProvider(final IModel<ISingleTable<X, Y, Z>> tableModel) {
    this.tableModel = tableModel;
    this.tableRowsModel = new LoadableDetachableModel<List<TableRow<X, Y, Z>>>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected List<TableRow<X, Y, Z>> load() {
        return getTableRows();
      }
    };
  }

  @Override
  public Iterator<? extends TableRow<X, Y, Z>> iterator(final long first, final long count) {
    final List<TableRow<X, Y, Z>> tableRows = tableRowsModel.getObject();

    final int fromIndex = (int) first;
    final int toIndex = (int) (first + count);
    final List<TableRow<X, Y, Z>> tableRowsToShow = tableRows.subList(fromIndex, toIndex);

    return tableRowsToShow.iterator();
  }

  @Override
  public long size() {
    final List<TableRow<X, Y, Z>> tableRows = tableRowsModel.getObject();
    return tableRows.size();
  }

  @Override
  public IModel<TableRow<X, Y, Z>> model(final TableRow<X, Y, Z> tableRow) {
    final X rowHeader = tableRow.getRowHeader();
    return new LoadableDetachableModel<TableRow<X, Y, Z>>() {

      private static final long serialVersionUID = 1L;

      @Override
      protected TableRow<X, Y, Z> load() {
        final ISingleTable<X, Y, Z> table = tableModel.getObject();
        final TableRow<X, Y, Z> tableRow = table.getTableRow(rowHeader);
        return tableRow;
      }
    };
  }

  private List<TableRow<X, Y, Z>> getTableRows() {
    final ISingleTable<X, Y, Z> table = tableModel.getObject();
    final SortParam<Y> sortParam = getSort();
    final List<X> rowHeaders;

    // Default sorting
    if (sortParam == null) {
      final Comparator<? super X> rowHeaderComparator = getRowHeaderComparator();
      rowHeaders = table.getRowHeadersSorted(rowHeaderComparator);
    }
    // Sorting by sort state
    else {
      final Y sortProperty = sortParam.getProperty();
      final Comparator<? super X> rowHeaderComparator = getRowHeaderComparator();
      final Comparator<? super Z> columnValueComparator;
      if (sortParam.isAscending()) {
        columnValueComparator = new AscendingComparator<Z>();
      } else {
        columnValueComparator = new DescendingComparator<Z>();
      }
      // Row headers for cells with values
      final List<X> rowHeadersWithValues =
          table.getRowHeadersSorted(sortProperty, rowHeaderComparator, columnValueComparator);
      // Row headers for cells without values
      final List<X> rowHeadersWithoutValues = table.getRowHeadersSorted(rowHeaderComparator);
      rowHeadersWithoutValues.removeAll(rowHeadersWithValues);
      // All row headers
      rowHeaders = Lists.newArrayListWithExpectedSize(rowHeadersWithValues.size() + rowHeadersWithoutValues.size());
      rowHeaders.addAll(rowHeadersWithValues);
      rowHeaders.addAll(rowHeadersWithoutValues);
    }

    final List<TableRow<X, Y, Z>> tableRows = table.getTableRows(rowHeaders);
    return tableRows;
  }

  protected abstract Comparator<? super X> getRowHeaderComparator();

  @Override
  public void detach() {
    super.detach();
    tableModel.detach();
    tableRowsModel.detach();
  }

}
