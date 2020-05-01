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

import java.util.Collection;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.SourceService;
import com.romeikat.datamessie.core.base.ui.behavior.ModelUpdatingBehavior;
import com.romeikat.datamessie.core.base.ui.component.SourceTypeChoice;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.SourceOverviewDto;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;
import com.romeikat.datamessie.core.view.ui.dataprovider.SourcesOverviewDataProvider;
import com.romeikat.datamessie.core.view.ui.page.SourcePage;

public class SourcesOverviewPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private static final long SOURCES_PER_PAGE = 10;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private DataView<SourceOverviewDto> sourcesOverviewList;

  private IDataProvider<SourceOverviewDto> sourcesOverviewDataProvider;

  private PagingNavigator sourcesOverviewNavigator;

  @SpringBean
  private SourceService sourceService;

  @SpringBean(name = "sourceDao")
  private SourceDao sourceDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  @SpringBean
  private StringUtil stringUtil;

  public SourcesOverviewPanel(final String id, final IModel<DocumentsFilterSettings> dfsModel) {
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

    // Sources list
    sourcesOverviewDataProvider =
        new SourcesOverviewDataProvider(dfsModel, sourceDao, sessionFactory);
    sourcesOverviewList =
        new DataView<SourceOverviewDto>("sourcesOverviewList", sourcesOverviewDataProvider) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final Item<SourceOverviewDto> item) {
            final IModel<SourceOverviewDto> sourceModel = item.getModel();
            final SourceOverviewDto source = item.getModelObject();
            // Link to source
            final PageParameters sourcePageParameters =
                ((AbstractAuthenticatedPage) getPage()).createProjectPageParameters();
            sourcePageParameters.set("id", source.getId());
            final Label nameLabel =
                new Label("name", new PropertyModel<String>(sourceModel, "name"));
            final Link<SourcePage> sourceLink = new BookmarkablePageLink<SourcePage>("sourceLink",
                SourcePage.class, sourcePageParameters);
            sourceLink.add(nameLabel);
            item.add(sourceLink);
            // Language
            final Label languageLabel =
                new Label("language", new PropertyModel<String>(sourceModel, "language"));
            item.add(languageLabel);
            // Types
            final SourceTypeChoice typesChoice = new SourceTypeChoice("types",
                new PropertyModel<Collection<SourceTypeDto>>(sourceModel, "types")).setWidth(300);
            typesChoice.add(new ModelUpdatingBehavior() {
              private static final long serialVersionUID = 1L;

              @Override
              protected void onUpdate(final AjaxRequestTarget target) {
                final Collection<SourceTypeDto> newSelection = typesChoice.getModelObject();
                final HibernateSessionProvider sessionProvider =
                    new HibernateSessionProvider(sessionFactory);
                new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
                  @Override
                  protected void execute(final StatelessSession statelessSession) {
                    sourceService.setSourceTypes(statelessSession, source.getId(), newSelection);
                  }
                }.execute();
                sessionProvider.closeStatelessSession();
              }
            });
            item.add(typesChoice);
            // Crawling enabling
            final CheckBox crawlingEnabledCheckBox = new CheckBox("crawlingEnabled",
                new PropertyModel<Boolean>(sourceModel, "crawlingEnabled"));
            // Updating behavior to save enabling immediately on change
            crawlingEnabledCheckBox.add(new ModelUpdatingBehavior() {
              private static final long serialVersionUID = 1L;

              @Override
              protected void onUpdate(final AjaxRequestTarget target) {
                final Boolean newSelection = crawlingEnabledCheckBox.getModelObject();
                final HibernateSessionProvider sessionProvider =
                    new HibernateSessionProvider(sessionFactory);
                new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
                  @Override
                  protected void execute(final StatelessSession statelessSession) {
                    sourceService.setCrawlingEnabled(statelessSession, source.getId(),
                        newSelection);
                  }
                }.execute();
                sessionProvider.closeStatelessSession();
              }
            });
            item.add(crawlingEnabledCheckBox);
            // Visibility
            final CheckBox visibleCheckBox =
                new CheckBox("visible", new PropertyModel<Boolean>(sourceModel, "visible"));
            // Updating behavior to save visibility immediately on change
            visibleCheckBox.add(new ModelUpdatingBehavior() {
              private static final long serialVersionUID = 1L;

              @Override
              protected void onUpdate(final AjaxRequestTarget target) {
                final Boolean newSelection = visibleCheckBox.getModelObject();
                final HibernateSessionProvider sessionProvider =
                    new HibernateSessionProvider(sessionFactory);
                new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
                  @Override
                  protected void execute(final StatelessSession statelessSession) {
                    sourceService.setVisible(statelessSession, source.getId(), newSelection);
                  }
                }.execute();
                sessionProvider.closeStatelessSession();
              }
            });
            item.add(visibleCheckBox);
            // Statistics checking
            final CheckBox statisticsCheckingCheckBox = new CheckBox("statisticsChecking",
                new PropertyModel<Boolean>(sourceModel, "statisticsChecking"));
            // Updating behavior to save statistics checking immediately on change
            statisticsCheckingCheckBox.add(new ModelUpdatingBehavior() {
              private static final long serialVersionUID = 1L;

              @Override
              protected void onUpdate(final AjaxRequestTarget target) {
                final Boolean newSelection = statisticsCheckingCheckBox.getModelObject();
                final HibernateSessionProvider sessionProvider =
                    new HibernateSessionProvider(sessionFactory);
                new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
                  @Override
                  protected void execute(final StatelessSession statelessSession) {
                    sourceService.setStatisticsChecking(statelessSession, source.getId(),
                        newSelection);
                  }
                }.execute();
                sessionProvider.closeStatelessSession();
              }
            });
            item.add(statisticsCheckingCheckBox);
            // Number of rules
            final Label numberOfRulesLabel = new Label("numberOfRules",
                source.getNumberOfRedirectingRules() + "/" + source.getNumberOfDeletingRules() + "/"
                    + source.getNumberOfTagSelectingRules());
            item.add(numberOfRulesLabel);
            // Cookie
            final Label cookieLabel =
                new Label("cookie", new PropertyModel<String>(sourceModel, "cookie"));
            item.add(cookieLabel);
          }
        };
    sourcesOverviewList.setItemsPerPage(SOURCES_PER_PAGE);
    add(sourcesOverviewList);

    // Sources navigator
    sourcesOverviewNavigator =
        new AjaxPagingNavigator("sourcesOverviewNavigator", sourcesOverviewList) {
          private static final long serialVersionUID = 1L;

          @Override
          public void onConfigure() {
            super.onConfigure();
            final long pageCount = getPageable().getPageCount();
            setVisible(pageCount > 1);
          }
        };
    sourcesOverviewNavigator.setOutputMarkupId(true);
    add(sourcesOverviewNavigator);

    // Number of sources
    final IModel<String> numberOfSourcesLabelModel = new LoadableDetachableModel<String>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected String load() {
        final long numberOfSources = sourcesOverviewDataProvider.size();
        final String suffix = numberOfSources == 1 ? " source" : " sources";
        final String numberOfSourcesString = stringUtil.formatAsInteger(numberOfSources) + suffix;
        return numberOfSourcesString;
      }
    };
    final Label numberOfSourcesLabel = new Label("numberOfSourcesLabel", numberOfSourcesLabelModel);
    add(numberOfSourcesLabel);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    dfsModel.detach();
  }

}
