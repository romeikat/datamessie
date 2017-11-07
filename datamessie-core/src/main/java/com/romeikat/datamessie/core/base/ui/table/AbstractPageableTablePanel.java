package com.romeikat.datamessie.core.base.ui.table;

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
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public abstract class AbstractPageableTablePanel<T extends Serializable> extends Panel {

  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_ROWS_PER_PAGE = 10;

  private final int rowsPerPage;

  private DataView<T> rowsList;

  private IDataProvider<T> rowsDataProvider;

  private PagingNavigator pagingNavigator;

  public AbstractPageableTablePanel(final String id) {
    this(id, DEFAULT_ROWS_PER_PAGE);
  }

  public AbstractPageableTablePanel(final String id, final int rowsPerPage) {
    super(id);
    this.rowsPerPage = rowsPerPage;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    setOutputMarkupId(true);

    final List<ITableColumn<T>> tableColumns = getTalbeColumns();

    // Paging navigator
    pagingNavigator = new AjaxPagingNavigator("pagingNavigator", rowsList) {
      private static final long serialVersionUID = 1L;

      @Override
      public void onConfigure() {
        super.onConfigure();
        final long pageCount = getPageable().getPageCount();
        setVisible(pageCount > 1);
      }
    };
    pagingNavigator.setOutputMarkupId(true);
    add(pagingNavigator);

    // Headline
    final ListView<ITableColumn<T>> columnHeadersListView =
        new ListView<ITableColumn<T>>("columnHeaders", tableColumns) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final ListItem<ITableColumn<T>> item) {
            final ITableColumn<T> tableColumn = item.getModelObject();
            final ITableComponent columnHeader = tableColumn.getColumnHeader();
            addToTable(item, columnHeader);
          }
        };
    add(columnHeadersListView);

    // Rows
    rowsDataProvider = new RowsDataProvider();
    rowsList = new DataView<T>("rowsList", rowsDataProvider) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final Item<T> item) {
        final IModel<T> rowModel = item.getModel();
        final ListView<ITableColumn<T>> rowColumnsListView =
            new ListView<ITableColumn<T>>("columnValues", tableColumns) {
              private static final long serialVersionUID = 1L;

              @Override
              protected void populateItem(final ListItem<ITableColumn<T>> item) {
                final ITableColumn<T> tableColumn = item.getModelObject();
                final ITableComponent columnValue = tableColumn.getColumnValue(rowModel);
                addToTable(item, columnValue);
              }
            };
        item.add(rowColumnsListView);
      }
    };
    rowsList.setItemsPerPage(rowsPerPage);
    add(rowsList);

    // Number of rows
    final long numberOfRows = rowsDataProvider.size();
    final String suffix = numberOfRows == 1 ? getRowsSingularName() : getRowsPluralName();
    final Label numberOfRowsLabel = new Label("numberOfRows", numberOfRows + " " + suffix);
    add(numberOfRowsLabel);
  }

  private void addToTable(final ListItem<ITableColumn<T>> tableColumnItem,
      final ITableComponent tableComponent) {
    if (tableComponent instanceof TableLabel) {
      final TableLabel tableLabel = (TableLabel) tableComponent;
      final TableLabelPanel tableLabelPanel = new TableLabelPanel("columnValue", tableLabel);
      tableColumnItem.add(tableLabelPanel);
    } else if (tableComponent instanceof TableBookmarkablePageLink) {
      final TableBookmarkablePageLink<?> link = (TableBookmarkablePageLink<?>) tableComponent;
      final TableBookmarkablePageLinkPanel tableLabelPanel =
          new TableBookmarkablePageLinkPanel("columnValue", link);
      tableColumnItem.add(tableLabelPanel);
    }

  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  protected abstract String getRowsSingularName();

  protected abstract String getRowsPluralName();

  protected abstract long getNumberOfRows();

  protected abstract List<? extends T> getRows(final long first, final long count);

  protected abstract List<ITableColumn<T>> getTalbeColumns();

  private class RowsDataProvider implements IDataProvider<T> {

    private static final long serialVersionUID = 1L;

    private IModel<Long> numberOfRowsModel;

    private RowsDataProvider() {
      // Load number of rows only once per request
      numberOfRowsModel = new LoadableDetachableModel<Long>() {
        private static final long serialVersionUID = 1L;

        @Override
        public Long load() {
          final long numberOfRows = AbstractPageableTablePanel.this.getNumberOfRows();
          return numberOfRows;
        }
      };
    }

    @Override
    public long size() {
      final long numberOfRows = numberOfRowsModel.getObject();
      return numberOfRows;
    }

    @Override
    public Iterator<? extends T> iterator(final long first, final long count) {
      final List<? extends T> rows = AbstractPageableTablePanel.this.getRows(first, count);
      return rows.iterator();
    }

    @Override
    public IModel<T> model(final T object) {
      return Model.of(object);
    }

    @Override
    public void detach() {
      numberOfRowsModel.detach();
    }

  }

}
