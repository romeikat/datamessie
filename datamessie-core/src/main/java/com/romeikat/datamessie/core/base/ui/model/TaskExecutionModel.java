package com.romeikat.datamessie.core.base.ui.model;

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

import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.romeikat.datamessie.core.base.service.TaskExecutionService;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionDto;

public class TaskExecutionModel extends LoadableDetachableModel<TaskExecutionDto> {

  private static final long serialVersionUID = 1L;

  private final long taskExecutionId;

  @SpringBean
  private TaskExecutionService taskExecutionService;

  public TaskExecutionModel(final long taskExecutionId) {
    this.taskExecutionId = taskExecutionId;
    Injector.get().inject(this);
  }

  @Override
  protected TaskExecutionDto load() {
    return taskExecutionService.getTaskExecution(taskExecutionId);
  }

}
