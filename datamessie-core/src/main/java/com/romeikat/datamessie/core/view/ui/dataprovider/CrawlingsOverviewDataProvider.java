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
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.dto.CrawlingOverviewDto;

public class CrawlingsOverviewDataProvider implements IDataProvider<CrawlingOverviewDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private final CrawlingDao crawlingDao;

  private final SessionFactory sessionFactory;

  private final IModel<Long> numberOfCrawlingsModel;

  public CrawlingsOverviewDataProvider(final IModel<DocumentsFilterSettings> dfsModel,
      final CrawlingDao crawlingDao, final SessionFactory sessionFactory) {
    this.dfsModel = dfsModel;
    this.crawlingDao = crawlingDao;
    this.sessionFactory = sessionFactory;

    // Loads number of crawlings only once per request
    numberOfCrawlingsModel = new LoadableDetachableModel<Long>() {
      private static final long serialVersionUID = 1L;

      @Override
      public Long load() {
        final Long numberOfCrawlings = crawlingDao.count(sessionFactory.getCurrentSession(),
            dfsModel.getObject().getProjectId());
        return numberOfCrawlings;
      }
    };
  }

  @Override
  public Iterator<? extends CrawlingOverviewDto> iterator(final long first, final long count) {
    final List<CrawlingOverviewDto> crawlings = crawlingDao.getAsOverviewDtos(
        sessionFactory.getCurrentSession(), dfsModel.getObject().getProjectId(), first, count);
    return crawlings.iterator();
  }

  @Override
  public long size() {
    final Long numberOfCrawlings = numberOfCrawlingsModel.getObject();
    return ObjectUtils.defaultIfNull(numberOfCrawlings, 0L);
  }

  @Override
  public IModel<CrawlingOverviewDto> model(final CrawlingOverviewDto crawling) {
    return Model.of(crawling);
  }

  @Override
  public void detach() {
    dfsModel.detach();
    numberOfCrawlingsModel.detach();
  }

}
