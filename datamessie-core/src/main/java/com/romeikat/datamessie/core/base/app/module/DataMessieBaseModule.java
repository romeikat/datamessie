package com.romeikat.datamessie.core.base.app.module;

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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Page;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.shared.IFullTextSearcher;
import com.romeikat.datamessie.core.base.app.shared.ISharedBean;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.page.StatisticsPage;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;
import com.romeikat.datamessie.core.base.ui.panel.StatisticsPanel;
import com.romeikat.datamessie.core.base.util.converter.DateConverter;
import com.romeikat.datamessie.core.base.util.converter.DocumentProcessingStateConverter;
import com.romeikat.datamessie.core.base.util.converter.DurationConverter;
import com.romeikat.datamessie.core.base.util.converter.LanguageConverter;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.base.util.converter.LocalDateTimeConverter;
import com.romeikat.datamessie.core.base.util.converter.TaskExecutionStatusConverter;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;
import com.romeikat.datamessie.model.enums.Language;
import jersey.repackaged.com.google.common.collect.Lists;

@Component
public class DataMessieBaseModule extends AbstractDataMessieModule {

  private static final long serialVersionUID = 1L;

  @Autowired
  private IFullTextSearcher remoteFullTextSearcher;

  @Autowired
  private IStatisticsManager remoteStatisticsManager;

  @Value("${base.module.enabled}")
  private boolean moduleEnabled;

  @Value("${documents.loading.parallelism.factor}")
  private Double documentsParallelismFactor;

  @Override
  public boolean isEnabled() {
    return moduleEnabled;
  }

  @Override
  public Integer getNumberOfRequiredDbConnections() {
    final long numberOfRequiredDbConnectionsToDataMessie =
        ParallelProcessing.getNumberOfThreads(documentsParallelismFactor);
    return (int) numberOfRequiredDbConnectionsToDataMessie;
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return StatisticsPage.class;
  }

  @Override
  public Map<String, Class<? extends Page>> getPagesToBeMounted() {
    final Map<String, Class<? extends Page>> pagesToBeMounted = Maps.newHashMap();
    pagesToBeMounted.put("statistics", StatisticsPage.class);
    return pagesToBeMounted;
  }

  @Override
  public Map<Class<?>, IConverter<?>> getConverters() {
    final Map<Class<?>, IConverter<?>> converters = Maps.newHashMap();
    converters.put(Date.class, DateConverter.INSTANCE_UI);
    converters.put(LocalDate.class, LocalDateConverter.INSTANCE_UI);
    converters.put(LocalDateTime.class, LocalDateTimeConverter.INSTANCE_UI);
    converters.put(Duration.class, DurationConverter.INSTANCE);
    converters.put(TaskExecutionStatus.class, TaskExecutionStatusConverter.INSTANCE);
    converters.put(DocumentProcessingStateConverter.class,
        DocumentProcessingStateConverter.INSTANCE);
    converters.put(Language.class, LanguageConverter.INSTANCE);
    return converters;
  }

  @Override
  public List<NavigationLink<? extends Page>> getNavigationLinks() {
    final List<NavigationLink<? extends Page>> navigationLinks =
        new LinkedList<NavigationLink<? extends Page>>();
    // Statistics
    final NavigationLink<StatisticsPage> statisticsNavigationLink =
        new NavigationLink<StatisticsPage>("Statistics", 1, StatisticsPage.class,
            DataMessieRoles.STATISTICS_PAGE);
    navigationLinks.add(statisticsNavigationLink);
    // Done
    return navigationLinks;
  }

  @Override
  public List<SidePanel> getSidePanels() {
    final List<SidePanel> sidePanels = new LinkedList<SidePanel>();
    sidePanels.add(new SidePanel(new StatisticsPanel(SidePanel.CONTENT_ID), 1));
    return sidePanels;
  }

  @Override
  protected Collection<ISharedBean> getSharedBeans() {
    return Lists.newArrayList(remoteFullTextSearcher, remoteStatisticsManager);
  }

}
