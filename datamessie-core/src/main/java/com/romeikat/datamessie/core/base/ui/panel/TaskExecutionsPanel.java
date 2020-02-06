package com.romeikat.datamessie.core.base.ui.panel;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.modalx.ModalContentWindow;
import com.romeikat.datamessie.core.base.service.TaskExecutionService;
import com.romeikat.datamessie.core.base.ui.model.TaskExecutionModel;
import com.romeikat.datamessie.core.base.ui.model.TaskExecutionsModel;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.converter.LocalDateTimeConverter;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionDto;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public class TaskExecutionsPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private final TaskExecutionsModel taskExecutionsModel;

  private final ListView<TaskExecutionDto> taskExecutionsList;

  @SpringBean
  private TaskExecutionService taskExecutionService;

  public TaskExecutionsPanel(final String id, final Collection<TaskExecutionStatus> status) {
    super(id);

    // Model
    taskExecutionsModel = new TaskExecutionsModel(status);

    // Task executions
    taskExecutionsList = new ListView<TaskExecutionDto>("taskExecutionsList", taskExecutionsModel) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<TaskExecutionDto> item) {
        final TaskExecutionDto taskExecution = item.getModel().getObject();
        final IModel<TaskExecutionDto> taskExecutionModel =
            new TaskExecutionModel(taskExecution.getId());
        // Link to works
        final AjaxLink<Void> modalContentWindowLink = new AjaxLink<Void>("modalContentWindowLink") {
          private static final long serialVersionUID = 1L;

          @Override
          public void onClick(final AjaxRequestTarget target) {
            final ModalContentWindow modalContentWindow =
                TaskExecutionsPanel.this.allocateModalWindow();
            if (modalContentWindow != null) {
              final TaskExecutionWorksPanel taskExecutionWorksPanel =
                  new TaskExecutionWorksPanel(modalContentWindow, taskExecutionModel);
              taskExecutionWorksPanel.show(target);
            }
          }
        };
        item.add(modalContentWindowLink);

        // Task name
        final String taskNameContent = taskExecution.getName();
        final Label taskName = new Label("taskName", taskNameContent);
        modalContentWindowLink.add(taskName);

        // Task details
        final StringBuilder taskDetailsContent = new StringBuilder();
        taskDetailsContent.append("(created ");
        taskDetailsContent
            .append(LocalDateTimeConverter.INSTANCE_UI.convertToString(taskExecution.getCreated()));
        if (taskExecution.getStatus() != null) {
          taskDetailsContent.append(", ");
          taskDetailsContent.append(taskExecution.getStatus().getName().toLowerCase());
        }
        taskDetailsContent.append(")");
        final Label taskDetails = new Label("taskDetails", taskDetailsContent);
        taskDetails.setVisible(false);
        modalContentWindowLink.add(taskDetails);
      }
    };
    taskExecutionsList.setOutputMarkupId(true);
    add(taskExecutionsList);
  }

  public ModalContentWindow allocateModalWindow() {
    final AbstractAuthenticatedPage page = (AbstractAuthenticatedPage) getPage();
    return page.allocateModalWindow();
  }

  @Override
  public void onDetach() {
    super.onDetach();

    taskExecutionsModel.detach();
  }

}
