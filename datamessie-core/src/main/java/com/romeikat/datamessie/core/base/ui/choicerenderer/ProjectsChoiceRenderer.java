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

import com.romeikat.datamessie.core.domain.dto.ProjectDto;

public class ProjectsChoiceRenderer implements IChoiceRenderer<ProjectDto> {

  private static final long serialVersionUID = 1L;

  @Override
  public Object getDisplayValue(final ProjectDto object) {
    return object.getName();
  }

  @Override
  public String getIdValue(final ProjectDto object, final int index) {
    return Long.toString(object.getId());
  }

  @Override
  public ProjectDto getObject(final String id, final IModel<? extends List<? extends ProjectDto>> choices) {
    final List<? extends ProjectDto> projects = choices.getObject();
    for (final ProjectDto project : projects) {
      if (Long.toString(project.getId()).equals(id)) {
        return project;
      }
    }

    return null;
  }

}
