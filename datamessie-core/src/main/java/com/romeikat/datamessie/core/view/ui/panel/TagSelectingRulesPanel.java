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

import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.romeikat.datamessie.core.base.ui.panel.DynamicListViewPanel;
import com.romeikat.datamessie.core.domain.dto.TagSelectingRuleDto;

public class TagSelectingRulesPanel extends DynamicListViewPanel<TagSelectingRuleDto> {

  private static final long serialVersionUID = 1L;

  public TagSelectingRulesPanel(final String id, final IModel<List<TagSelectingRuleDto>> itemsModel) {
    super(id, itemsModel);
  }

  @Override
  protected Panel getItemPanel(final String id, final IModel<TagSelectingRuleDto> tagSelectingRuleModel) {
    final Panel tagSelectingRulePanel = new TagSelectingRulePanel(id, tagSelectingRuleModel);
    return tagSelectingRulePanel;
  }

  @Override
  protected TagSelectingRuleDto newItem() {
    return new TagSelectingRuleDto();
  }

}
