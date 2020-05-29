package com.romeikat.datamessie.core.base.app.shared;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.module.DataMessieModuleProvider;
import com.romeikat.datamessie.core.base.app.module.IDataMessieModule;
import com.romeikat.datamessie.core.base.util.comparator.SharedBeanOrderComparator;

@Service
public class SharedBeanProvider {

  private static final Logger LOG = LoggerFactory.getLogger(SharedBeanProvider.class);

  private static final SharedBeanOrderComparator ORDER_COMPARATOR =
      SharedBeanOrderComparator.INSTANCE;

  @Autowired
  private DataMessieModuleProvider dataMessieModuleProvider;

  public <T extends ISharedBean> T getSharedBean(final Class<T> clazz) {
    final List<T> sharedBeans = new LinkedList<T>();
    for (final IDataMessieModule dataMessieModule : dataMessieModuleProvider
        .getActiveDataMessieModules()) {
      final T moduleSharedBean = dataMessieModule.getSharedBean(clazz);
      if (moduleSharedBean != null) {
        sharedBeans.add(moduleSharedBean);
      }
    }
    if (sharedBeans.isEmpty()) {
      LOG.error("No shared bean of type {} found", clazz.getName());
      return null;
    }
    Collections.sort(sharedBeans, ORDER_COMPARATOR);
    return sharedBeans.iterator().next();
  }

}
