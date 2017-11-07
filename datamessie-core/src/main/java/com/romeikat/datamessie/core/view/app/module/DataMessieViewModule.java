package com.romeikat.datamessie.core.view.app.module;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Page;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.module.AbstractDataMessieModule;
import com.romeikat.datamessie.core.base.app.shared.ISharedBean;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.page.SignInPage;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;
import com.romeikat.datamessie.core.view.ui.page.CrawlingsPage;
import com.romeikat.datamessie.core.view.ui.page.DocumentPage;
import com.romeikat.datamessie.core.view.ui.page.DocumentsPage;
import com.romeikat.datamessie.core.view.ui.page.ProjectPage;
import com.romeikat.datamessie.core.view.ui.page.SourcePage;
import com.romeikat.datamessie.core.view.ui.page.SourcesPage;

@Component
public class DataMessieViewModule extends AbstractDataMessieModule {

  private static final long serialVersionUID = 1L;

  @Value("${view.module.enabled}")
  private boolean moduleEnabled;

  @Override
  public Integer getNumberOfRequiredDbConnections() {
    return 1;
  }

  @Override
  public boolean isEnabled() {
    return moduleEnabled;
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return null;
  }

  @Override
  public Map<String, Class<? extends Page>> getPagesToBeMounted() {
    final Map<String, Class<? extends Page>> pagesToBeMounted = Maps.newHashMap();
    pagesToBeMounted.put("signin", SignInPage.class);
    pagesToBeMounted.put("project", ProjectPage.class);
    pagesToBeMounted.put("crawlings", CrawlingsPage.class);
    pagesToBeMounted.put("sources", SourcesPage.class);
    pagesToBeMounted.put("source", SourcePage.class);
    pagesToBeMounted.put("documents", DocumentsPage.class);
    pagesToBeMounted.put("document", DocumentPage.class);
    return pagesToBeMounted;
  }

  @Override
  public Map<Class<?>, IConverter<?>> getConverters() {
    return null;
  }

  @Override
  public List<NavigationLink<? extends Page>> getNavigationLinks() {
    final List<NavigationLink<? extends Page>> navigationLinks =
        new LinkedList<NavigationLink<? extends Page>>();
    // Project
    navigationLinks.add(new NavigationLink<ProjectPage>("Project", 2, ProjectPage.class,
        DataMessieRoles.PROJECT_PAGE));
    // Sources
    navigationLinks.add(new NavigationLink<SourcesPage>("Sources", 3, SourcesPage.class,
        DataMessieRoles.SOURCES_PAGE));
    // Crawlings
    navigationLinks.add(new NavigationLink<CrawlingsPage>("Crawlings", 4, CrawlingsPage.class,
        DataMessieRoles.CRAWLINGS_PAGE));
    // Documents
    final NavigationLink<DocumentsPage> documentsNavigationLink = new NavigationLink<DocumentsPage>(
        "Documents", 5, DocumentsPage.class, DataMessieRoles.DOCUMENTS_PAGE);
    navigationLinks.add(documentsNavigationLink);
    // Done
    return navigationLinks;
  }

  @Override
  public List<SidePanel> getSidePanels() {
    return null;
  }


  @Override
  protected Collection<ISharedBean> getSharedBeans() {
    return null;
  }

}
