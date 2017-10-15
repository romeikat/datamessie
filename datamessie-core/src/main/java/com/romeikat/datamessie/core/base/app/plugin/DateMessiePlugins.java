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
import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.util.ApplicationContentProvider;

public class DateMessiePlugins {

  private static DateMessiePlugins INSTANCE;

  private final ApplicationContext ctx;
  private final Map<Class<?>, Collection<? extends Object>> pluginsCache;
  private final Map<Class<?>, Object> beansCache;

  public static DateMessiePlugins getInstance(final ApplicationContext ctx) {
    if (INSTANCE == null) {
      INSTANCE = new DateMessiePlugins(ctx);
    }

    return INSTANCE;
  }

  public static DateMessiePlugins getInstance(final ApplicationContentProvider ctxProvider) {
    return getInstance(ctxProvider.getApplicationContext());
  }

  private DateMessiePlugins(final ApplicationContext ctx) {
    this.ctx = ctx;
    pluginsCache = Maps.newHashMap();
    beansCache = Maps.newHashMap();
  }

  public <P> P getOrLoadPlugin(final Class<P> pluginClazz) {
    final Collection<P> plugins = getOrLoadPlugins(pluginClazz);
    if (plugins.size() == 1) {
      return plugins.iterator().next();
    }

    return null;
  }

  public <P> Collection<P> getOrLoadPlugins(final Class<P> pluginClazz) {
    synchronized (pluginsCache) {
      // Get plugins from cache, if available
      if (pluginsCache.containsKey(pluginClazz)) {
        final Collection<P> plugins = getPluginsFromCache(pluginClazz);
        return plugins;
      }

      // Otherwise, load plugins for the first time and put into cache
      else {
        final Collection<P> plugins = loadPlugins(pluginClazz);
        this.pluginsCache.put(pluginClazz, plugins);
        return plugins;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <P> Collection<P> getPluginsFromCache(final Class<P> pluginClazz) {
    return (Collection<P>) pluginsCache.get(pluginClazz);
  }

  private <P> Collection<P> loadPlugins(final Class<P> pluginClazz) {
    final DataMessiePluginLoader<P> pluginLoader = new DataMessiePluginLoader<P>(pluginClazz, ctx);
    final Collection<P> plugins = pluginLoader.getPlugins();
    return plugins;
  }

  public <P, B> B getOrLoadBeanFromPlugin(final Class<P> pluginClazz, final Class<B> beanClazz) {
    synchronized (beansCache) {
      // Get bean from cache, if available
      if (beansCache.containsKey(beanClazz)) {
        final B bean = getBeanFromCache(beanClazz);
        return bean;
      }

      // Otherwise, load bean for the first time and put into cache
      else {
        final B bean = loadBean(pluginClazz, beanClazz);
        this.beansCache.put(beanClazz, bean);
        return bean;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <B> B getBeanFromCache(final Class<B> beanClazz) {
    return (B) beansCache.get(beanClazz);
  }

  private <B, P> B loadBean(final Class<P> pluginClazz, final Class<B> beanClazz) {
    final ApplicationContext pluginCtx = getPluginContext(pluginClazz);
    if (pluginCtx == null) {
      return null;
    }

    final B bean = pluginCtx.getBean(beanClazz);
    return bean;
  }

  public <P> ApplicationContext getPluginContext(final Class<P> pluginClazz) {
    final P plugin = getOrLoadPlugin(pluginClazz);
    if (plugin == null) {
      return null;
    }

    if (!IDataMessiePluginWithContext.class.isAssignableFrom(plugin.getClass())) {
      return null;
    }

    final IDataMessiePluginWithContext pluginWithCtx = (IDataMessiePluginWithContext) plugin;
    final ApplicationContext pluginContext = pluginWithCtx.getPluginContext();
    return pluginContext;
  }

}
