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

import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.ui.choicerenderer.ProjectsChoiceRenderer;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;

public class ProjectSelector extends DropDownChoice<ProjectDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<List<ProjectDto>> allProjectsModel;

  private final IChoiceRenderer<ProjectDto> projectsChoiceRenderer;

  @SpringBean(name = "projectDao")
  private ProjectDao projectDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public ProjectSelector(final String id, final IModel<ProjectDto> selectedProjectModel) {
    super(id);

    // Selected project
    setModel(selectedProjectModel);

    // Project choices
    allProjectsModel = new LoadableDetachableModel<List<ProjectDto>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public List<ProjectDto> load() {
        return projectDao.getAllAsDtos(sessionFactory.getCurrentSession());
      }
    };
    setChoices(allProjectsModel);

    // Choice renderer
    projectsChoiceRenderer = new ProjectsChoiceRenderer();
    setChoiceRenderer(projectsChoiceRenderer);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    allProjectsModel.detach();
  }

}
