package com.romeikat.datamessie.core.base.task.scheduling;

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
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskManager;

public class FooIntervalTaskSchedulingThread extends IntervalTaskSchedulingThread {

  private final Task task;

  private final String taskName;

  private final Integer taskExecutionInterval;

  private final boolean allowOverlap;

  private final TaskManager taskManager;

  public FooIntervalTaskSchedulingThread(final Task task, final String taskName,
      final Integer taskExecutionInterval, final boolean allowOverlap,
      final TaskManager taskManager) {
    this.task = task;
    this.taskName = taskName;
    this.taskExecutionInterval = taskExecutionInterval;
    this.allowOverlap = allowOverlap;
    this.taskManager = taskManager;
  }

  @Override
  protected Task getTask() {
    return task;
  }

  @Override
  protected String getTaskName() {
    return taskName;
  }

  @Override
  protected Integer getTaskExecutionInterval() {
    return taskExecutionInterval;
  }

  @Override
  protected boolean allowOverlap() {
    return allowOverlap;
  }

  @Override
  protected LocalDateTime getActualStartOfLatestCompletedTask() {
    return null;
  }

  @Override
  protected boolean shouldSkipTaskExecution() {
    return false;
  };

  @Override
  protected boolean shouldStopTaskExecution() {
    return false;
  }

  @Override
  protected TaskManager getTaskManager() {
    return taskManager;
  }

}
