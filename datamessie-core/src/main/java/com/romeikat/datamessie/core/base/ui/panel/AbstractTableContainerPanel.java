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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class AbstractTableContainerPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private AjaxLazyLoadPanel tablePanel;

  public AbstractTableContainerPanel(final String id) {
    super(id);
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    tablePanel = new AjaxLazyLoadPanel("tablePanel") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        final AbstractTablePanel<?, ?, ?> tablePanel = createTablePanel(id);
        tablePanel.setOutputMarkupId(true);
        return tablePanel;
      }
    };
    tablePanel.setOutputMarkupId(true);
    addOrReplace(tablePanel);

  }

  protected abstract AbstractTablePanel<?, ?, ?> createTablePanel(String id);

}
