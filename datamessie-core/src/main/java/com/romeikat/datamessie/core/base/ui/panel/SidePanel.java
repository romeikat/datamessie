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

import org.apache.wicket.markup.html.panel.Panel;

public class SidePanel implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String CONTENT_ID = "sidePanel";

  private final Panel panel;

  private final int order;

  public SidePanel(final Panel panel, final int order) {
    this.panel = panel;
    this.order = order;
  }

  public int getOrder() {
    return order;
  }

  public Panel getPanel() {
    return panel;
  }

}
