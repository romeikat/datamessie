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

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import com.romeikat.datamessie.core.base.ui.choicerenderer.RedirectingRuleModesChoiceRenderer;
import com.romeikat.datamessie.core.domain.enums.RedirectingRuleMode;

public class RedirectingRuleModeSelector extends DropDownChoice<RedirectingRuleMode> {

  private static final long serialVersionUID = 1L;

  private final IModel<List<RedirectingRuleMode>> allRedirectingRuleModesModel;

  private final IChoiceRenderer<RedirectingRuleMode> redirectingRuleModesChoiceRenderer;

  public RedirectingRuleModeSelector(final String id,
      final IModel<RedirectingRuleMode> redirectingRuleModeModel) {
    super(id);

    // Selected mode
    setModel(redirectingRuleModeModel);

    // Modes
    allRedirectingRuleModesModel = new LoadableDetachableModel<List<RedirectingRuleMode>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public List<RedirectingRuleMode> load() {
        return Arrays.asList(RedirectingRuleMode.values());
      }
    };
    setChoices(allRedirectingRuleModesModel);

    // Choice renderer
    redirectingRuleModesChoiceRenderer = new RedirectingRuleModesChoiceRenderer();
    setChoiceRenderer(redirectingRuleModesChoiceRenderer);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    allRedirectingRuleModesModel.detach();
  }

}
