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

import java.util.Collection;
import com.romeikat.datamessie.core.base.app.shared.ISharedBean;

public abstract class AbstractDataMessieModule implements IDataMessieModule {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ISharedBean> T getSharedBean(final Class<T> clazz) {
    final Collection<ISharedBean> sharedBeans = getSharedBeans();
    if (sharedBeans == null) {
      return null;
    }

    for (final ISharedBean sharedBean : sharedBeans) {
      if (clazz.isAssignableFrom(sharedBean.getClass())) {
        return (T) sharedBean;
      }
    }

    return null;
  }

  protected abstract Collection<ISharedBean> getSharedBeans();

}
