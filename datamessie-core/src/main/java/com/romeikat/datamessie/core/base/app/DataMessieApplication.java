package com.romeikat.datamessie.core.base.app;

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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Application;
import org.apache.wicket.ConverterLocator;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.convert.IConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.app.module.DataMessieModuleProvider;
import com.romeikat.datamessie.core.base.app.module.IDataMessieModule;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.page.SignInPage;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;
import com.romeikat.datamessie.core.base.util.DataMessieException;
import com.romeikat.datamessie.core.base.util.comparator.NavigationLinkOrderComparator;
import com.romeikat.datamessie.core.base.util.comparator.SidePanelsOrderComparator;

@Component
public class DataMessieApplication extends AuthenticatedWebApplication {

  private static final Logger LOG = LoggerFactory.getLogger(DataMessieApplication.class);

  private static final int REQUEST_TIMEOUT = 180;

  @Autowired
  private DataMessieModuleProvider dataMessieModuleProvider;

  @Value("${connections.maxPoolSize}")
  private int maxPoolSizeForDataMessie;

  @Override
  public void init() {
    super.init();
    inject();
    setLogging();
    checkDbConnectionPool();
    setTimeout();
    mountPages();
  }

  private void inject() {
    getComponentInstantiationListeners().add(new SpringComponentInjector(this));
  }

  private void setLogging() {
    System.setProperty("java.util.logging.config.file", "logging.properties");
  }

  private void checkDbConnectionPool() {
    int totalNumberOfRequiredDbConnections = 0;
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final Integer numberOfRequiredDbConnections =
          dataMessieModule.getNumberOfRequiredDbConnections();
      if (numberOfRequiredDbConnections == null) {
        continue;
      }

      if (numberOfRequiredDbConnections != null) {
        totalNumberOfRequiredDbConnections += numberOfRequiredDbConnections;
      }
    }

    if (totalNumberOfRequiredDbConnections > maxPoolSizeForDataMessie) {
      LOG.warn("{} connections to the data.messie database are required, but only {} are available",
          totalNumberOfRequiredDbConnections, maxPoolSizeForDataMessie);
    }
  }

  private void setTimeout() {
    getRequestCycleSettings()
        .setTimeout(org.apache.wicket.util.time.Duration.minutes(REQUEST_TIMEOUT));
  }

  private void mountPages() {
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final Map<String, Class<? extends Page>> pagesToBeMounted =
          dataMessieModule.getPagesToBeMounted();
      if (pagesToBeMounted != null) {
        for (final String path : pagesToBeMounted.keySet()) {
          final Class<? extends Page> clazz = pagesToBeMounted.get(path);
          mountPage(path, clazz);
        }
      }
    }
  }

  @Override
  protected IConverterLocator newConverterLocator() {
    final ConverterLocator converterLocator = (ConverterLocator) super.newConverterLocator();
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final Map<Class<?>, IConverter<?>> converters = dataMessieModule.getConverters();
      if (converters != null) {
        for (final Class<?> clazz : converters.keySet()) {
          final IConverter<?> converter = converters.get(clazz);
          converterLocator.set(clazz, converter);
        }
      }
    }
    return converterLocator;
  }

  public static DataMessieApplication get() {
    return (DataMessieApplication) Application.get();
  }

  /**
   * @see org.apache.wicket.Application#getHomePage(org.apache.wicket.request.Request,
   *      org.apache.wicket.request.Response)
   */
  @Override
  public DataMessieSession newSession(final Request request, final Response response) {
    return new DataMessieSession(request);
  }

  /**
   * @see org.apache.wicket.Application#getHomePage()
   */
  @Override
  public Class<? extends Page> getHomePage() {
    final List<Class<? extends Page>> homePages = Lists.newLinkedList();
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final Class<? extends Page> homePage = dataMessieModule.getHomePage();
      if (homePage != null) {
        homePages.add(homePage);
      }
    }
    if (homePages.size() != 1) {
      throw new DataMessieException(
          String.format("Modules must provide a unique home page, but provide {} home pages"));
    }
    final Class<? extends Page> homePage = homePages.get(0);
    return homePage;
  }

  public List<NavigationLink<? extends Page>> getNavigationLinks() {
    final List<NavigationLink<? extends Page>> navigationLinks =
        new LinkedList<NavigationLink<? extends Page>>();
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final List<NavigationLink<? extends Page>> moduleNavigationLinks =
          dataMessieModule.getNavigationLinks();
      if (moduleNavigationLinks != null) {
        navigationLinks.addAll(moduleNavigationLinks);
      }
    }
    Collections.sort(navigationLinks, NavigationLinkOrderComparator.INSTANCE);
    return navigationLinks;
  }

  public List<SidePanel> getSidePanels() {
    final List<SidePanel> sidePanels = new LinkedList<SidePanel>();
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final List<? extends SidePanel> moduleSidePanels = dataMessieModule.getSidePanels();
      if (moduleSidePanels != null) {
        sidePanels.addAll(moduleSidePanels);
      }
    }
    Collections.sort(sidePanels, SidePanelsOrderComparator.INSTANCE);
    return sidePanels;
  }

  @Override
  protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
    return DataMessieSession.class;
  }

  @Override
  protected Class<? extends WebPage> getSignInPageClass() {
    return SignInPage.class;
  }

}
