package com.romeikat.datamessie.core.base.ui.panel;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.datetime.StyleDateConverter;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.ui.component.CleanedContentFilter;
import com.romeikat.datamessie.core.base.ui.component.CrawlingIdFilter;
import com.romeikat.datamessie.core.base.ui.component.DocumentProcessingStateFilter;
import com.romeikat.datamessie.core.base.ui.component.DocumentsFilter;
import com.romeikat.datamessie.core.base.ui.component.LocalDateTextField;
import com.romeikat.datamessie.core.base.ui.component.SourceIdFilter;
import com.romeikat.datamessie.core.base.ui.component.SourceTypeIdFilter;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentsFilterPanel extends Panel {

  private static final String DATE_TIME_PATTERN = "yyyyMMdd";

  private static final long serialVersionUID = 1L;

  private SourceIdFilter sourceIdFilter;

  private SourceTypeIdFilter sourceTypeIdFilter;

  private CrawlingIdFilter crawlingIdFilter;

  private LocalDateTextField fromDateFilter;

  private Label toLabel;

  private LocalDateTextField toDateFilter;

  private TextArea<String> cleanedContentFilter;

  private DocumentProcessingStateFilter statesFilter;

  private TextArea<String> documentsFilter;

  @SpringBean(name = "sourceDao")
  private SourceDao sourceDao;

  @SpringBean(name = "crawlingDao")
  private CrawlingDao crawlingDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public DocumentsFilterPanel(final String id) {
    super(id);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    final IModel<DocumentsFilterSettings> dfsModel =
        DataMessieSession.get().getDocumentsFilterSettingsModel();
    // Form
    final Form<DocumentsFilterSettings> filterForm =
        new Form<DocumentsFilterSettings>("filterForm", dfsModel) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void onError() {
            super.onError();
          }

          @Override
          protected void onSubmit() {
            final PageParameters pageParameters = getPage().getPageParameters();
            // Source
            final Long selectedSourceId = getSelectedSourceId();
            if (selectedSourceId == null) {
              pageParameters.remove("source");
            } else {
              pageParameters.set("source", selectedSourceId);
            }
            // Source type
            final Collection<Long> selectedSourceTypeIds = getSelectedSourceTypeIds();
            if (selectedSourceTypeIds == null || selectedSourceTypeIds.isEmpty()) {
              pageParameters.remove("sourcetypes");
            } else {
              pageParameters.set("sourcetypes", StringUtils.join(selectedSourceTypeIds, ","));
            }
            // Crawling
            final Long selectedCrawlingId = getSelectedCrawlingId();
            if (selectedCrawlingId == null) {
              pageParameters.remove("crawling");
            } else {
              pageParameters.set("crawling", selectedCrawlingId);
            }
            // From date
            final LocalDate selectedFromDate = getSelectedFromDate();
            final String fromDate = formatLocalDate(selectedFromDate);
            if (fromDate == null) {
              pageParameters.remove("from");
            } else {
              pageParameters.set("from", fromDate);
            }
            // To date
            final LocalDate selectedToDate = getSelectedToDate();
            final String toDate = formatLocalDate(selectedToDate);
            if (toDate == null) {
              pageParameters.remove("to");
            } else {
              pageParameters.set("to", toDate);
            }
            // Cleaned content is not processed as it is not included in the URL
            // Document IDs are not processed as they are not included in the URL
            // States
            final Collection<DocumentProcessingState> selectedStates = getSelectedStates();
            if (selectedStates == null || selectedStates.isEmpty()) {
              pageParameters.remove("states");
            } else {
              final List<Integer> statesOrdinals = new ArrayList<Integer>(selectedStates.size());
              for (final DocumentProcessingState selectedState : selectedStates) {
                statesOrdinals.add(selectedState.ordinal());
              }
              Collections.sort(statesOrdinals);
              pageParameters.set("states", StringUtils.join(statesOrdinals, ","));
            }
          }
        };
    add(filterForm);

    // Submit link
    final SubmitLink submitLink = new SubmitLink("submit", filterForm);
    filterForm.add(submitLink);

    // Sources
    sourceIdFilter = new SourceIdFilter("sourceIdFilter", dfsModel);
    filterForm.add(sourceIdFilter);

    // Source types
    sourceTypeIdFilter = new SourceTypeIdFilter("sourceTypeIdFilter", dfsModel);
    filterForm.add(sourceTypeIdFilter);

    // Crawlings
    crawlingIdFilter = new CrawlingIdFilter("crawlingIdFilter", dfsModel);
    filterForm.add(crawlingIdFilter);

    // From date
    fromDateFilter = new LocalDateTextField("fromDateFilter",
        new PropertyModel<LocalDate>(dfsModel, "fromDate"), new StyleDateConverter("M-", false));
    filterForm.add(fromDateFilter);
    // From date picker
    final DatePicker fromDatePicker = new DatePicker();
    fromDatePicker.setShowOnFieldClick(true);
    fromDatePicker.setAutoHide(true);
    fromDateFilter.add(fromDatePicker);

    // To label
    toLabel = new Label("toLabel", Model.of("to"));
    filterForm.add(toLabel);

    // To date
    toDateFilter = new LocalDateTextField("toDateFilter",
        new PropertyModel<LocalDate>(dfsModel, "toDate"), new StyleDateConverter("M-", false));
    filterForm.add(toDateFilter);
    // To date picker
    final DatePicker toDatePicker = new DatePicker();
    toDatePicker.setShowOnFieldClick(true);
    toDatePicker.setAutoHide(true);
    toDateFilter.add(toDatePicker);

    // Cleaned content
    cleanedContentFilter = new CleanedContentFilter("cleanedContentFilter", dfsModel);
    filterForm.add(cleanedContentFilter);

    // Documents
    documentsFilter = new DocumentsFilter("documentsFilter", dfsModel);
    filterForm.add(documentsFilter);

    // State
    statesFilter = new DocumentProcessingStateFilter("statesFilter", dfsModel);
    filterForm.add(statesFilter);
  }

  public static String formatLocalDate(final LocalDate localDate) {
    // No date corresponds to 0
    if (localDate == null) {
      return "0";
    }
    // Today corresponds to no date
    final LocalDate today = LocalDate.now();
    if (localDate.equals(today)) {
      return null;
    }
    // Date pattern
    return localDate.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
  }

  private Long getSelectedSourceId() {
    final Long selectedSourceId = sourceIdFilter.getModelObject();
    return selectedSourceId;
  }

  private Collection<Long> getSelectedSourceTypeIds() {
    final Collection<Long> selectedSourceTypeIds = sourceTypeIdFilter.getModelObject();
    return selectedSourceTypeIds;
  }

  private Long getSelectedCrawlingId() {
    final Long selectedCrawlingId = crawlingIdFilter.getModelObject();
    return selectedCrawlingId;
  }

  private LocalDate getSelectedFromDate() {
    final Date selectedFromDate = fromDateFilter.getModelObject();
    if (selectedFromDate == null) {
      return null;
    }
    return DateUtil.toLocalDate(selectedFromDate);
  }

  private LocalDate getSelectedToDate() {
    final Date selectedToDate = toDateFilter.getModelObject();
    if (selectedToDate == null) {
      return null;
    }
    return DateUtil.toLocalDate(selectedToDate);
  }

  private Collection<DocumentProcessingState> getSelectedStates() {
    final Collection<DocumentProcessingState> selectedStates = statesFilter.getModelObject();
    return selectedStates;
  }

  public SourceIdFilter getSourceFilter() {
    return sourceIdFilter;
  }

  public SourceTypeIdFilter getSourceTypeFilter() {
    return sourceTypeIdFilter;
  }

  public CrawlingIdFilter getCrawlingFilter() {
    return crawlingIdFilter;
  }

  public LocalDateTextField getFromDateFilter() {
    return fromDateFilter;
  }

  public Label getToLabel() {
    return toLabel;
  }

  public LocalDateTextField getToDateFilter() {
    return toDateFilter;
  }

  public TextArea<String> getCleanedContentFilter() {
    return cleanedContentFilter;
  }

  public DocumentProcessingStateFilter getStatesFilter() {
    return statesFilter;
  }

  public TextArea<String> getDocumentsFilter() {
    return documentsFilter;
  }

}
