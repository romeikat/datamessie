package com.romeikat.datamessie.core.base.ui.page;

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
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.ui.panel.DocumentsFilterPanel;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public abstract class AbstractDocumentsFilterPage extends AbstractAuthenticatedPage {

  private static final long serialVersionUID = 1L;

  private DocumentsFilterPanel documentsFilterPanel;

  public AbstractDocumentsFilterPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  private void initialize() {
    // Documents filter
    documentsFilterPanel = new DocumentsFilterPanel("documentsFilterPanel");
    add(documentsFilterPanel);

    // Apply request parameters
    applyRequestParametersToDocumentsFilterSettings();
  }

  public DocumentsFilterPanel getDocumentsFilterPanel() {
    return documentsFilterPanel;
  }

  private StringValue getParameterValue(final IRequestParameters requestParameters,
      final String parameterName) {
    final Set<String> requestParameterNames = requestParameters.getParameterNames();
    if (!requestParameterNames.contains(parameterName)) {
      return null;
    }
    final StringValue parameterValue = requestParameters.getParameterValue(parameterName);
    if (parameterValue.isNull()) {
      return null;
    }
    return parameterValue;
  }

  private void applyRequestParametersToDocumentsFilterSettings() {
    final DocumentsFilterSettings dfs = DataMessieSession.get().getDocumentsFilterSettings();
    final IRequestParameters requestParameters = getRequest().getRequestParameters();
    // Project
    final Long projectId = getActiveProjectId();
    dfs.setProjectId(projectId);
    // Source
    final StringValue sourceParameter = getParameterValue(requestParameters, "source");
    final Long sourceId = sourceParameter == null ? null : sourceParameter.toLong();
    dfs.setSourceId(sourceId);
    // Source visible (fixed)
    final Boolean sourceVisible = true;
    dfs.setSourceVisible(sourceVisible);
    // Source type
    final StringValue sourceTypesParameter = getParameterValue(requestParameters, "sourcetypes");
    final Collection<Long> sourceTypeIds =
        sourceTypesParameter == null ? null : parseSourceTypeIds(sourceTypesParameter);
    dfs.setSourceTypeIds(sourceTypeIds);
    // Crawling
    final StringValue crawlingParameter = getParameterValue(requestParameters, "crawling");
    final Long crawlingId = crawlingParameter == null ? null : crawlingParameter.toLong();
    dfs.setCrawlingId(crawlingId);
    // From date
    final StringValue fromDateParameter = getParameterValue(requestParameters, "from");
    final LocalDate fromDate = parseLocalDate(fromDateParameter);
    dfs.setFromDate(fromDate);
    // To date
    final StringValue toDateParameter = getParameterValue(requestParameters, "to");
    final LocalDate toDate = parseLocalDate(toDateParameter);
    dfs.setToDate(toDate);
    // Cleaned content is not processed as it is not included in the URL
    // Document IDs are not processed as they are not included in the URL
    // States
    final StringValue statesParameter = getParameterValue(requestParameters, "states");
    final Collection<DocumentProcessingState> states =
        statesParameter == null ? null : parseStates(statesParameter);
    dfs.setStates(states);
  }

  private Collection<Long> parseSourceTypeIds(final StringValue stringValue) {
    List<Long> sourceTypeIds = null;
    try {
      sourceTypeIds = new ArrayList<Long>();
      final String[] sourceTypeIdStrings = StringUtils.split(stringValue.toString(), ",");
      for (final String sourceTypeIdString : sourceTypeIdStrings) {
        final long sourceTypeId = Long.parseLong(sourceTypeIdString);
        sourceTypeIds.add(sourceTypeId);
      }
    } catch (final Exception e) {
      sourceTypeIds = null;
    }
    return sourceTypeIds;
  }

  private Collection<DocumentProcessingState> parseStates(final StringValue stringValue) {
    List<DocumentProcessingState> states = null;
    try {
      states = new ArrayList<DocumentProcessingState>();
      final DocumentProcessingState[] allStates = DocumentProcessingState.values();
      final String[] stateOrdinalStrings = StringUtils.split(stringValue.toString(), ",");
      for (final String stateOrdinalString : stateOrdinalStrings) {
        final int stateOrdinal = Integer.parseInt(stateOrdinalString);
        final DocumentProcessingState state = allStates[stateOrdinal];
        states.add(state);
      }
    } catch (final Exception e) {
      states = null;
    }
    return states;
  }

  private LocalDate parseLocalDate(final StringValue stringValue) {
    final LocalDate today = LocalDate.now();
    // No date corresponds to today
    if (stringValue == null) {
      return today;
    }
    // 0 corresponds to no date
    if (stringValue.toString().equals("0")) {
      return null;
    }
    // Negative numbers correspond to number of days from today in the past
    try {
      final int numberOfDays = Integer.parseInt(stringValue.toString());
      if (numberOfDays < 0) {
        final LocalDate localDate = today.plusDays(numberOfDays);
        return localDate;
      }
    } catch (final NumberFormatException e) {
    }
    // Date pattern
    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    try {
      final LocalDate localDate = LocalDate.parse(stringValue.toString(), dateFormatter);
      return localDate;
    } catch (final IllegalArgumentException ex) {
    }
    // Ohterwise, use today
    return today;
  }

}
