package com.romeikat.datamessie.core.base.app.plugin;

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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericXmlApplicationContext;

public abstract class AbstractDateMessiePluginWithGenericXmlApplicationContext
    implements IDataMessiePluginWithContext, ApplicationContextAware {

  private ApplicationContext appCtxWithPlugin;
  private GenericXmlApplicationContext pluginCtx;

  @Override
  public void setApplicationContext(final ApplicationContext appCtxWithPlugin) throws BeansException {
    this.appCtxWithPlugin = appCtxWithPlugin;
  }

  protected <T> T getPluginBean(final Class<T> clazz) {
    loadPluginCtx();

    return pluginCtx.getBean(clazz);
  }

  protected <T> T getPluginBean(final String name, final Class<T> clazz) {
    loadPluginCtx();

    return pluginCtx.getBean(name, clazz);
  }

  private void loadPluginCtx() {
    if (pluginCtx != null) {
      return;
    }

    pluginCtx = new GenericXmlApplicationContext();
    pluginCtx.setParent(appCtxWithPlugin);
    final String pluginCtxFile = getPluginCtxFile();
    pluginCtx.load(pluginCtxFile);
    pluginCtx.refresh();
  }

  protected abstract String getPluginCtxFile();

  @Override
  public ApplicationContext getPluginContext() {
    loadPluginCtx();

    return pluginCtx;
  }

}
