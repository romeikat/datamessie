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

import java.util.Collection;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class DataMessiePluginLoader<T> {

  private static final Class<DataMessiePlugin> PLUGIN_ANNOTATION_TYPE = DataMessiePlugin.class;
  private static final String PLUGIN_BASE_PACKAGE = "com.romeikat.datamessie.plugin";

  private final Class<T> pluginType;
  private AnnotationConfigApplicationContext appCtxWithPlugin;

  private final ApplicationContext appCtx;

  public DataMessiePluginLoader(final Class<T> pluginType, final ApplicationContext appCtx) {
    this.pluginType = pluginType;
    this.appCtx = appCtx;
  }

  /**
   * Returns the only plugin annotated with type T.
   *
   * @return The only respective plugin
   */
  public T getPlugin() {
    loadAppCtxWithPlugin();

    try {
      final T plugin = appCtxWithPlugin.getBean(pluginType);
      return plugin;
    } catch (final NoSuchBeanDefinitionException e) {
      return null;
    }
  }

  /**
   * Returns all plugins annotated with type T.
   *
   * @return The respective plugins
   */
  public Collection<T> getPlugins() {
    loadAppCtxWithPlugin();

    final Collection<T> plugins = appCtxWithPlugin.getBeansOfType(pluginType).values();
    return plugins;
  }

  private void loadAppCtxWithPlugin() {
    if (appCtxWithPlugin != null) {
      return;
    }

    // Create the annotation-based context
    appCtxWithPlugin = new AnnotationConfigApplicationContext();
    appCtxWithPlugin.setParent(appCtx);

    // Scan for classes annotated with @<PluginAnnotaionType>,
    // do not include standard Spring annotations in scan
    final ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(appCtxWithPlugin, false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(PLUGIN_ANNOTATION_TYPE));
    scanner.scan(PLUGIN_BASE_PACKAGE);

    appCtxWithPlugin.refresh();
  }

}
