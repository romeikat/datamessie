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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.behavior.ModelUpdatingBehavior;
import com.romeikat.datamessie.core.base.ui.component.StatisticsIntervalSelector;
import com.romeikat.datamessie.core.base.ui.component.StatisticsPeriodSelector;
import com.romeikat.datamessie.core.base.ui.component.StatisticsTypeSelector;
import com.romeikat.datamessie.core.base.ui.panel.AbstractStatisticsPanel;
import com.romeikat.datamessie.core.base.ui.panel.AbstractTablePanel;
import com.romeikat.datamessie.core.base.ui.panel.BookmarkablePageLinkPanel;
import com.romeikat.datamessie.core.base.ui.panel.DocumentsFilterPanel;
import com.romeikat.datamessie.core.base.util.model.Percentage0Model;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.domain.enums.StatisticsInterval;
import com.romeikat.datamessie.core.domain.enums.StatisticsType;
import com.romeikat.datamessie.core.statistics.cache.GetFirstDayOfMonthFunction;
import com.romeikat.datamessie.core.statistics.cache.GetFirstDayOfWeekFunction;
import com.romeikat.datamessie.core.statistics.cache.GetFirstDayOfYearFunction;
import com.romeikat.datamessie.core.statistics.cache.GetNumberOfDocumentsFunction;
import com.romeikat.datamessie.core.statistics.cache.GetNumberOfDocumentsRateFunction;
import com.romeikat.datamessie.core.view.ui.page.DocumentsPage;

@AuthorizeInstantiation(DataMessieRoles.STATISTICS_PAGE)
public class StatisticsPage extends AbstractTablePage {

  private static final long serialVersionUID = 1L;

  private StatisticsTypeSelector statisticsTypeSelector;

  private StatisticsPeriodSelector statisticsPeriodSelector;

  private StatisticsIntervalSelector statisticsIntervalSelector;

  @SpringBean
  private SharedBeanProvider sharedBeanProvider;

