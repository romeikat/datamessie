package com.romeikat.datamessie.core.base.ui.panel;

import java.io.File;
import java.io.Serializable;

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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.wicketstuff.modalx.ModalContentPanel;
import org.wicketstuff.modalx.ModalContentWindow;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.ui.component.AjaxConfirmationLink;
import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.FileDownloadLink;
import com.romeikat.datamessie.core.base.util.converter.DateConverter;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionDto;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionWorkDto;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;
import jersey.repackaged.com.google.common.collect.Lists;

public class TaskExecutionWorksPanel extends ModalContentPanel {

  private static final long serialVersionUID = 1L;

  private static final boolean AUTO_UPDATE = true;
  private static final int AUTO_UPDATING_INTERVAL = 10;

  private static final int MAX_TASK_EXECUTION_WORKS = 5000;

  private AjaxLink<Void> updateLink;
  private final AjaxConfirmationLink<Void> cancelLink;

  private final IModel<TaskExecutionDto> taskExecutionModel;
  private final IModel<List<TaskExecutionWorkDto>> taskExecutionWorksModel;
  private final ListView<TaskExecutionWorkDto> taskExecutionsWorkList;

  @SpringBean
  private TaskManager taskManager;

  static class FileResultDownloadLink extends FileDownloadLink {

    private static final long serialVersionUID = 1L;

    public FileResultDownloadLink(final String id,
        final IModel<TaskExecutionDto> taskExecutionModel) {
      super(id, createFileResultModel(taskExecutionModel));
    }

    @Override
    protected void onConfigure() {
      super.onConfigure();

      setVisible(getModelObject() != null);
    }

    private static IModel<File> createFileResultModel(
        final IModel<TaskExecutionDto> taskExecutionModel) {
      return new LoadableDetachableModel<File>() {
        private static final long serialVersionUID = 1L;

        @Override
        public File load() {
          final TaskExecutionDto taskExecution = taskExecutionModel.getObject();
          final Serializable result = taskExecution.getResult();
          final boolean hasFileResult =
              result != null && File.class.isAssignableFrom(result.getClass());
          if (hasFileResult) {
            return (File) result;
          } else {
            return null;
          }
        }
      };
    }
  }

  public TaskExecutionWorksPanel(final ModalContentWindow modalContentWindow,
      final IModel<TaskExecutionDto> taskExecutionModel) {
    super(modalContentWindow, null);
    this.taskExecutionModel = taskExecutionModel;
    setTitle("Task Details");

    // Auto update
    if (AUTO_UPDATE) {
      final TaskExecutionDto taskExecution = taskExecutionModel.getObject();
      final boolean autoUpdateNecessary = taskExecution.getStatus() != TaskExecutionStatus.COMPLETED
          && taskExecution.getStatus() != TaskExecutionStatus.CANCELLED
          && taskExecution.getStatus() != TaskExecutionStatus.FAILED;
      if (autoUpdateNecessary) {
        add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(AUTO_UPDATING_INTERVAL)));
      }
    }

    // Update link
    updateLink = new AjaxLink<Void>("updateLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onConfigure() {
        super.onConfigure();
        final TaskExecutionDto taskExecution =
            TaskExecutionWorksPanel.this.taskExecutionModel.getObject();
        final boolean visible = taskExecution.getStatus() != TaskExecutionStatus.COMPLETED
            && taskExecution.getStatus() != TaskExecutionStatus.CANCELLED
            && taskExecution.getStatus() != TaskExecutionStatus.FAILED;
        setVisible(visible);
      }

      @Override
      public void onClick(final AjaxRequestTarget target) {
        target.add(TaskExecutionWorksPanel.this);
        target.add(((AbstractAuthenticatedPage) getPage()).getTaskExecutionsPanel());
      }
    };
    updateLink.setVisible(!AUTO_UPDATE);
    add(updateLink);

    // Download link
    final DownloadLink fileResultDownloadLink =
        new FileResultDownloadLink("fileResultDownloadLink", taskExecutionModel);
    add(fileResultDownloadLink);

    // Cancel link
    final String confirmation = "Would you really like to cancel the execution of this task?";
    cancelLink = new AjaxConfirmationLink<Void>("cancelLink", confirmation) {
      private static final long serialVersionUID = 1L;

      @Override
      public void onConfigure() {
        super.onConfigure();
        final TaskExecutionDto taskExecution =
            TaskExecutionWorksPanel.this.taskExecutionModel.getObject();
        final boolean visible = taskExecution.getStatus() != TaskExecutionStatus.COMPLETED
            && taskExecution.getStatus() != TaskExecutionStatus.CANCEL_REQUESTED
            && taskExecution.getStatus() != TaskExecutionStatus.CANCELLED
            && taskExecution.getStatus() != TaskExecutionStatus.FAILED;
        setVisible(visible);
      }

      @Override
      public void onClick(final AjaxRequestTarget target) {
        final TaskExecutionDto taskExecution =
            TaskExecutionWorksPanel.this.taskExecutionModel.getObject();
        taskManager.cancelTask(taskExecution.getId());
        target.add(TaskExecutionWorksPanel.this);
        target.add(((AbstractAuthenticatedPage) getPage()).getTaskExecutionsPanel());
        modalContentWindow.close(target);
      }
    };
    add(cancelLink);

    // Model
    taskExecutionWorksModel = new LoadableDetachableModel<List<TaskExecutionWorkDto>>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected List<TaskExecutionWorkDto> load() {
        final TaskExecutionDto taskExecution = taskExecutionModel.getObject();
        if (taskExecution == null) {
          return Collections.emptyList();
        }

        final List<TaskExecutionWorkDto> taskExecutionWorks =
            Lists.newArrayList(taskExecution.getWorks());
        Collections.reverse(taskExecutionWorks);
        final int taskExecutionWorksSize = taskExecutionWorks.size();
        if (taskExecutionWorksSize <= MAX_TASK_EXECUTION_WORKS) {
          return taskExecutionWorks;
        }
        return taskExecutionWorks.subList(0, MAX_TASK_EXECUTION_WORKS);
      }
    };

    // Task execution works
    taskExecutionsWorkList =
        new ListView<TaskExecutionWorkDto>("worksList", taskExecutionWorksModel) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final ListItem<TaskExecutionWorkDto> item) {
            final IModel<TaskExecutionWorkDto> taskExecutionWorkModel = item.getModel();
            final TaskExecutionWorkDto taskExecutionWork = taskExecutionWorkModel.getObject();
            final StringBuilder workLabelContent = new StringBuilder();
            final boolean empty = taskExecutionWork.getMessage() == null;
            if (!empty) {
              // Start
              if (taskExecutionWork.getStart() != null) {
                workLabelContent.append(DateConverter.INSTANCE_UI
                    .convertToString(new Date(taskExecutionWork.getStart())));
                workLabelContent.append(": ");
              }
              // Message
              if (taskExecutionWork.getMessage() != null) {
                workLabelContent.append(taskExecutionWork.getMessage());
              }
              // Duration
              if (taskExecutionWork.getDuration() != null) {
                final long seconds = Math.round(taskExecutionWork.getDuration() / 1000d);
                workLabelContent.append(" (");
                workLabelContent.append(seconds);
                workLabelContent.append(" s)");
              }
            }
            final Label workLabel = new Label("workLabel", workLabelContent.toString());
            item.add(workLabel);
          }
        };
    taskExecutionsWorkList.setOutputMarkupId(true);
    add(taskExecutionsWorkList);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    taskExecutionModel.detach();
    taskExecutionWorksModel.detach();
  }

}
