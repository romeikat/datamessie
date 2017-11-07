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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.service.SourceService;
import com.romeikat.datamessie.core.base.ui.behavior.ModelUpdatingBehavior;
import com.romeikat.datamessie.core.base.ui.component.SourceTypeChoice;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.dto.SourceDto;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;

@AuthorizeInstantiation(DataMessieRoles.SOURCES_PAGE)
public class SourcesPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private IModel<List<SourceDto>> sourcesModel;

  private ListView<SourceDto> sourcesList;

  private Link<Void> addSourceLink;

  @SpringBean
  private SourceService sourceService;

  @SpringBean(name = "sourceDao")
  private SourceDao sourceDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourcesPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    sourcesModel = new LoadableDetachableModel<List<SourceDto>>() {
      private static final long serialVersionUID = 1L;

      @Override
      public List<SourceDto> load() {
        final ProjectDto activeProject = getActiveProject();
        if (activeProject == null) {
          return Collections.emptyList();
        }
        final HibernateSessionProvider sessionProvider =
            new HibernateSessionProvider(sessionFactory);
        final List<SourceDto> dtos =
            sourceDao.getAsDtos(sessionProvider.getStatelessSession(), activeProject.getId(), null);
        sessionProvider.closeStatelessSession();
        return dtos;
      }
    };

    sourcesList = new ListView<SourceDto>("sourcesList", sourcesModel) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<SourceDto> item) {
        final IModel<SourceDto> sourceModel = item.getModel();
        final SourceDto source = item.getModelObject();
        // Link to source
        final PageParameters sourcePageParameters = createProjectPageParameters();
        sourcePageParameters.set("id", source.getId());
        final Label nameLabel = new Label("name", new PropertyModel<String>(sourceModel, "name"));
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
        // Number of rules
        final Label numberOfRulesLabel = new Label("numberOfRules",
            source.getNumberOfRedirectingRules() + "/" + source.getNumberOfTagSelectingRules());
        item.add(numberOfRulesLabel);
      }

      @Override
      public void onConfigure() {
        super.onConfigure();
        setVisible(getActiveProject() != null);
      }
    };
    sourcesList.setOutputMarkupId(true);
    add(sourcesList);

    // Link to add a new source, if no sources have been added yet
    addSourceLink = new Link<Void>("addSource") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick() {
        // TODO source creation
      }

      @Override
      public void onConfigure() {
        setVisible(false);
        // final List<SourceDto> sources = sourcesModel.getObject();
        // setVisible(sources.isEmpty());
      }
    };
    addSourceLink.setOutputMarkupId(true);
    add(addSourceLink);
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    sourcesModel.detach();
  }

}
