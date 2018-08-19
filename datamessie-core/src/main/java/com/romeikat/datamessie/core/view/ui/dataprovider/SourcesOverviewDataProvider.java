package com.romeikat.datamessie.core.view.ui.dataprovider;

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

import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.dto.SourceOverviewDto;

public class SourcesOverviewDataProvider implements IDataProvider<SourceOverviewDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private final SourceDao sourceDao;

  private final SessionFactory sessionFactory;

  private final IModel<Long> numberOfSourcesModel;

  public SourcesOverviewDataProvider(final IModel<DocumentsFilterSettings> dfsModel,
      final SourceDao sourceDao, final SessionFactory sessionFactory) {
    this.dfsModel = dfsModel;
    this.sourceDao = sourceDao;
    this.sessionFactory = sessionFactory;

    // Loads number of documents only once per request
    numberOfSourcesModel = new LoadableDetachableModel<Long>() {
      private static final long serialVersionUID = 1L;

      @Override
      public Long load() {
        final Long numberOfDocuments = sourceDao.count(sessionFactory.getCurrentSession(),
            dfsModel.getObject().getProjectId());
        return numberOfDocuments;
      }
    };
  }

  @Override
  public Iterator<? extends SourceOverviewDto> iterator(final long first, final long count) {
    final List<SourceOverviewDto> documents =
        sourceDao.getAsOverviewDtos(sessionFactory.getCurrentSession(),
            dfsModel.getObject().getProjectId(), null, first, count);
    return documents.iterator();
  }

  @Override
  public long size() {
    final Long numberOfSources = numberOfSourcesModel.getObject();
    return ObjectUtils.defaultIfNull(numberOfSources, 0L);
  }

  @Override
  public IModel<SourceOverviewDto> model(final SourceOverviewDto source) {
    return Model.of(source);
  }

  @Override
  public void detach() {
    dfsModel.detach();
    numberOfSourcesModel.detach();
  }

}
