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
import org.apache.wicket.model.PropertyModel;
import org.wicketstuff.select2.Select2MultiChoice;
import org.wicketstuff.select2.Settings;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;

public class SourceTypeIdFilter extends Select2MultiChoice<Long> {

  private static final long serialVersionUID = 1L;

  private final IModel<Collection<Long>> selectedSourceTypeIdsModel;

  public SourceTypeIdFilter(final String id, final IModel<DocumentsFilterSettings> dfsModel) {
    super(id);

    // Choice Provider
    setProvider(new SourceTypeIdChoiceProvider());

    // Selected project model
    selectedSourceTypeIdsModel = new PropertyModel<Collection<Long>>(dfsModel, "sourceTypeIds");
    setModel(selectedSourceTypeIdsModel);

    // Settings
    final Settings settings = getSettings();
    settings.setWidth("300px");
    settings.setPlaceholder("Select source types (boolean AND)");
    settings.setAllowClear(true);
    settings.setMinimumResultsForSearch(20);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    selectedSourceTypeIdsModel.detach();
  }

}
