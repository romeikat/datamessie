package com.romeikat.datamessie.core.base.ui.component;

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

import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;

public abstract class AjaxConfirmationLink<T> extends AjaxLink<T> {

  private static final long serialVersionUID = 1L;

  private final String text;

  public AjaxConfirmationLink(final String id, final String text) {
    super(id);
    this.text = text;
  }

  @Override
  protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
    super.updateAjaxAttributes(attributes);

    final AjaxCallListener ajaxCallListener = new AjaxCallListener();
    ajaxCallListener.onPrecondition("return confirm('" + text + "');");
    attributes.getAjaxCallListeners().add(ajaxCallListener);
  }
}
