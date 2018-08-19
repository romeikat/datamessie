package com.romeikat.datamessie.core.view.ui.panel;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2018 Dr. Raphael Romeikat
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
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.domain.dto.CrawlingOverviewDto;
import com.romeikat.datamessie.core.rss.service.CrawlingService;
import com.romeikat.datamessie.core.view.ui.dataprovider.CrawlingsOverviewDataProvider;

public class CrawlingsOverviewPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private static final long CRAWLINGS_PER_PAGE = 10;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private DataView<CrawlingOverviewDto> crawlingsOverviewList;

  private IDataProvider<CrawlingOverviewDto> crawlingsOverviewDataProvider;

  private PagingNavigator crawlingsOverviewNavigator;

  @SpringBean
  private CrawlingService crawlingService;

  @SpringBean(name = "crawlingDao")
  private CrawlingDao crawlingDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  @SpringBean
  private StringUtil stringUtil;

  public CrawlingsOverviewPanel(final String id, final IModel<DocumentsFilterSettings> dfsModel) {
    super(id);
    this.dfsModel = dfsModel;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    setOutputMarkupId(true);

    // Crawlings list
    crawlingsOverviewDataProvider =
        new CrawlingsOverviewDataProvider(dfsModel, crawlingDao, sessionFactory);
    crawlingsOverviewList =
        new DataView<CrawlingOverviewDto>("crawlingsOverviewList", crawlingsOverviewDataProvider) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final Item<CrawlingOverviewDto> item) {
            final IModel<CrawlingOverviewDto> crawlingModel = item.getModel();
            final CrawlingOverviewDto crawling = item.getModelObject();
            final boolean crawlingInProgress = crawling.getCompleted() == null;
            // Started
            final Label startedLabel = new Label("startedLabel",
                new PropertyModel<LocalDateTime>(crawlingModel, "started"));
            item.add(startedLabel);
            // Duration
            final Label durationLabel =
                new Label("durationLabel", new PropertyModel<Duration>(crawlingModel, "duration"));
            durationLabel.setVisible(!crawlingInProgress);
            item.add(durationLabel);
            // Ongoing
            final String ongoingText = "ongoing";
            final Label ongoingLabel = new Label("ongoingLabel", ongoingText);
            ongoingLabel.setVisible(crawlingInProgress);
            item.add(ongoingLabel);
          }
        };
    crawlingsOverviewList.setItemsPerPage(CRAWLINGS_PER_PAGE);
    add(crawlingsOverviewList);

    // Crawlings navigator
    crawlingsOverviewNavigator =
        new AjaxPagingNavigator("crawlingsOverviewNavigator", crawlingsOverviewList) {
          private static final long serialVersionUID = 1L;

          @Override
          public void onConfigure() {
            super.onConfigure();
            final long pageCount = getPageable().getPageCount();
            setVisible(pageCount > 1);
          }
        };
    crawlingsOverviewNavigator.setOutputMarkupId(true);
    add(crawlingsOverviewNavigator);

    // Number of crawlings
    final IModel<String> numberOfCrawlingsLabelModel = new LoadableDetachableModel<String>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected String load() {
        final long numberOfCrawlings = crawlingsOverviewDataProvider.size();
        final String suffix = numberOfCrawlings == 1 ? " crawling" : " crawlings";
        final String numberOfCrawlingsString =
            stringUtil.formatAsInteger(numberOfCrawlings) + suffix;
        return numberOfCrawlingsString;
      }
    };
    final Label numberOfCrawlingsLabel =
        new Label("numberOfCrawlingsLabel", numberOfCrawlingsLabelModel);
    add(numberOfCrawlingsLabel);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    dfsModel.detach();
  }

}
