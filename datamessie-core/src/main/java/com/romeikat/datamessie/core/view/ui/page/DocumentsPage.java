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
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.page.AbstractDocumentsFilterPage;
import com.romeikat.datamessie.core.view.ui.panel.DocumentsOverviewPanel;

@AuthorizeInstantiation(DataMessieRoles.DOCUMENTS_PAGE)
public class DocumentsPage extends AbstractDocumentsFilterPage {

  private static final long serialVersionUID = 1L;

  private AjaxLazyLoadPanel documentsOverviewPanel;

  public DocumentsPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    // Documents overview
    documentsOverviewPanel = new AjaxLazyLoadPanel("documentsOverviewPanel") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        return new DocumentsOverviewPanel(id,
            DataMessieSession.get().getDocumentsFilterSettingsModel());
      }

      @Override
      public void onConfigure() {
        super.onConfigure();
        setVisible(getActiveProject() != null);
      }
    };
    documentsOverviewPanel.setOutputMarkupId(true);
    add(documentsOverviewPanel);
  }

  @Override
  protected Class<? extends Page> getNavigationLinkClass() {
    return DocumentsPage.class;
  }

}
