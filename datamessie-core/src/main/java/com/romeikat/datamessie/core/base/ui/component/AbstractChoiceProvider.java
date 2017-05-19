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

import org.apache.wicket.ajax.json.JSONException;
import org.apache.wicket.ajax.json.JSONWriter;
import org.wicketstuff.select2.ChoiceProvider;

public abstract class AbstractChoiceProvider<T> implements ChoiceProvider<T> {

  private static final long serialVersionUID = 1L;

  public abstract String getDisplayValue(T choiceId);

  public abstract String getIdValue(T choice);

  @Override
  public final void toJson(final T choice, final JSONWriter writer) throws JSONException {
    writer.key("id").value(getIdValue(choice)).key("text").value(getDisplayValue(choice));
  }

  @Override
  public void detach() {}

}
