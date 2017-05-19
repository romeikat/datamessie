package com.romeikat.datamessie.core.view.ui.page;

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

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;

import com.romeikat.datamessie.core.base.service.ProjectService;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;

@AuthorizeInstantiation(DataMessieRoles.PROJECT_PAGE)
public class ProjectPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  @SpringBean
  private ProjectService projectService;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public ProjectPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    // Form
    final Form<ProjectDto> projectForm =
        new Form<ProjectDto>("projectForm", new CompoundPropertyModel<ProjectDto>(getActiveProjectModel())) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void onSubmit() {
            // TODO: integrate when authentication is implemented
            // final HibernateSessionProvider sessionProvider = new
            // HibernateSessionProvider(sessionFactory);
            // new ExecuteWithTransaction(sessionProvider) {
            //
            // @Override
            // protected void execute(final StatelessSession statelessSession) {
            // projectService.updateProject(statelessSession, getModelObject());
            // }
            // }.execute();

            // TODO: response (on failure only)
            // PageParameters pageParameters = getPageParameters();
            // final String usernameValue = username.getModelObject();
            // pageParameters.set("username", usernameValue);
            // setResponsePage(SuccessPage.class, pageParameters);
          }

          @Override
          protected void onConfigure() {
            super.onConfigure();
            setVisible(getModelObject() != null);
          }
        };
    add(projectForm);

    // Name
    final TextField<String> nameTextField = new TextField<String>("name");
    nameTextField.setRequired(true);
    projectForm.add(nameTextField);
    // Crawling enabled
    final CheckBox crawlingEnabledCheckBox = new CheckBox("crawlingEnabled");
    projectForm.add(crawlingEnabledCheckBox);
    // Crawling interval
    final TextField<Integer> crawlingIntervalTextField = new TextField<Integer>("crawlingInterval");
    crawlingIntervalTextField.setRequired(true);
    projectForm.add(crawlingIntervalTextField);
    // Preprocessing enabled
    final CheckBox preprocessingEnabledCheckBox = new CheckBox("preprocessingEnabled");
    projectForm.add(preprocessingEnabledCheckBox);
  }

}