  public StatisticsPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    getDocumentsFilterPanel().getCrawlingFilter().setVisible(false);
    getDocumentsFilterPanel().getFromDateFilter().setVisible(false);
    getDocumentsFilterPanel().getToLabel().setVisible(false);
    getDocumentsFilterPanel().getToDateFilter().setVisible(false);
    getDocumentsFilterPanel().getCleanedContentFilter().setVisible(false);
    getDocumentsFilterPanel().getStatesFilter().setVisible(false);
    getDocumentsFilterPanel().getDocumentsFilter().setVisible(false);
  }

  private void initialize() {
    statisticsTypeSelector = new StatisticsTypeSelector("statisticsTypeSelector");
    statisticsTypeSelector.add(new ModelUpdatingBehavior() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target) {
        final Component tableContainerPanel = getTableContainerPanel();
        target.add(tableContainerPanel);
      }
    });
    add(statisticsTypeSelector);

    statisticsPeriodSelector = new StatisticsPeriodSelector("statisticsPeriodSelector");
    statisticsPeriodSelector.add(new ModelUpdatingBehavior() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target) {
        final Component tableContainerPanel = getTableContainerPanel();
        target.add(tableContainerPanel);
      }
    });
    add(statisticsPeriodSelector);

    statisticsIntervalSelector = new StatisticsIntervalSelector("statisticsIntervalSelector");
    statisticsIntervalSelector.add(new ModelUpdatingBehavior() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target) {
        final Component tableContainerPanel = getTableContainerPanel();
        target.add(tableContainerPanel);
      }
    });
    add(statisticsIntervalSelector);
  }

  private LocalDate getFromDate() {
    final LocalDate toDate = getToDate();
    if (toDate == null) {
      return LocalDate.now();
    }

    Integer statisticsPeriod = DataMessieSession.get().getStatisticsPeriodModel().getObject();
    if (statisticsPeriod == null) {
      return LocalDate.now();
    }

    final StatisticsInterval statisticsInterval =
        DataMessieSession.get().getStatisticsIntervalModel().getObject();
    if (statisticsInterval == null) {
      return LocalDate.now();
    }

    // Minimum value is 1
    statisticsPeriod = Math.max(statisticsPeriod, 1);

    // Calculate
    final LocalDate fromDate = toDate.plusDays(1);
    switch (statisticsInterval) {
      case DAY:
        return fromDate.minusDays(statisticsPeriod);
      case WEEK:
        return fromDate.minusWeeks(statisticsPeriod);
      case MONTH:
        return fromDate.minusMonths(statisticsPeriod);
      case YEAR:
        return fromDate.minusYears(statisticsPeriod);
      default:
        return LocalDate.now();
    }
  }

  private LocalDate getToDate() {
    return LocalDate.now();
  }

  @Override
  protected AbstractTablePanel<?, ?, ?> createTablePanel(final String id) {

    final StatisticsType statisticsType = statisticsTypeSelector.getModelObject();
    switch (statisticsType) {
      case ALL_DOCUMENTS:
        return getStatisticsPanelForStates(id, DocumentProcessingState.getWithout(
            DocumentProcessingState.TECHNICAL_ERROR, DocumentProcessingState.TO_BE_DELETED));
      case DOWNLOADED_DOCUMENTS:
        return getStatisticsPanelForStates(id,
            DocumentProcessingState.getWithout(DocumentProcessingState.DOWNLOAD_ERROR,
                DocumentProcessingState.REDIRECTING_ERROR, DocumentProcessingState.TECHNICAL_ERROR,
                DocumentProcessingState.TO_BE_DELETED));
      case DOWNLOAD_SUCCESS_RATE:
        return getStatisticsPanelForStatesRate(id,
            DocumentProcessingState.getWithout(DocumentProcessingState.DOWNLOAD_ERROR,
                DocumentProcessingState.REDIRECTING_ERROR, DocumentProcessingState.TECHNICAL_ERROR,
                DocumentProcessingState.TO_BE_DELETED),
            DocumentProcessingState.getWithout(DocumentProcessingState.TECHNICAL_ERROR,
                DocumentProcessingState.TO_BE_DELETED));
      case PREPROCESSED_DOCUMENTS:
        return getStatisticsPanelForStates(id,
            DocumentProcessingState.getWith(DocumentProcessingState.STEMMED));
      case PREPROCESSING_SUCCESS_RATE:
        return getStatisticsPanelForStatesRate(id,
            DocumentProcessingState.getWith(DocumentProcessingState.STEMMED),
            DocumentProcessingState.getWith(DocumentProcessingState.CLEANED,
                DocumentProcessingState.CLEANING_ERROR, DocumentProcessingState.STEMMED));
      case TO_BE_PREPROCESSED:
        return getStatisticsPanelForStates(id,
            DocumentProcessingState.getWith(DocumentProcessingState.DOWNLOADED,
                DocumentProcessingState.REDIRECTED, DocumentProcessingState.CLEANED));
      case DOWNLOAD_ERRORS:
        return getStatisticsPanelForStates(id,
            DocumentProcessingState.getWith(DocumentProcessingState.DOWNLOAD_ERROR));
      case CLEANING_ERRORS:
        return getStatisticsPanelForStates(id,
            DocumentProcessingState.getWith(DocumentProcessingState.CLEANING_ERROR));
      default:
        return getEmptyStatisticsPanel(id);
    }
  }

  private AbstractTablePanel<Long, LocalDate, Long> getEmptyStatisticsPanel(final String id) {
    return new AbstractStatisticsPanel<Long>(id, this) {
      private static final long serialVersionUID = 1L;

      @Override
      protected SparseSingleTable<Long, LocalDate, Long> getStatistics(
          final Collection<Long> sourceIds) {
        return new SparseSingleTable<Long, LocalDate, Long>();
      }

      @Override
      protected IModel<String> getValueModel(final Long value) {
        final String formattedValue = value == null ? null : String.valueOf(value);
        return Model.of(formattedValue);
      }
    };
  }

  private AbstractTablePanel<Long, LocalDate, Long> getStatisticsPanelForStates(final String id,
      final DocumentProcessingState[] states) {
    return new AbstractStatisticsPanel<Long>(id, this) {
      private static final long serialVersionUID = 1L;

      @Override
      protected SparseSingleTable<Long, LocalDate, Long> getStatistics(
          final Collection<Long> sourceIds) {
        final Function<LocalDate, LocalDate> transformDateFunction = getTransformDateFunction();
        final GetNumberOfDocumentsFunction transformValueFunction =
            new GetNumberOfDocumentsFunction(states);
        final IStatisticsManager statisticsManager =
            sharedBeanProvider.getSharedBean(IStatisticsManager.class);
        if (statisticsManager == null) {
          return new SparseSingleTable<Long, LocalDate, Long>();
        }

        final LocalDate fromDate = getFromDate();
        final LocalDate toDate = getToDate();
        final SparseSingleTable<Long, LocalDate, Long> statistics = statisticsManager.getStatistics(
            sourceIds, fromDate, toDate, transformDateFunction, transformValueFunction);
        return statistics;
      }

      @Override
      protected Component getValueComponent(final String componentId, final Long sourceId,
          final LocalDate date, final IModel<String> valueModel) {
        final PageParameters pageParameters = new PageParameters();
        // Project
        pageParameters.set("project", ((AbstractAuthenticatedPage) getPage()).getActiveProjectId());
        // Source
        pageParameters.set("source", sourceId);
        // Dates
        final String fromDate = getFromDate(date);
        pageParameters.set("from", fromDate);
        final String toDate = getToDate(date);
        pageParameters.set("to", toDate);
        // States
        final List<Integer> statesOrdinals = new ArrayList<Integer>(states.length);
        for (final DocumentProcessingState state : states) {
          statesOrdinals.add(state.ordinal());
        }
        Collections.sort(statesOrdinals);
        pageParameters.set("states", StringUtils.join(statesOrdinals, ","));
        // Link
        final BookmarkablePageLinkPanel<DocumentsPage> documentsLink =
            new BookmarkablePageLinkPanel<DocumentsPage>(componentId, DocumentsPage.class,
                pageParameters, valueModel);
        return documentsLink;
      }

      @Override
      protected IModel<String> getValueModel(final Long value) {
        final String formattedValue = value == null ? null : String.valueOf(value);
        return Model.of(formattedValue);
      }
    };
  }

  private String getFromDate(final LocalDate date) {
    final LocalDate fromDate = date;
    return DocumentsFilterPanel.formatLocalDate(fromDate);
  }

  private String getToDate(final LocalDate date) {
    LocalDate toDate;
    final StatisticsInterval statisticsInterval = statisticsIntervalSelector.getModelObject();
    switch (statisticsInterval) {
      case DAY:
        toDate = date;
        break;
      case WEEK:
        toDate = date.plusWeeks(1).minusDays(1);
        break;
      case MONTH:
        toDate = date.plusMonths(1).minusDays(1);
        break;
      case YEAR:
        toDate = date.plusYears(1).minusDays(1);
        break;
      default:
        toDate = null;
    }
    return DocumentsFilterPanel.formatLocalDate(toDate);
  }

  private AbstractTablePanel<Long, LocalDate, Double> getStatisticsPanelForStatesRate(
      final String id, final DocumentProcessingState[] states1,
      final DocumentProcessingState[] states2) {
    return new AbstractStatisticsPanel<Double>(id, this) {
      private static final long serialVersionUID = 1L;

      @Override
      protected SparseSingleTable<Long, LocalDate, Double> getStatistics(
          final Collection<Long> sourceIds) {
        final Function<LocalDate, LocalDate> transformDateFunction = getTransformDateFunction();
        final GetNumberOfDocumentsRateFunction transformValueFunction =
            new GetNumberOfDocumentsRateFunction(states1, states2);
        final IStatisticsManager statisticsManager =
            sharedBeanProvider.getSharedBean(IStatisticsManager.class);
        if (statisticsManager == null) {
          return new SparseSingleTable<Long, LocalDate, Double>();
        }

        final LocalDate fromDate = getFromDate();
        final LocalDate toDate = getToDate();
        final SparseSingleTable<Long, LocalDate, Double> statistics =
            statisticsManager.getStatistics(sourceIds, fromDate, toDate, transformDateFunction,
                transformValueFunction);
        return statistics;
      }

      @Override
      protected IModel<String> getValueModel(final Double value) {
        return new Percentage0Model(value);
      }
    };
  }

  private Function<LocalDate, LocalDate> getTransformDateFunction() {
    final StatisticsInterval statisticsInterval = statisticsIntervalSelector.getModelObject();
    switch (statisticsInterval) {
      case DAY:
        return Functions.identity();
      case WEEK:
        return new GetFirstDayOfWeekFunction();
      case MONTH:
        return new GetFirstDayOfMonthFunction();
      case YEAR:
        return new GetFirstDayOfYearFunction();
      default:
        return new Function<LocalDate, LocalDate>() {
          @Override
          public LocalDate apply(final LocalDate from) {
            return null;
          }
        };
    }

  }

}
