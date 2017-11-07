package com.romeikat.datamessie.core.base.ui.panel;

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
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.dataprovider.AbstractTableDataProvider;
import com.romeikat.datamessie.core.base.ui.page.AbstractTable;
import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.ISingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.TableRow;

@AuthorizeInstantiation(DataMessieRoles.STATISTICS_PAGE)
public abstract class AbstractTablePanel<X extends Comparable<? super X>, Y extends Comparable<? super Y>, Z extends Serializable & Comparable<? super Z>>
    extends Panel {

  private static final long serialVersionUID = 1L;

  private final int DEFAULT_ROWS_PER_PAGE = 20;

  private IModel<ISingleTable<X, Y, Z>> tableModel;

  private List<IColumn<TableRow<X, Y, Z>, Y>> columns;

  public AbstractTablePanel(final String id) {
    super(id);

    tableModel = new LoadableDetachableModel<ISingleTable<X, Y, Z>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public ISingleTable<X, Y, Z> load() {
        return getTable();
      }
    };
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    columns = new ArrayList<IColumn<TableRow<X, Y, Z>, Y>>();

    // Row header column
    final AbstractColumn<TableRow<X, Y, Z>, Y> rowHeaderColumn =
        new AbstractColumn<TableRow<X, Y, Z>, Y>(getFirstColumnHeaderModel(), null) {
          private static final long serialVersionUID = 1L;

          @Override
          public void populateItem(final Item<ICellPopulator<TableRow<X, Y, Z>>> cellItem,
              final String componentId, final IModel<TableRow<X, Y, Z>> rowModel) {
            final TableRow<X, Y, Z> row = rowModel.getObject();
            final X rowHeader = row.getRowHeader();
            cellItem.add(getRowHeaderComponent(componentId, rowHeader));
          }
        };
    columns.add(rowHeaderColumn);

    // Value columns
    final ISingleTable<X, Y, Z> table = tableModel.getObject();
    final List<Y> columnHeaders = table.getColumnHeadersSorted(getColumnHeaderComparator());
    for (final Y columnHeader : columnHeaders) {
      final AbstractColumn<TableRow<X, Y, Z>, Y> valueColumn =
          new AbstractColumn<TableRow<X, Y, Z>, Y>(getColumnHeaderModel(columnHeader),
              columnHeader) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(final Item<ICellPopulator<TableRow<X, Y, Z>>> cellItem,
                final String componentId, final IModel<TableRow<X, Y, Z>> rowModel) {
              final TableRow<X, Y, Z> row = rowModel.getObject();
              final X rowHeader = row.getRowHeader();
              final Z value = row.getValues().get(columnHeader);
              final IModel<String> valueModel = getValueModel(value);
              cellItem.add(getValueComponent(componentId, rowHeader, columnHeader, valueModel));
            }
          };
      columns.add(valueColumn);
    }

    // Table
    final AbstractTableDataProvider<X, Y, Z> dataProvider =
        new AbstractTableDataProvider<X, Y, Z>(tableModel) {
          private static final long serialVersionUID = 1L;

          @Override
          protected Comparator<X> getRowHeaderComparator() {
            return AbstractTablePanel.this.getRowHeaderComparator();
          }
        };
    final DataTable<TableRow<X, Y, Z>, Y> dataTable =
        new AbstractTable<TableRow<X, Y, Z>, Y>("table", columns, dataProvider, getRowsPerPage()) {
          private static final long serialVersionUID = 1L;

          @Override
          protected String getSingularObjectName() {
            return AbstractTablePanel.this.getSingularObjectName();
          }

          @Override
          protected String getPluralObjectName() {
            return AbstractTablePanel.this.getPluralObjectName();
          }
        };
    addOrReplace(dataTable);
  }

  protected abstract ISingleTable<X, Y, Z> getTable();

  protected int getRowsPerPage() {
    return DEFAULT_ROWS_PER_PAGE;
  }

  protected abstract String getSingularObjectName();

  protected abstract String getPluralObjectName();

  protected abstract IModel<String> getFirstColumnHeaderModel();

  protected abstract IModel<String> getColumnHeaderModel(Y columnHeader);

  protected abstract Component getRowHeaderComponent(String componentId, X rowHeader);

  protected abstract Component getValueComponent(String componentId, X rowHeader, Y columnHeader,
      IModel<String> valueModel);

  protected abstract IModel<String> getValueModel(Z value);

  protected Comparator<X> getRowHeaderComparator() {
    return new AscendingComparator<X>();
  }

  protected Comparator<Y> getColumnHeaderComparator() {
    return new AscendingComparator<Y>();
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    tableModel.detach();
  }

}
