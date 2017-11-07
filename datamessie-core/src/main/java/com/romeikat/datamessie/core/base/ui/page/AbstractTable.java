package com.romeikat.datamessie.core.base.ui.page;

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
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;

public abstract class AbstractTable<T, S> extends DataTable<T, S> {

  private static final long serialVersionUID = 1L;

  public AbstractTable(final String id, final List<? extends IColumn<T, S>> columns,
      final ISortableDataProvider<T, S> dataProvider, final int rowsPerPage) {
    super(id, columns, dataProvider, rowsPerPage);

    setOutputMarkupId(true);
    setVersioned(false);

    addTopToolbar(new TableNavigationToolbar(this));
    addTopToolbar(new AjaxFallbackHeadersToolbar<S>(this, dataProvider));
    addBottomToolbar(new NoRecordsToolbar(this));
  }

  @Override
  protected Item<T> newRowItem(final String id, final int index, final IModel<T> model) {
    return new OddEvenItem<T>(id, index, model);
  }

  protected abstract String getSingularObjectName();

  protected abstract String getPluralObjectName();

}
