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
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public abstract class IntervalTaskSchedulingThread extends Thread {

  private static final Logger LOG = LoggerFactory.getLogger(IntervalTaskSchedulingThread.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyy HH:mm:ss");

  private TaskExecution latestTaskExecution = null;

  protected abstract Task getTask();

  protected abstract String getTaskName();

  protected abstract Integer getTaskExecutionInterval();

  protected boolean allowOverlap() {
    return true;
  }

  protected abstract LocalDateTime getActualStartOfLatestCompletedTask();

  /**
   * Specifies whether execution of the task should be stopped completely. This check is performed
   * just before the task would be executed.
   *
   * @return
   */
  protected boolean shouldStopTaskExecution() {
    return false;
  }

  /**
   * Specifies whether an execution of the task should be skipped. This check is performed just
   * before the task would be executed.
   *
   * @return
   */
  protected boolean shouldSkipTaskExecution() {
    return false;
  };

  protected void onAfterTriggeringTask(final TaskExecution latestTaskExecution) {}

  protected abstract TaskManager getTaskManager();

  @Override
  public void run() {
    // Repeatedly start task according to task execution interval
    while (true) {

      // Determine task execution interval
      final Integer taskExecutionInterval = getTaskExecutionInterval();
      if (taskExecutionInterval == null) {
        LOG.error("No more scheduling task \"{}\" as task execution interval is not available",
            getTaskName());
        break;
      }

      // Handle possible overlap with latest task execution
      if (!allowOverlap()) {

        // Wait until next actual start (next actual start is one full interval after actual
        // start of latest task execution)
        waitUntilNextActualStart(taskExecutionInterval);

        // If latest task execution has not yet finished, wait a full interval
        final boolean hasLatestTaskExecutionFinished = hasLatestTaskExecutionFinished();
        if (!hasLatestTaskExecutionFinished) {
          LOG.info(
              "Skippig task execution as latest task execution for \"{}\" has not yet finished",
              getTaskName());
          waitMillis(taskExecutionInterval);
          continue;
        }

      }

      // Check whether further executions should be performed
      final boolean shouldStopTaskExecution = shouldStopTaskExecution();
      if (shouldStopTaskExecution) {
        LOG.warn("Stopping task execution for task \"{}\" as desired", getTaskName());
        break;
      }

      // Check whether this execution should be skipped
      final boolean shouldSkipTaskExecution = shouldSkipTaskExecution();
      if (shouldSkipTaskExecution) {
        LOG.debug("Skippig task execution for task \"{}\" as desired", getTaskName());
        waitMillis(taskExecutionInterval);
        continue;
      }

      // Execute task
      final Task task = getTask();
      if (task == null) {
        LOG.error("Not executing task \"{}\" as task is not available", getTaskName());
      } else {
        LOG.debug("Executing task \"{}\"", getTaskName());
        latestTaskExecution = getTaskManager().addTask(task);
        IntervalTaskSchedulingThread.this.onAfterTriggeringTask(latestTaskExecution);
      }

      // Wait a full interval
      waitMillis(taskExecutionInterval);
    }

    LOG.debug("Task execution for task \"{}\" finished", getTaskName());
  }

  private void waitUntilNextActualStart(final Integer taskExecutionInterval) {
    final LocalDateTime nextActualStart = getNextActualStart(taskExecutionInterval);
    final long delay = DateUtil.getDelayUntil(nextActualStart);
    if (delay == 0) {
      LOG.debug("Scheduling task \"{}\" to be executed immediately", getTaskName());
    } else {
      LOG.debug("Scheduling task \"{}\" to be executed at {}", getTaskName(),
          DATE_TIME_FORMATTER.format(nextActualStart));
      waitMillis(delay);
    }
  }

  private LocalDateTime getNextActualStart(final int taskExecutionInterval) {
    final LocalDateTime actualStartOfLatestCompletedTask = getActualStartOfLatestCompletedTask();
    if (actualStartOfLatestCompletedTask == null) {
      return LocalDateTime.now();
    }

    final LocalDateTime nextStart =
        actualStartOfLatestCompletedTask.plusNanos(taskExecutionInterval * 1000);
    return nextStart;
  }

  private boolean hasLatestTaskExecutionFinished() {
    final boolean isLatestTaskExecutionFinished = latestTaskExecution == null
        || latestTaskExecution.getStatus() == TaskExecutionStatus.COMPLETED
        || latestTaskExecution.getStatus() == TaskExecutionStatus.CANCELLED
        || latestTaskExecution.getStatus() == TaskExecutionStatus.FAILED;
    return isLatestTaskExecutionFinished;
  }

  private static void waitMillis(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
    }
  }

}
