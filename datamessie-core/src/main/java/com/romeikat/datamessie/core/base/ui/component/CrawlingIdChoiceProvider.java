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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;

import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.converter.LocalDateTimeConverter;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.CrawlingOverviewDto;

public class CrawlingIdChoiceProvider extends AbstractIdBasedChoiceProvider<CrawlingOverviewDto> {

  private static final long serialVersionUID = 1L;

  private final IModel<DocumentsFilterSettings> dfsModel;

  @SpringBean(name = "crawlingDao")
  private CrawlingDao crawlingDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public CrawlingIdChoiceProvider(final IModel<DocumentsFilterSettings> dfsModel) {
    super();
    Injector.get().inject(this);
    this.dfsModel = dfsModel;
  }

  @Override
  protected List<CrawlingOverviewDto> provideChoices() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final DocumentsFilterSettings documentsFilterSetting = dfsModel.getObject();
    final Long projectId = documentsFilterSetting == null ? null : documentsFilterSetting.getProjectId();
    final List<CrawlingOverviewDto> choices =
        crawlingDao.getAsOverviewDtos(sessionProvider.getStatelessSession(), projectId);
    sessionProvider.closeStatelessSession();
    return choices;
  }

  @Override
  protected long provideId(final CrawlingOverviewDto choice) {
    return choice.getId();
  }

  @Override
  protected String provideDisplayText(final CrawlingOverviewDto choice) {
    final LocalDateTime started = choice.getStarted();
    if (started == null) {
      return null;
    }
    return LocalDateTimeConverter.INSTANCE_UI.convertToString(started);
  }

  @Override
  protected Comparator<CrawlingOverviewDto> getComparator() {
    // Sorting by started (descending)
    final Comparator<CrawlingOverviewDto> displayTextComparator = new Comparator<CrawlingOverviewDto>() {
      @Override
      public int compare(final CrawlingOverviewDto choice1, final CrawlingOverviewDto choice2) {
        final LocalDateTime started1 = choice1.getStarted();
        final LocalDateTime started2 = choice2.getStarted();
        if (started1 == null && started2 == null) {
          return 0;
        }
        if (started1 == null) {
          return -1;
        }
        if (started2 == null) {
          return 1;
        }
        return started2.compareTo(started1);
      }
    };
    return displayTextComparator;
  }

  @Override
  public void detach() {
    super.detach();

    dfsModel.detach();
  }

}
