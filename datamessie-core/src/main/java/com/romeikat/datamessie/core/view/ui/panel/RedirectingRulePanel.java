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
import com.romeikat.datamessie.core.base.ui.component.LocalDateTextField;
import com.romeikat.datamessie.core.base.ui.component.RedirectingRuleModeSelector;
import com.romeikat.datamessie.core.domain.dto.RedirectingRuleDto;
import com.romeikat.datamessie.core.domain.enums.RedirectingRuleMode;

public class RedirectingRulePanel extends Panel {

  private static final long serialVersionUID = 1L;

  public RedirectingRulePanel(final String id,
      final IModel<RedirectingRuleDto> redirectingRuleModel) {
    super(id, redirectingRuleModel);

    // Regex
    final TextField<String> regexTextField =
        new TextField<String>("regex", new PropertyModel<String>(redirectingRuleModel, "regex"));
    add(regexTextField);
    regexTextField.add(new ModelUpdatingBehavior());
    // Regex group
    final TextField<String> regexGroupTextField = new TextField<String>("regexGroup",
        new PropertyModel<String>(redirectingRuleModel, "regexGroup"));
    add(regexGroupTextField);
    regexGroupTextField.add(new ModelUpdatingBehavior());

    // Active from
    final LocalDateTextField activeFromTextField = new LocalDateTextField("activeFrom",
        new PropertyModel<LocalDate>(redirectingRuleModel, "activeFrom"),
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
        new PropertyModel<LocalDate>(redirectingRuleModel, "activeTo"),
        new StyleDateConverter("M-", false));
    add(activeToTextField);
    activeToTextField.add(new ModelUpdatingBehavior());
    // Active to date picker
    final DatePicker activeToDatePicker = new DatePicker();
    activeToDatePicker.setShowOnFieldClick(true);
    activeToDatePicker.setAutoHide(true);
    activeToTextField.add(activeToDatePicker);

    // Mode
    final RedirectingRuleModeSelector modeSelector = new RedirectingRuleModeSelector("mode",
        new PropertyModel<RedirectingRuleMode>(redirectingRuleModel, "mode"));
    modeSelector.add(new ModelUpdatingBehavior());
    add(modeSelector);
  }

}
