package com.romeikat.datamessie.core.base.task.management;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.util.FileUtil;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public class TaskExecution {

  private static final Logger LOG = LoggerFactory.getLogger(TaskExecution.class);

  private static long nextId = 1;

  private final long id;

  private final String name;

  private final boolean visibleAfterCompleted;

  private final Integer priority;

  private TaskExecutionStatus status;

  private final List<TaskExecutionWork> works;

  private Task task;

  private File logFile;

  private final FileUtil fileUtil;

  public TaskExecution(final Task task, final FileUtil fileUtil) {
    id = getNextId();
    name = task.getName();
    visibleAfterCompleted = task.isVisibleAfterCompleted();
    priority = task.getPriority();
    status = TaskExecutionStatus.EXECUTION_REQUESTED;
    works = new ArrayList<TaskExecutionWork>();
    this.task = task;
    logFile = null;
    this.fileUtil = fileUtil;
  }

  public void start() {
    synchronized (this) {
      // Execution requested
      if (status == TaskExecutionStatus.EXECUTION_REQUESTED) {
        // Start
        status = TaskExecutionStatus.EXECUTING;
        LOG.debug("Task {} executing", name);
      }
      // Otherwise
      else {
        // Starting is not necessary
        return;
      }
    }
    // Asynchronous execution
    new Thread() {
      @Override
      public void run() {
        if (task != null) {
          try {
            task.execute(TaskExecution.this);
            status = TaskExecutionStatus.COMPLETED;
            LOG.debug("Task {} completed", name);
          } catch (final TaskCancelledException e) {
            status = TaskExecutionStatus.CANCELLED;
            LOG.debug("Task {} was cancelled", name);
          } catch (final Exception e) {
            status = TaskExecutionStatus.FAILED;
            LOG.error("Task " + name + " failed", e);
          }
        }
      }
    }.start();
  }

  public void requestPause() {
    synchronized (this) {
      // Execution requested or task is idle
      if (status == TaskExecutionStatus.EXECUTION_REQUESTED) {
        // Mark as pausing
        status = TaskExecutionStatus.PAUSING;
        return;
      }
      // Executing
      else if (status == TaskExecutionStatus.EXECUTING || status == TaskExecutionStatus.IDLE) {
        // Announce request to task
        status = TaskExecutionStatus.PAUSE_REQUESTED;
        LOG.debug("Task {} requested to pause", name);
      }
      // Otherwise
      else {
        // Pausing is not necessary
        return;
      }
    }
    // Wait until pause is granted by task, but at most five minutes
    int secondsWaited = 0;
    while (status == TaskExecutionStatus.PAUSE_REQUESTED) {
      waitMillis(1000);
      secondsWaited += 1;
      if (secondsWaited >= 5 * 60) {
        break;
      }
    }
  }

  public void allowResume() {
    synchronized (this) {
      // Pause requested or pausing
      if (status == TaskExecutionStatus.PAUSE_REQUESTED || status == TaskExecutionStatus.PAUSING) {
        // Allow task to execute
        status = TaskExecutionStatus.EXECUTING;
        LOG.debug("Task {} executing", name);
      }
    }
  }

  public void requestCancel() {
    synchronized (this) {
      // No more executing
      if (status == TaskExecutionStatus.COMPLETED || status == TaskExecutionStatus.CANCELLED
          || status == TaskExecutionStatus.FAILED) {
        // Cancelling is not necessary
        return;
      }
      // Otherwise
      else {
        // Announce request to cancel
        status = TaskExecutionStatus.CANCEL_REQUESTED;
        LOG.debug("Task {} requested to cancel", name);
      }
    }
  }

  public void checkpoint(final long idleTime) throws TaskCancelledException {
    // Checkpoint
    checkpoint();
    // Remain at idle state for the requested duration
    status = TaskExecutionStatus.IDLE;
    waitMillis(idleTime);
    // Going back to execution, if still idle (a pause/cancel could have been requested in the
    // meantime)
    synchronized (this) {
      if (status == TaskExecutionStatus.IDLE) {
        status = TaskExecutionStatus.EXECUTING;
      }
    }
    // Checkpoint
    checkpoint();
  }

  public void checkpoint() throws TaskCancelledException {
    cancelIfRequested();
    pauseIfRequested();
  }

  private void pauseIfRequested() {
    synchronized (this) {
      // Pause requested
      if (status == TaskExecutionStatus.PAUSE_REQUESTED) {
        // Grant pause
        status = TaskExecutionStatus.PAUSING;
        LOG.debug("Task {} pausing", name);
      }
      // Otherwise
      else {
        // Granting pause is not necessary
        return;
      }
    }
    // Wait until allowed to resume
    while (status == TaskExecutionStatus.PAUSING) {
      waitMillis(1000);
    }
    // Now, allowed to resume by allowResume(), so status was already set to EXECUTING
  }

  private void cancelIfRequested() throws TaskCancelledException {
    // Cancel requested
    if (status == TaskExecutionStatus.CANCEL_REQUESTED) {
      // Cancel
      throw new TaskCancelledException();
    }
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public boolean isVisible() {
    return visibleAfterCompleted || status != TaskExecutionStatus.COMPLETED;
  }

  public Integer getPriority() {
    return priority;
  }

  public TaskExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(final TaskExecutionStatus taskExecutionStatus) {
    synchronized (this) {
      status = taskExecutionStatus;
      if (status == TaskExecutionStatus.COMPLETED || status == TaskExecutionStatus.CANCELLED
          || status == TaskExecutionStatus.FAILED) {
        task = null;
      }
    }
  }

  public List<TaskExecutionWork> getWorks() {
    return new ArrayList<TaskExecutionWork>(works);
  }

  public Long getLatestActivity() {
    synchronized (works) {
      for (int i = works.size() - 1; i >= 0; i--) {
        final TaskExecutionWork work = works.get(i);
        final Long latestWorkActivity = work.getLatestActivity();
        if (latestWorkActivity != null) {
          return latestWorkActivity;
        }
      }
    }
    return null;
  }

  public Task getTask() {
    return task;
  }

  public void setLogFile(final File logFile) {
    this.logFile = logFile;
  }

  public void reportWork(final String message) {
    final TaskExecutionWork work = new TaskExecutionWork(message);
    work.setStart(System.currentTimeMillis());
    synchronized (works) {
      // Add work
      works.add(work);
      LOG.debug("Task {} - {}", task.getName(), work.getMessage());
    }
    // Logfile
    if (logFile != null) {
      fileUtil.appendToFile(logFile, work.toString());
    }
  }

  public void reportEmptyWork() {
    final TaskExecutionWork work = new TaskExecutionWork(null);
    work.setStart(System.currentTimeMillis());
    synchronized (works) {
      // Add work
      works.add(work);
      LOG.debug("Task {}", task.getName());
    }
    // Logfile
    if (logFile != null) {
      fileUtil.appendToFile(logFile, "");
    }
  }

  public TaskExecutionWork reportWorkStart(final String message) {
    final TaskExecutionWork work = new TaskExecutionWork(message);
    work.setStart(System.currentTimeMillis());
    synchronized (works) {
      // Add work
      works.add(work);
      LOG.debug("Work {} - {} started", task.getName(), work.getMessage());
    }
    // Logfile
    if (logFile != null) {
      fileUtil.appendToFile(logFile, work.toString());
    }
    return work;
  }

  public void reportWorkEnd(final TaskExecutionWork work) {
    if (work == null) {
      LOG.error("End of work reported for null reference");
      return;
    }
    // Update work
    work.setEnd(System.currentTimeMillis());
    LOG.debug("Work {} - {} ended (duration {} ms)", task.getName(), work.getMessage(), work.getDuration());
    // No output to logfile as output that started later might have been appended already
  }

  private void waitMillis(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
    }
  }

  private synchronized static long getNextId() {
    return nextId++;
  }

}
