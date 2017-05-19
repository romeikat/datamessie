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

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;

public class TableNavigationToolbar extends AjaxNavigationToolbar {

  private static final long serialVersionUID = 1L;

  public TableNavigationToolbar(final AbstractTable<?, ?> table) {
    super(table);
  }

  @Override
  protected WebComponent newNavigatorLabel(final String navigatorId, final DataTable<?, ?> table) {
    final WebComponent navigatorLabel = new TableNavigatorLabel(navigatorId, (AbstractTable<?, ?>) table);
    return navigatorLabel;
  }

  @Override
  protected PagingNavigator newPagingNavigator(final String navigatorId, final DataTable<?, ?> table) {
    final PagingNavigator pagingNavigator = new TablePagingNavigator(navigatorId, (AbstractTable<?, ?>) table);
    return pagingNavigator;
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    setVisible(true);
  }

}
