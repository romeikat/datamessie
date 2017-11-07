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

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class TableBookmarkablePageLink<T> extends BookmarkablePageLink<T>
    implements ITableComponent {

  private static final long serialVersionUID = 1L;

  private final IModel<String> labelModel;

  public TableBookmarkablePageLink(final Class<? extends Page> pageClass,
      final PageParameters parameters, final IModel<String> labelModel) {
    super(ITableComponent.CONTENT_ID, pageClass, parameters);
    this.labelModel = labelModel;
  }

  protected IModel<String> getLabelModel() {
    return labelModel;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    labelModel.detach();
  }

}
