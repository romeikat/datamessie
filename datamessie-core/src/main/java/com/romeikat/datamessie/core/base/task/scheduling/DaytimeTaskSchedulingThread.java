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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.util.DateUtil;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public abstract class DaytimeTaskSchedulingThread extends Thread {

  private static final Logger LOG = LoggerFactory.getLogger(DaytimeTaskSchedulingThread.class);

  private static final DateTimeFormatter DATE_TIME_FPRMATTER = DateTimeFormatter.ofPattern("dd.MM.yyy HH:mm:ss");

  private TaskExecution latestTaskExecution = null;

  protected abstract Task getTask();

  protected abstract String getTaskName();

  protected abstract LocalTime getTaskExecutionDaytime();

  protected boolean allowOverlap() {
    return true;
  }

  protected boolean shouldStopTaskExecution() {
    return false;
  }

  protected boolean shouldSkipTaskExecution() {
    return false;
  };

  protected abstract TaskManager getTaskManager();

  @Override
  public void run() {
    // Repeatedly start crawling according to crawling interval
    while (true) {

      // Determine task execution daytime
      final LocalTime taskExecutionDaytime = getTaskExecutionDaytime();
      if (taskExecutionDaytime == null) {
        LOG.warn("No more scheduling task \"{}\" as task execution daytime is not available", getTaskName());
        break;
      }

      // Wait until execution daytime
      waitUntilDaytime(taskExecutionDaytime);

      // Handle possible overlap with latest task execution
      if (!allowOverlap()) {

        // If latest task execution has not yet finished, wait a full interval
        final boolean hasLatestTaskExecutionFinished = hasLatestTaskExecutionFinished();
        if (!hasLatestTaskExecutionFinished) {
          LOG.info("Skippig task execution as latest task execution for \"{}\" has not yet finished", getTaskName());
          waitMillis(1000);
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
        LOG.info("Skippig task execution for task \"{}\" as desired", getTaskName());
        waitMillis(1000);
        continue;
      }

      // Execute task
      final Task task = getTask();
      if (task == null) {
        LOG.warn("Not executing task \"{}\" as task is not available", getTaskName());
      } else {
        LOG.info("Executing task \"{}\"", getTaskName());
        latestTaskExecution = getTaskManager().addTask(task);
      }

      // Next task execution
      waitMillis(1000);
    }

    LOG.debug("Task execution for task \"{}\" finished", getTaskName());
  }

  private void waitUntilDaytime(final LocalTime taskExecutionDaytime) {
    final LocalDateTime nextStart = getNextStart(taskExecutionDaytime);
    final long delay = DateUtil.getDelayUntil(nextStart);
    if (delay == 0) {
      LOG.info("Scheduling task \"{}\" to be executed immediately", getTaskName());
    } else {
      LOG.info("Scheduling task \"{}\" to be executed at {}", getTaskName(), DATE_TIME_FPRMATTER.format(nextStart));
      waitMillis(delay);
    }
  }

  private LocalDateTime getNextStart(final LocalTime taskExecutionDaytime) {
    final LocalDate today = LocalDate.now();
    final LocalDateTime nextStart;
    if (LocalTime.now().isBefore(taskExecutionDaytime)) {
      // Next task execution is today
      nextStart = LocalDateTime.of(today, taskExecutionDaytime);
    } else {
      // Next task execution is tomorrow
      nextStart = LocalDateTime.of(today, taskExecutionDaytime).plusDays(1);
    }
    return nextStart;
  }

  private boolean hasLatestTaskExecutionFinished() {
    final boolean isLatestTaskExecutionFinished =
        latestTaskExecution == null || latestTaskExecution.getStatus() == TaskExecutionStatus.COMPLETED
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
