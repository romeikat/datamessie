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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

public class TableNavigatorLabel extends Label {

  private static final long serialVersionUID = 1L;

  public TableNavigatorLabel(final String id, final AbstractTable<?, ?> table) {
    super(id);

    final IModel<String> model = new AbstractReadOnlyModel<String>() {
      private static final long serialVersionUID = 1L;

      @Override
      public String getObject() {
        final long itemCount = table.getItemCount();
        final String objectName = itemCount == 1 ? table.getSingularObjectName() : table.getPluralObjectName();
        final String label = itemCount + " " + objectName;
        return label;
      }
    };
    setDefaultModel(model);
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    setVisible(true);
  }

}
