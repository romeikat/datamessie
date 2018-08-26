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

import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.service.SourceService;
import com.romeikat.datamessie.core.base.ui.component.SourceTypeChoice;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.RedirectingRuleDto;
import com.romeikat.datamessie.core.domain.dto.SourceDto;
import com.romeikat.datamessie.core.domain.dto.TagSelectingRuleDto;
import com.romeikat.datamessie.core.view.ui.panel.RedirectingRulesPanel;
import com.romeikat.datamessie.core.view.ui.panel.TagSelectingRulesPanel;

@AuthorizeInstantiation(DataMessieRoles.SOURCE_PAGE)
public class SourcePage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private IModel<SourceDto> sourceModel;

  @SpringBean
  private SourceService sourceService;

  @SpringBean(name = "sourceDao")
  private SourceDao sourceDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourcePage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    // Source
    final StringValue idParameter = getRequest().getRequestParameters().getParameterValue("id");
    final Long userId = DataMessieSession.get().getUserId();
    final SourceDto source = idParameter.isNull() ? null
        : sourceDao.getAsDto(sessionProvider.getStatelessSession(), userId, idParameter.toLong());
    // Model cannot be a LoadableDetachableModel as the contained DTO will be edited across
    // multiple Ajax requests (by RedirectingRulesPanel and TagSelectingRulesPanel)
    sourceModel = new Model<SourceDto>(source) {
      private static final long serialVersionUID = 1L;

      @Override
      public SourceDto getObject() {
        final SourceDto oldSource = super.getObject();
        // If a valid new source is requested, return that one
        final StringValue idParameter = getRequest().getRequestParameters().getParameterValue("id");
        if (!idParameter.isNull() && oldSource != null) {
          if (oldSource.getId() != idParameter.toLong()) {
            final HibernateSessionProvider sessionProvider =
                new HibernateSessionProvider(sessionFactory);
            final SourceDto newSource = sourceDao.getAsDto(sessionProvider.getStatelessSession(),
                userId, idParameter.toLong());
            sessionProvider.closeStatelessSession();
            if (newSource != null) {
              return newSource;
            }
          }
        }
        // Return old source
        return oldSource;
      }
    };

    // Form
    final Form<SourceDto> sourceForm =
        new Form<SourceDto>("source", new CompoundPropertyModel<SourceDto>(sourceModel)) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void onSubmit() {
            final HibernateSessionProvider sessionProvider =
                new HibernateSessionProvider(sessionFactory);
            new ExecuteWithTransaction(sessionProvider.getStatelessSession()) {
              @Override
              protected void execute(final StatelessSession statelessSession) {
                sourceService.updateSource(statelessSession, getModelObject());
              }
            }.execute();
            sessionProvider.closeStatelessSession();
          }

          @Override
          protected void onConfigure() {
            super.onConfigure();
            setVisible(getModelObject() != null);
          }
        };
    add(sourceForm);

    // ID
    final Label idLabel = new Label("id");
    sourceForm.add(idLabel);
    // Name
    final TextField<String> nameTextField = new TextField<String>("name");
    sourceForm.add(nameTextField);
    // Language
    final Label languageLabel = new Label("language");
    sourceForm.add(languageLabel);
    // Types
    final SourceTypeChoice typesChoice = new SourceTypeChoice("types").setWidth(320);
    sourceForm.add(typesChoice);
    // URL
    final TextField<String> urlTextField = new TextField<String>("url");
    sourceForm.add(urlTextField);
    // Link to URL
    final ExternalLink urlLink =
        new ExternalLink("urlLink", new PropertyModel<String>(sourceModel, "url"));
    sourceForm.add(urlLink);
    // URL extracting rules
    final IModel<List<RedirectingRuleDto>> redirectingRulesModel =
        new PropertyModel<List<RedirectingRuleDto>>(sourceModel, "redirectingRules");
    final RedirectingRulesPanel redirectingRulesPanel =
        new RedirectingRulesPanel("redirectingRules", redirectingRulesModel);
    sourceForm.add(redirectingRulesPanel);
    // Tag selecting rules
    final IModel<List<TagSelectingRuleDto>> tagSelectingRulesModel =
        new PropertyModel<List<TagSelectingRuleDto>>(sourceModel, "tagSelectingRules");
    final TagSelectingRulesPanel tagSelectingRulesPanel =
        new TagSelectingRulesPanel("tagSelectingRules", tagSelectingRulesModel);
    sourceForm.add(tagSelectingRulesPanel);
    // Visible
    final CheckBox visibleCheckBox = new CheckBox("visible");
    sourceForm.add(visibleCheckBox);

    sessionProvider.closeStatelessSession();
  }

  @Override
  protected Class<? extends Page> getNavigationLinkClass() {
    return SourcesPage.class;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    sourceModel.detach();
  }

}
