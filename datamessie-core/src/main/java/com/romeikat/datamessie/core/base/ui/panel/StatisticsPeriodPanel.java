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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.romeikat.datamessie.core.base.app.shared.IStatisticsManager;
import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.model.LongModel;
import com.romeikat.datamessie.core.base.util.model.Percentage0Model;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.dto.StatisticsDto;

public class StatisticsPeriodPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private IModel<StatisticsDto> statisticsModel;

  @SpringBean
  private SharedBeanProvider sharedBeanProvider;

  public StatisticsPeriodPanel(final String id, final Integer numberOfDays) {
    super(id);

    // Model
    statisticsModel = new LoadableDetachableModel<StatisticsDto>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected StatisticsDto load() {
        final ProjectDto activeProject = ((AbstractAuthenticatedPage) getPage()).getActiveProject();
        if (activeProject == null) {
          return StatisticsDto.emptyStatistics();
        }
        final IStatisticsManager statisticsManager =
            sharedBeanProvider.getSharedBean(IStatisticsManager.class);
        if (statisticsManager == null) {
          return StatisticsDto.emptyStatistics();
        }
        return statisticsManager.getStatistics(activeProject.getId(), numberOfDays);
      }
    };

    // All documents
    final Label allDocumentsLabel = new Label("allDocuments",
        new LongModel(new PropertyModel<Long>(statisticsModel, "allDocuments")));
    add(allDocumentsLabel);
    // Downloaded documents
    final Label downloadedDocumentsLabel = new Label("downloadedDocuments",
        new LongModel(new PropertyModel<Long>(statisticsModel, "downloadedDocuments")));
    add(downloadedDocumentsLabel);
    // Preprocessed documents
    final Label preprocessedDocumentsLabel = new Label("preprocessedDocuments",
        new LongModel(new PropertyModel<Long>(statisticsModel, "preprocessedDocuments")));
    add(preprocessedDocumentsLabel);
    // Download success
    final Label downloadSuccessLabel = new Label("downloadSuccess",
        new Percentage0Model(new PropertyModel<Double>(statisticsModel, "downloadSuccess")));
    add(downloadSuccessLabel);
    // Preprocessing success
    final Label preprocessingSuccessLabel = new Label("preprocessingSuccess",
        new Percentage0Model(new PropertyModel<Double>(statisticsModel, "preprocessingSuccess")));
    add(preprocessingSuccessLabel);
    // Documents to be preprocessed
    final Label documentsToBePreprocessedLabel = new Label("documentsToBePreprocessed",
        new LongModel(new PropertyModel<Long>(statisticsModel, "documentsToBePreprocessed")));
    add(documentsToBePreprocessedLabel);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    statisticsModel.detach();
  }

}
