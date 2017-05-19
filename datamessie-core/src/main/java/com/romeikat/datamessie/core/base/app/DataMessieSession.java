package com.romeikat.datamessie.core.base.app;

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
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.romeikat.datamessie.core.base.service.AuthenticationService;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.enums.StatisticsInterval;
import com.romeikat.datamessie.core.domain.enums.StatisticsType;

public class DataMessieSession extends AuthenticatedWebSession {

  private static final long serialVersionUID = 1L;

  private String username;

  private String passwordHash;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private final IModel<StatisticsType> statisticsTypeModel;

  private final IModel<Integer> statisticsPeriodModel;

  private final IModel<StatisticsInterval> statisticsIntervalModel;

  @SpringBean
  private AuthenticationService authenticationService;

  public DataMessieSession(final Request request) {
    super(request);
    Injector.get().inject(this);
    bind();
    dfsModel = new Model<DocumentsFilterSettings>();
    dfsModel.setObject(new DocumentsFilterSettings());
    statisticsTypeModel = new Model<StatisticsType>();
    statisticsTypeModel.setObject(StatisticsType.DOWNLOADED_DOCUMENTS);
    statisticsPeriodModel = new Model<Integer>();
    statisticsPeriodModel.setObject(10);
    statisticsIntervalModel = new Model<StatisticsInterval>();
    statisticsIntervalModel.setObject(StatisticsInterval.DAY);
  }

  public static DataMessieSession get() {
    return (DataMessieSession) Session.get();
  }

  @Override
  public boolean authenticate(final String username, final String password) {
    final boolean authSuccess = authenticationService.authenticate(username, password);
    if (authSuccess) {
      this.username = username;
      passwordHash = authenticationService.getHash(password);
    }
    return authSuccess;
  }

  @Override
  public DataMessieRoles getRoles() {
    final DataMessieRoles roles = authenticationService.getRoles(username, passwordHash, isSignedIn());
    return roles;
  }

  @Override
  public void signOut() {
    super.signOut();
    username = null;
  }

  public String getUsername() {
    return username;
  }

  public IModel<DocumentsFilterSettings> getDocumentsFilterSettingsModel() {
    return dfsModel;
  }

  public DocumentsFilterSettings getDocumentsFilterSettings() {
    return dfsModel.getObject();
  }

  public IModel<StatisticsType> getStatisticsTypeModel() {
    return statisticsTypeModel;
  }

  public IModel<Integer> getStatisticsPeriodModel() {
    return statisticsPeriodModel;
  }

  public IModel<StatisticsInterval> getStatisticsIntervalModel() {
    return statisticsIntervalModel;
  }

}
