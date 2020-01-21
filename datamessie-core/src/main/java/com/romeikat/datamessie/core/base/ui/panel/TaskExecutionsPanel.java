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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.modalx.ModalContentWindow;
import com.romeikat.datamessie.core.base.service.TaskExecutionService;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionDto;

public class TaskExecutionsPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private final TaskExecutionsModel taskExecutionsModel;

  private final ListView<TaskExecutionDto> taskExecutionsList;

  @SpringBean
  private TaskExecutionService taskExecutionService;

  public class TaskExecutionsModel extends LoadableDetachableModel<List<TaskExecutionDto>> {
    private static final long serialVersionUID = 1L;

    private Map<Long, TaskExecutionDto> taskExecutions;

    @Override
    protected List<TaskExecutionDto> load() {
      final List<TaskExecutionDto> taskExecutionsOrdered =
          taskExecutionService.getVisibleTaskExecutionsOrderedByLatestActivityDesc();
      // Store for later retrieval
      taskExecutions =
          taskExecutionsOrdered.stream().collect(Collectors.toMap(t -> t.getId(), t -> t));
      // Reverse so the latest one is the first one
      return taskExecutionsOrdered;
    }

    public TaskExecutionDto getTaskExecution(final long id) {
      return taskExecutions.get(id);
    }
  }

  public class CallbackModel extends LoadableDetachableModel<TaskExecutionDto> {
    private static final long serialVersionUID = 1L;

    private final long taskExecutionId;
    private final TaskExecutionsModel taskExecutionsModel;

    public CallbackModel(final long taskExecutionId,
        final TaskExecutionsModel taskExecutionsModel) {
      this.taskExecutionId = taskExecutionId;
      this.taskExecutionsModel = taskExecutionsModel;
    }

    @Override
    protected TaskExecutionDto load() {
      return taskExecutionsModel.getTaskExecution(taskExecutionId);
    }
  }

  public TaskExecutionsPanel(final String id) {
    super(id);

    // Model
    taskExecutionsModel = new TaskExecutionsModel();

    // Task executions
    taskExecutionsList = new ListView<TaskExecutionDto>("taskExecutionsList", taskExecutionsModel) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<TaskExecutionDto> item) {
        final TaskExecutionDto taskExecution = item.getModel().getObject();
        final IModel<TaskExecutionDto> callbackModel =
            new CallbackModel(taskExecution.getId(), taskExecutionsModel);
        // Link to works
        final AjaxLink<Void> modalContentWindowLink = new AjaxLink<Void>("modalContentWindowLink") {
          private static final long serialVersionUID = 1L;

          @Override
          public void onClick(final AjaxRequestTarget target) {
            final ModalContentWindow modalContentWindow =
                TaskExecutionsPanel.this.allocateModalWindow();
            if (modalContentWindow != null) {
              final TaskExecutionWorksPanel taskExecutionWorksPanel =
                  new TaskExecutionWorksPanel(modalContentWindow, callbackModel);
              taskExecutionWorksPanel.show(target);
            }
          }
        };
        String taskLabelContent = taskExecution.getName();
        if (taskExecution.getStatus() != null) {
          taskLabelContent += " (" + taskExecution.getStatus().getName().toLowerCase() + ")";
        }
        final Label taskLabel = new Label("taskLabel", taskLabelContent);
        modalContentWindowLink.add(taskLabel);
        item.add(modalContentWindowLink);
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
