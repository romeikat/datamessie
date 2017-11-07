package com.romeikat.datamessie.core.base.init;

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
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.IExecutingPlugin;

@Service
public class ExecutingPluginsExecution {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutingPluginsExecution.class);

  @Autowired
  private ApplicationContext ctx;

  @PostConstruct
  private void initialize() {
    final Collection<IExecutingPlugin> plugins =
        DateMessiePlugins.getInstance(ctx).getOrLoadPlugins(IExecutingPlugin.class);
    for (final IExecutingPlugin plugin : plugins) {
      try {
        plugin.execute();
      } catch (final Exception e) {
        final String msg =
            String.format("Could not execute plugin %s", plugin.getClass().getName());
        LOG.error(msg, e);
      }
    }
  }

}
