package com.romeikat.datamessie.core.rss.app.module;

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
import java.util.List;
import java.util.Map;

import org.apache.wicket.Page;
import org.apache.wicket.util.convert.IConverter;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.romeikat.datamessie.core.base.app.module.AbstractDataMessieModule;
import com.romeikat.datamessie.core.base.app.shared.ISharedBean;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;

@Component
public class DataMessieRssModule extends AbstractDataMessieModule {

  private static final long serialVersionUID = 1L;

  @Value("${rss.module.enabled}")
  private boolean moduleEnabled;

  @Value("${crawling.sources.parallelism.factor}")
  private Double sourcesParallelismFactor;

  @Autowired
  private ProjectDao projectDao;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public boolean isEnabled() {
    return moduleEnabled;
  }

  @Override
  public Integer getNumberOfRequiredDbConnections() {
    // One crawling per project
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final long numberOfProjects = projectDao.countAll(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    final long numberOfRequiredDbConnectionsToDataMessie =
        ParallelProcessing.getNumberOfThreads(sourcesParallelismFactor) * numberOfProjects;

    return (int) numberOfRequiredDbConnectionsToDataMessie;
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return null;
  }

  @Override
  public Map<String, Class<? extends Page>> getPagesToBeMounted() {
    return null;
  }

  @Override
  public Map<Class<?>, IConverter<?>> getConverters() {
    return null;
  }

  @Override
  public List<NavigationLink<? extends Page>> getNavigationLinks() {
    return null;
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
