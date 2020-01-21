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

import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.app.CurrentDocumentFilterSettingsModel;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.service.SourceService;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.SourceDto;
import com.romeikat.datamessie.core.view.ui.panel.SourcesOverviewPanel;

@AuthorizeInstantiation(DataMessieRoles.SOURCES_PAGE)
public class SourcesPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private AjaxLazyLoadPanel sourcesOverviewPanel;

  private Link<Void> addSourceLink;

  @SpringBean
  private SourceService sourceService;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourcesPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    // Sources overview
    sourcesOverviewPanel = new AjaxLazyLoadPanel("sourcesOverviewPanel") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        return new SourcesOverviewPanel(id, new CurrentDocumentFilterSettingsModel());
      }

      @Override
      public void onConfigure() {
        super.onConfigure();
        setVisible(getActiveProject() != null);
      }
    };
    sourcesOverviewPanel.setOutputMarkupId(true);
    add(sourcesOverviewPanel);

    // Link to add a new source, if no sources have been added yet
    addSourceLink = new Link<Void>("addSource") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick() {
        final DocumentsFilterSettings dfs = DataMessieSession.get().getDocumentsFilterSettings();
        final Long projectId = dfs.getProjectId();

        // Create
        final HibernateSessionProvider sessionProvider =
            new HibernateSessionProvider(sessionFactory);
        final Long userId = DataMessieSession.get().getUserId();
        final SourceDto newSource =
            sourceService.createSource(sessionProvider.getStatelessSession(), userId, projectId);

        // Open
        final PageParameters sourcePageParameters = createProjectPageParameters();
        sourcePageParameters.set("id", newSource.getId());
        setResponsePage(SourcePage.class, sourcePageParameters);
      }
    };
    addSourceLink.setOutputMarkupId(true);
    add(addSourceLink);
  }

}
