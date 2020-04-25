package com.romeikat.datamessie.core.view.ui.panel;

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

import java.time.LocalDate;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import com.romeikat.datamessie.core.base.ui.behavior.ModelUpdatingBehavior;
import com.romeikat.datamessie.core.base.ui.component.DeletingRuleModeSelector;
import com.romeikat.datamessie.core.base.ui.component.LocalDateTextField;
import com.romeikat.datamessie.core.domain.dto.DeletingRuleDto;
import com.romeikat.datamessie.core.domain.enums.DeletingRuleMode;

public class DeletingRulePanel extends Panel {

  private static final long serialVersionUID = 1L;

  public DeletingRulePanel(final String id, final IModel<DeletingRuleDto> deletingRuleModel) {
    super(id, deletingRuleModel);

    // Selector
    final TextField<String> selectorTextField =
        new TextField<String>("selector", new PropertyModel<String>(deletingRuleModel, "selector"));
    selectorTextField.add(new ModelUpdatingBehavior());
    add(selectorTextField);

    // Active from
    final LocalDateTextField activeFromTextField = new LocalDateTextField("activeFrom",
        new PropertyModel<LocalDate>(deletingRuleModel, "activeFrom"),
        new StyleDateConverter("M-", false));
    add(activeFromTextField);
    activeFromTextField.add(new ModelUpdatingBehavior());
    // Active from date picker
    final DatePicker activeFromDatePicker = new DatePicker();
    activeFromDatePicker.setShowOnFieldClick(true);
    activeFromDatePicker.setAutoHide(true);
    activeFromTextField.add(activeFromDatePicker);

    // Active to
    final LocalDateTextField activeToTextField = new LocalDateTextField("activeTo",
        new PropertyModel<LocalDate>(deletingRuleModel, "activeTo"),
        new StyleDateConverter("M-", false));
    add(activeToTextField);
    activeToTextField.add(new ModelUpdatingBehavior());
    // Active to date picker
    final DatePicker activeToDatePicker = new DatePicker();
    activeToDatePicker.setShowOnFieldClick(true);
    activeToDatePicker.setAutoHide(true);
    activeToTextField.add(activeToDatePicker);

    // Mode
    final DeletingRuleModeSelector modeSelector = new DeletingRuleModeSelector("mode",
        new PropertyModel<DeletingRuleMode>(deletingRuleModel, "mode"));
    modeSelector.add(new ModelUpdatingBehavior());
    add(modeSelector);
  }

}
