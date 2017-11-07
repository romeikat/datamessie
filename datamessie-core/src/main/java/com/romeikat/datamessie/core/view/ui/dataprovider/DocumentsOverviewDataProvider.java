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
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.dto.DocumentOverviewDto;

public class DocumentsOverviewDataProvider implements IDataProvider<DocumentOverviewDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private final DocumentDao documentDao;

  private final SessionFactory sessionFactory;

  private final IModel<Long> numberOfDocumentsModel;

  public DocumentsOverviewDataProvider(final IModel<DocumentsFilterSettings> dfsModel,
      final DocumentDao documentDao, final SessionFactory sessionFactory) {
    this.dfsModel = dfsModel;
    this.documentDao = documentDao;
    this.sessionFactory = sessionFactory;

    // Loads number of documents only once per request
    numberOfDocumentsModel = new LoadableDetachableModel<Long>() {
      private static final long serialVersionUID = 1L;

      @Override
      public Long load() {
        final Long numberOfDocuments =
            documentDao.count(sessionFactory.getCurrentSession(), dfsModel.getObject());
        return numberOfDocuments;
      }
    };
  }

  @Override
  public Iterator<? extends DocumentOverviewDto> iterator(final long first, final long count) {
    final List<DocumentOverviewDto> documents = documentDao
        .getAsOverviewDtos(sessionFactory.getCurrentSession(), dfsModel.getObject(), first, count);
    return documents.iterator();
  }

  @Override
  public long size() {
    final Long numberOfDocuments = numberOfDocumentsModel.getObject();
    return ObjectUtils.defaultIfNull(numberOfDocuments, 0L);
  }

  @Override
  public IModel<DocumentOverviewDto> model(final DocumentOverviewDto document) {
    return Model.of(document);
  }

  @Override
  public void detach() {
    dfsModel.detach();
    numberOfDocumentsModel.detach();
  }

}
