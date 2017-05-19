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

import java.util.Collection;

import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.Select2MultiChoice;
import org.wicketstuff.select2.Settings;

import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;

public class SourceTypeChoice extends Select2MultiChoice<SourceTypeDto> {

  private static final long serialVersionUID = 1L;

  public SourceTypeChoice(final String id) {
    this(id, null);
  }

  public SourceTypeChoice(final String id, final IModel<Collection<SourceTypeDto>> model) {
    super(id, model);

    // Choice Provider
    setProvider(new SourceTypeChoiceProvider());

    // Settings
    final Settings settings = getSettings();
    settings.setWidth("150px");
    settings.setPlaceholder("Source types");
    settings.setAllowClear(true);
    settings.setMinimumResultsForSearch(20);
  }

  public SourceTypeChoice setWidth(final int width) {
    final Settings settings = getSettings();
    settings.setWidth(width + "px");
    return this;
  }

}
