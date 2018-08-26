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

import java.util.List;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.SourceOverviewDto;

public class SourceIdChoiceProvider extends AbstractIdBasedChoiceProvider<SourceOverviewDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<DocumentsFilterSettings> dfsModel;

  @SpringBean
  private SourceDao sourceDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourceIdChoiceProvider(final IModel<DocumentsFilterSettings> dfsModel) {
    super();
    Injector.get().inject(this);
    this.dfsModel = dfsModel;
  }

  @Override
  protected List<SourceOverviewDto> provideChoices() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final DocumentsFilterSettings documentsFilterSetting = dfsModel.getObject();
    final Long projectId =
        documentsFilterSetting == null ? null : documentsFilterSetting.getProjectId();
    final Long userId = DataMessieSession.get().getUserId();
    final List<SourceOverviewDto> choices = sourceDao.getAsOverviewDtos(
        sessionProvider.getStatelessSession(), userId, projectId, true, null, null);
    sessionProvider.closeStatelessSession();
    return choices;
  }

  @Override
  protected long provideId(final SourceOverviewDto choice) {
    return choice.getId();
  }

  @Override
  protected String provideDisplayText(final SourceOverviewDto choice) {
    return choice.getName();
  }

  @Override
  public void detach() {
    super.detach();

    dfsModel.detach();
  }

}
