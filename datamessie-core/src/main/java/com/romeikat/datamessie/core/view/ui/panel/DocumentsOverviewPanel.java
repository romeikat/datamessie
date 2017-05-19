package com.romeikat.datamessie.core.view.ui.panel;

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

import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.basic.Label;
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

import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.domain.dto.DocumentOverviewDto;
import com.romeikat.datamessie.core.view.ui.dataprovider.DocumentsOverviewDataProvider;
import com.romeikat.datamessie.core.view.ui.page.DocumentPage;
import com.romeikat.datamessie.core.view.ui.page.SourcePage;

public class DocumentsOverviewPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private static final long DOCUMENTS_PER_PAGE = 10;

  private final IModel<DocumentsFilterSettings> dfsModel;

  private DataView<DocumentOverviewDto> documentsOverviewList;

  private IDataProvider<DocumentOverviewDto> documentsOverviewDataProvider;

  private PagingNavigator documentsOverviewNavigator;

  @SpringBean(name = "documentDao")
  private DocumentDao documentDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  @SpringBean
  private StringUtil stringUtil;

  public DocumentsOverviewPanel(final String id, final IModel<DocumentsFilterSettings> dfsModel) {
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

    // Documents list
    documentsOverviewDataProvider = new DocumentsOverviewDataProvider(dfsModel, documentDao, sessionFactory);
    documentsOverviewList = new DataView<DocumentOverviewDto>("documentsOverviewList", documentsOverviewDataProvider) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final Item<DocumentOverviewDto> item) {
        final IModel<DocumentOverviewDto> documentModel = item.getModel();
        final DocumentOverviewDto document = item.getModelObject();
        // Published
        final Label publishedLabel =
            new Label("publishedLabel", new PropertyModel<LocalDateTime>(documentModel, "published"));
        item.add(publishedLabel);
        // Link to source
        final PageParameters sourcePageParameters =
            ((AbstractAuthenticatedPage) getPage()).createProjectPageParameters();
        sourcePageParameters.set("id", document.getSourceId());
        final Label sourceNameLabel =
            new Label("sourceNameLabel", new PropertyModel<String>(documentModel, "sourceName"));
        final Link<SourcePage> sourceLink =
            new BookmarkablePageLink<SourcePage>("sourceLink", SourcePage.class, sourcePageParameters);
        sourceLink.add(sourceNameLabel);
        item.add(sourceLink);
        // Link to document
        final PageParameters pageParameters = ((AbstractAuthenticatedPage) getPage()).createProjectPageParameters();
        pageParameters.set("id", document.getId());
        final Label documentTitleLabel =
            new Label("documentTitleLabel", new PropertyModel<String>(documentModel, "title"));
        final Link<DocumentPage> documentLink =
            new BookmarkablePageLink<DocumentPage>("documentLink", DocumentPage.class, pageParameters);
        documentLink.add(documentTitleLabel);
        item.add(documentLink);
      }
    };
    documentsOverviewList.setItemsPerPage(DOCUMENTS_PER_PAGE);
    add(documentsOverviewList);

    // Documents navigator
    documentsOverviewNavigator = new AjaxPagingNavigator("documentsOverviewNavigator", documentsOverviewList) {
      private static final long serialVersionUID = 1L;

      @Override
      public void onConfigure() {
        super.onConfigure();
        final long pageCount = getPageable().getPageCount();
        setVisible(pageCount > 1);
      }
    };
    documentsOverviewNavigator.setOutputMarkupId(true);
    add(documentsOverviewNavigator);

    // Number of documents
    final IModel<String> numberOfDocumentsLabelModel = new LoadableDetachableModel<String>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected String load() {
        final long numberOfDocuments = documentsOverviewDataProvider.size();
        final String suffix = numberOfDocuments == 1 ? " document" : " documents";
        final String numberOfDocumentsString = stringUtil.formatAsInteger(numberOfDocuments) + suffix;
        return numberOfDocumentsString;
      }
    };
    final Label numberOfDocumentsLabel = new Label("numberOfDocumentsLabel", numberOfDocumentsLabelModel);
    add(numberOfDocumentsLabel);
  }

}
