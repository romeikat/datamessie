package com.romeikat.datamessie.core.base.ui.component;

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
import org.apache.wicket.Page;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;

public class NavigationLink<T extends Page> implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String label;

  private final int order;

  private final Class<T> pageClass;

  private final PageParameters pageParameters;

  private final DataMessieRoles roles;

  public NavigationLink(final String label, final int order, final Class<T> pageClass,
      final String... roles) {
    this(label, order, pageClass, null, roles);
  }

  public NavigationLink(final String label, final int order, final Class<T> pageClass,
      final PageParameters pageParameters, final String... roles) {
    this.label = label;
    this.order = order;
    this.pageClass = pageClass;
    this.pageParameters = pageParameters;
    this.roles = DataMessieRoles.getRoles(roles);
  }

  public boolean isVisible(final DataMessieSession session) {
    final DataMessieRoles roles = session.getRoles();
    return roles.hasAnyRole(this.roles);
  }

  public String getLabel() {
    return label;
  }

  public int getOrder() {
    return order;
  }

  public Class<T> getPageClass() {
    return pageClass;
  }

  public PageParameters getPageParameters() {
    return pageParameters;
  }

  public DataMessieRoles getRoles() {
    return roles;
  }

}
