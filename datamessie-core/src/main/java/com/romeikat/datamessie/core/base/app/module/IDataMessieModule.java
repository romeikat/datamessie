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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Page;
import org.apache.wicket.util.convert.IConverter;
import com.romeikat.datamessie.core.base.app.shared.ISharedBean;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;

public interface IDataMessieModule extends Serializable {

  boolean isEnabled();

  Integer getNumberOfRequiredDbConnections();

  Class<? extends Page> getHomePage();

  Map<String, Class<? extends Page>> getPagesToBeMounted();

  Map<Class<?>, IConverter<?>> getConverters();

  List<NavigationLink<? extends Page>> getNavigationLinks();

  List<SidePanel> getSidePanels();

  <T extends ISharedBean> T getSharedBean(Class<T> clazz);

}
