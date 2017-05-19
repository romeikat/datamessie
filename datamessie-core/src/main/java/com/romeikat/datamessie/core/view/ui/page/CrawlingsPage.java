package com.romeikat.datamessie.core.view.ui.page;

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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;

import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.domain.dto.CrawlingDto;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;

@AuthorizeInstantiation(DataMessieRoles.CRAWLINGS_PAGE)
public class CrawlingsPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private IModel<List<CrawlingDto>> crawlingsModel;

  private ListView<CrawlingDto> crawlingsList;

  @SpringBean(name = "crawlingDao")
  private CrawlingDao crawlingDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public CrawlingsPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    crawlingsModel = new LoadableDetachableModel<List<CrawlingDto>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public List<CrawlingDto> load() {
        final ProjectDto activeProject = getActiveProject();
        if (activeProject == null) {
          return Collections.emptyList();
        }
        return crawlingDao.getAsDtos(sessionFactory.getCurrentSession(), activeProject.getId());
      }
    };

    crawlingsList = new ListView<CrawlingDto>("crawlingsList", crawlingsModel) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<CrawlingDto> item) {
        final IModel<CrawlingDto> crawlingModel = item.getModel();
        final CrawlingDto crawling = item.getModelObject();
        final boolean crawlingInProgress = crawling.getCompleted() == null;
        // Started
        final Label startedLabel =
            new Label("startedLabel", new PropertyModel<LocalDateTime>(crawlingModel, "started"));
        item.add(startedLabel);
        // Duration
        final Label durationLabel = new Label("durationLabel", new PropertyModel<Duration>(crawlingModel, "duration"));
        durationLabel.setVisible(!crawlingInProgress);
        item.add(durationLabel);
        // Ongoing
        final String ongoingText = "ongoing";
        final Label ongoingLabel = new Label("ongoingLabel", ongoingText);
        ongoingLabel.setVisible(crawlingInProgress);
        item.add(ongoingLabel);
      }

      @Override
      public void onConfigure() {
        super.onConfigure();
        setVisible(getActiveProject() != null);
      }
    };
    crawlingsList.setOutputMarkupId(true);
    add(crawlingsList);
  }

}
