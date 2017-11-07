package com.romeikat.datamessie.core.base.ui.choicerenderer;

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
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

public abstract class EnumChoiceRenderer<E extends Enum<E>> implements IChoiceRenderer<E> {

  private static final long serialVersionUID = 1L;

  private final Class<E> clazz;

  public EnumChoiceRenderer(final Class<E> clazz) {
    this.clazz = clazz;
  }

  @Override
  public String getIdValue(final E object, final int index) {
    return object.name();
  }

  @Override
  public E getObject(final String id, final IModel<? extends List<? extends E>> choices) {
    return Enum.valueOf(clazz, id);
  }

}
