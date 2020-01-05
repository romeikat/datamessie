package com.romeikat.datamessie.dao.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (mysql)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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

import java.util.Objects;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {

  private static ApplicationContext CTX;

  @Override
  public void setApplicationContext(final ApplicationContext ctx) throws BeansException {
    CTX = ctx;
  }

  public static <T extends Object> T getBean(final Class<T> beanClass) {
    return CTX.getBean(beanClass);
  }

  public static String getPropertyValue(final String property) {
    String propertyValue = getPropertyValueFromApplication(CTX, property);
    if (propertyValue != null) {
      return propertyValue;
    }

    propertyValue = getPropertyValueFromEnvironment(CTX, property);
    if (propertyValue != null) {
      return propertyValue;
    }

    final String msg = String.format("Could not resolve property %s", property);
    throw new IllegalArgumentException(msg);
  }

  public static String getPropertyValueFromApplication(final ApplicationContext ctx,
      final String property) {
    if (ctx instanceof ConfigurableApplicationContext) {
      final ConfigurableApplicationContext confCtx = (ConfigurableApplicationContext) ctx;
      final ConfigurableListableBeanFactory beanFactory = confCtx.getBeanFactory();
      final String embeddedValue = "${" + property + "}";
      final String propertyValue = beanFactory.resolveEmbeddedValue(embeddedValue);
      // If no value resolvers are registered, Spring just returns the embedded value, so we must
      // work around this case
      if (propertyValue != null && !Objects.equals(propertyValue, embeddedValue)) {
        return propertyValue;
      }
    }

    final ApplicationContext parentCtx = ctx.getParent();
    if (parentCtx == null) {
      return null;
    } else {
      return getPropertyValueFromApplication(parentCtx, property);
    }
  }

  public static String getPropertyValueFromEnvironment(final ApplicationContext ctx,
      final String property) {
    final Environment env = ctx.getEnvironment();
    final String propertyValue = env.getProperty(property);
    if (propertyValue != null) {
      return propertyValue;
    }

    final ApplicationContext parentCtx = ctx.getParent();
    if (parentCtx == null) {
      return null;
    } else {
      return getPropertyValueFromEnvironment(parentCtx, property);
    }
  }

}
