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

import java.time.LocalDateTime;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.domain.dto.DocumentDto;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@AuthorizeInstantiation(DataMessieRoles.DOCUMENT_PAGE)
public class DocumentPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private IModel<DocumentDto> documentModel;

  private ExternalLink urlLink;

  @SpringBean(name = "documentDao")
  private DocumentDao documentDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public DocumentPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    // Document
    documentModel = new LoadableDetachableModel<DocumentDto>() {
      private static final long serialVersionUID = 1L;

      @Override
      public DocumentDto load() {
        final StringValue idParameter = getRequest().getRequestParameters().getParameterValue("id");
        return idParameter.isNull() ? null
            : documentDao.getAsDto(sessionFactory.getCurrentSession(), idParameter.toLong());
      }
    };
    final DocumentDto document = documentModel.getObject();

    // Table
    final WebMarkupContainer documentWmc = new WebMarkupContainer("document") {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onConfigure() {
        super.onConfigure();
        setVisible(documentModel.getObject() != null);
      }
    };
    add(documentWmc);

    // ID
    final Label idLabel = new Label("idLabel", new PropertyModel<Long>(documentModel, "id"));
    documentWmc.add(idLabel);
    // Title
    final Label titleLabel =
        new Label("titleLabel", new PropertyModel<String>(documentModel, "title"));
    documentWmc.add(titleLabel);
    // Stemmed title
    final Label stemmedTitleLabel =
        new Label("stemmedTitleLabel", new PropertyModel<String>(documentModel, "stemmedTitle"));
    documentWmc.add(stemmedTitleLabel);
    // URLs
    urlLink = new ExternalLink("urlLink", new PropertyModel<String>(documentModel, "url"),
        new PropertyModel<String>(documentModel, "url"));
    urlLink.setContextRelative(false);
    documentWmc.add(urlLink);
    // Description
    final Label descriptionLabel =
        new Label("descriptionLabel", new PropertyModel<String>(documentModel, "description"));
    documentWmc.add(descriptionLabel);
    // Stemmed description
    final Label stemmedDescriptionLabel = new Label("stemmedDescriptionLabel",
        new PropertyModel<String>(documentModel, "stemmedDescription"));
    documentWmc.add(stemmedDescriptionLabel);
    // Published
    final Label publishedLabel =
        new Label("publishedLabel", new PropertyModel<LocalDateTime>(documentModel, "published"));
    documentWmc.add(publishedLabel);
    // Downloaded
    final Label downloadedLabel =
        new Label("downloadedLabel", new PropertyModel<LocalDateTime>(documentModel, "downloaded"));
    documentWmc.add(downloadedLabel);
    // Link to source
    final PageParameters sourcePageParameters = createProjectPageParameters();
    sourcePageParameters.set("id", document.getSourceId());
    final Label sourceNameLabel =
        new Label("sourceNameLabel", new PropertyModel<String>(documentModel, "sourceName"));
    final Link<SourcePage> sourceLink =
        new BookmarkablePageLink<SourcePage>("sourceLink", SourcePage.class, sourcePageParameters);
    sourceLink.add(sourceNameLabel);
    documentWmc.add(sourceLink);
    // Status code
    final Label statusCodeLabel =
        new Label("statusCodeLabel", new PropertyModel<Integer>(documentModel, "statusCode"));
    documentWmc.add(statusCodeLabel);
    // State
    final Label stateLabel =
        new Label("stateLabel", new PropertyModel<DocumentProcessingState>(documentModel, "state"));
    documentWmc.add(stateLabel);
    // Raw content
    final TextArea<String> rawContentTextArea = new TextArea<String>("rawContentTextArea",
        new PropertyModel<String>(documentModel, "rawContent"));
    documentWmc.add(rawContentTextArea);
    // Cleaned content
    final TextArea<String> cleanedContentTextArea = new TextArea<String>("cleanedContentTextArea",
        new PropertyModel<String>(documentModel, "cleanedContent"));
    documentWmc.add(cleanedContentTextArea);
    // Stemmed content
    final TextArea<String> stemmedContentTextArea = new TextArea<String>("stemmedContentTextArea",
        new PropertyModel<String>(documentModel, "stemmedContent"));
    documentWmc.add(stemmedContentTextArea);
    // Named entities
    final TextArea<String> namedEntitiesTextArea = new TextArea<String>("namedEntitiesTextArea",
        new PropertyModel<String>(documentModel, "namedEntities"));
    documentWmc.add(namedEntitiesTextArea);
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    final DocumentDto document = documentModel.getObject();
    if (document != null) {
      urlLink.setVisible(document.getUrl() != null);
    }
  }

  @Override
  protected Class<? extends Page> getNavigationLinkClass() {
    return DocumentsPage.class;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    documentModel.detach();
  }

}
