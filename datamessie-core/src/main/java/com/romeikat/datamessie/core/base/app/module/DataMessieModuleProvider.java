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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.IModulesProvider;

@Service
public class DataMessieModuleProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DataMessieModuleProvider.class);

  @Autowired
  private ApplicationContext ctx;

  @Autowired
  private List<IDataMessieModule> dataMessieModules;

  private List<IDataMessieModule> activeDataMessieModules;

  public synchronized List<IDataMessieModule> getActiveDataMessieModules() {
    // Active modules already determined
    if (activeDataMessieModules != null) {
      return activeDataMessieModules;
    }

    // Active modules not yet determined
    loadActiveDataMessieModules();
    return activeDataMessieModules;
  }

  private void loadActiveDataMessieModules() {
    activeDataMessieModules = new ArrayList<IDataMessieModule>();

    // Modules from core
    for (final IDataMessieModule dataMessieModule : dataMessieModules) {
      processModule(dataMessieModule);
    }

    // Modules from plugins
    final Collection<IModulesProvider> plugins =
        DateMessiePlugins.getInstance(ctx).getOrLoadPlugins(IModulesProvider.class);
    for (final IModulesProvider plugin : plugins) {
      final Collection<IDataMessieModule> dataMessieModules = plugin.provideModules();
      for (final IDataMessieModule dataMessieModule : dataMessieModules) {
        processModule(dataMessieModule);
      }
    }
  }

  private void processModule(final IDataMessieModule dataMessieModule) {
    if (dataMessieModule.isEnabled()) {
      activeDataMessieModules.add(dataMessieModule);
    }
    final String state = dataMessieModule.isEnabled() ? "enabled" : "disabled";
    LOG.debug("{} is {}", dataMessieModule.getClass().getSimpleName(), state);
  }

}
