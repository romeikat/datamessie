package com.romeikat.datamessie.core.base.ui.behavior;

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
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;

public class FocusBehavior extends Behavior {
  private static final long serialVersionUID = 1L;

  @Override
  public void bind(final Component component) {
    if (component == null || !FormComponent.class.isAssignableFrom(component.getClass())) {
      throw new IllegalArgumentException("FocusBehavior must be bound to a FormComponent");
    }
    component.setOutputMarkupId(true);
  }

  @Override
  public void renderHead(final Component component, final IHeaderResponse iHeaderResponse) {
    super.renderHead(component, iHeaderResponse);
    iHeaderResponse.render(OnLoadHeaderItem
        .forScript("document.getElementById('" + component.getMarkupId() + "').focus();"));
  }

  @Override
  public boolean isTemporary(final Component component) {
    return true;
  }

}
