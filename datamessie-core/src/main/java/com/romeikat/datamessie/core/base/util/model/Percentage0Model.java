package com.romeikat.datamessie.core.base.util.model;

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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import com.romeikat.datamessie.core.base.util.converter.PercentageConverter;

public class Percentage0Model extends LoadableDetachableModel<String> {

  private static final long serialVersionUID = 1L;

  private final IModel<Double> model;

  public Percentage0Model(final IModel<Double> model) {
    this.model = model;
  }

  public Percentage0Model(final Double value) {
    this(Model.of(value));
  }

  @Override
  protected String load() {
    final Double d = model.getObject();
    return PercentageConverter.INSTANCE_0.convertToString(d);
  }

}
