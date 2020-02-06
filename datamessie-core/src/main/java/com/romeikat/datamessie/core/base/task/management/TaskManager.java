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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.util.FileUtil;
import com.romeikat.datamessie.core.base.util.naming.NameGenerator;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

@Service
public class TaskManager {

  private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);

  public static final long DEFAULT_MANAGEMENT_INTERVAL = 5000;

  private long managementInterval = DEFAULT_MANAGEMENT_INTERVAL;

  private final List<TaskExecution> taskExecutions;

  @Autowired
  private NameGenerator nameGenerator;

  @Autowired
  private FileUtil fileUtil;

  public TaskManager() {
    setManagementInterval(DEFAULT_MANAGEMENT_INTERVAL);
    taskExecutions = new ArrayList<TaskExecution>();
    // Continuously manage task executions
    new Thread() {
      @Override
      public void run() {
        while (true) {
          waitMillis(getManagementInterval());
          manageTasksExecutions();
        }
      }
    }.start();
  }

  public long getManagementInterval() {
    return managementInterval;
  }

  public void setManagementInterval(final long managementInterval) {
    this.managementInterval = managementInterval;
  }

  protected void reset() {
    taskExecutions.clear();
    setManagementInterval(DEFAULT_MANAGEMENT_INTERVAL);
  }

  public TaskExecution addTask(final Task task) {
    // Request task execution
    final String taskExecutionName = createTaskExecutionName(task);
    final TaskExecution taskExecution = new TaskExecution(task, taskExecutionName, fileUtil);
    synchronized (taskExecutions) {
      taskExecutions.add(taskExecution);
      LOG.debug("Task {} requested", taskExecution.getName());
    }
    // Done
    return taskExecution;
  }

  public void addTask(final Task task, final long delay) {
    waitMillis(delay);
    addTask(task);
  }

  private String createTaskExecutionName(final Task task) {
    final String taskName = task.getName();
    try {
      final String generatedName = nameGenerator.generateName();
      return StringUtils.isBlank(generatedName) ? taskName
          : taskName + " \"" + generatedName + "\"";
    } catch (final Exception e) {
      LOG.error("Could not generate name for task execution", e);
      return taskName;
    }
  }

  public TaskExecution cancelTask(final long taskExecutionId) {
    // Cancel task execution
    final TaskExecution taskExecution = getTaskExecution(taskExecutionId);
    if (taskExecution != null) {
      taskExecution.requestCancel();
    }
    // Done
    return taskExecution;
  }

  public void cancelTask(final Task task) {
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final Task existingTask = taskExecution.getTask();
        if (existingTask == task) {
          taskExecution.requestCancel();
        }
      }
    }
  }

  private void manageTasksExecutions() {
    // Background tasks
    manageBackgroundTasks();

    // Foreground tasks
    manageForegroundTasks();
  }

  private void manageBackgroundTasks() {
    final List<TaskExecution> requestedBackgroundTasks = getRequestedBackgroundTasks();
    for (final TaskExecution requestedBackgroundTask : requestedBackgroundTasks) {
      requestedBackgroundTask.start();
    }
  }

  private void manageForegroundTasks() {
    final int hightestPrio = 1;
    final int lowestPrio = getLowestPrio();
    for (int prio = hightestPrio; prio <= lowestPrio; prio++) {
      synchronized (taskExecutions) {
        // Requested tasks
        final TaskExecution firstRequestedTask = getFirstRequestedTask(prio);
        if (firstRequestedTask != null) {
          // Task requested, but a more important task already active
          if (isAnyMoreImportantTaskActive(prio)) {
            // Done
            return;
          }
          // Otherwise
          else {
            // Pause less important tasks
            pauseLessImportantActiveTasks(prio);
            // Start requested task
            firstRequestedTask.start();
            // Done
            return;
          }
        }

        // Pausing tasks
        final TaskExecution firstPausingTask = getFirstPausingTask(prio);
        if (firstPausingTask != null) {
          // Task pausing, but a more important task still active
          if (isAnyMoreImportantTaskActive(prio)) {
            // Done
            return;
          }
          // Otherwise
          else {
            // Resume pausing task
            firstPausingTask.allowResume();
            // Done
            return;
          }
        }
      }
    }
  }

  public <T extends Task> Set<T> getActiveTasks(final Class<T> clazz) {
    final Set<T> result = Sets.newHashSet();
    final List<TaskExecution> taskExecutions = getTaskExecutions(clazz,
        TaskExecutionStatus.EXECUTION_REQUESTED, TaskExecutionStatus.EXECUTING,
        TaskExecutionStatus.PAUSE_REQUESTED, TaskExecutionStatus.PAUSING, TaskExecutionStatus.IDLE);
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final Task task = taskExecution.getTask();
        if (clazz.isAssignableFrom(task.getClass())) {
          @SuppressWarnings("unchecked")
          final T typedTask = (T) task;
          result.add(typedTask);
        }
      }
    }
    return result;
  }

  public List<TaskExecution> getVisibleTaskExecutionsOrderedByLatestActivityDesc() {
    final List<TaskExecution> result = new ArrayList<TaskExecution>();
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if (taskExecution.isVisible()) {
          result.add(taskExecution);
        }
      }
    }
    // Sort from latest activity to earliest activity
    final Comparator<TaskExecution> taskExecutionComparator = new Comparator<TaskExecution>() {

      @Override
      public int compare(final TaskExecution taskExecution1, final TaskExecution taskExecution2) {
        // Prio 1: latest activity
        Long taskExecution1LatestActivity = taskExecution1.getLatestActivity();
        if (taskExecution1LatestActivity == null) {
          taskExecution1LatestActivity = Long.MIN_VALUE;
        }
        Long taskExecution2LatestActivity = taskExecution2.getLatestActivity();
        if (taskExecution2LatestActivity == null) {
          taskExecution2LatestActivity = Long.MIN_VALUE;
        }
        final int result = taskExecution2LatestActivity.compareTo(taskExecution1LatestActivity);
        if (result != 0) {
          return result;
        }
        // Prio 2: priority
        final int priority1NotNull =
            taskExecution1.getPriority() == null ? Integer.MAX_VALUE : taskExecution1.getPriority();
        final int priority2NotNull =
            taskExecution2.getPriority() == null ? Integer.MAX_VALUE : taskExecution2.getPriority();
        return priority1NotNull - priority2NotNull;
      }

    };
    Collections.sort(result, taskExecutionComparator);
    // Done
    return result;
  }

  public TaskExecution getTaskExecution(final long taskExecutionId) {
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final boolean idMatches = taskExecution.getId() == taskExecutionId;
        if (idMatches) {
          return taskExecution;
        }
      }
    }
    return null;
  }

  public List<TaskExecution> getTaskExecutions(final Class<? extends Task> clazz,
      final TaskExecutionStatus... taskExecutionStatus) {
    final List<TaskExecution> result = new LinkedList<TaskExecution>();
    final List<TaskExecutionStatus> taskExecutionStatusAsList = Arrays.asList(taskExecutionStatus);
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final boolean statusMatches = taskExecutionStatus == null
            || taskExecutionStatusAsList.contains(taskExecution.getStatus());
        if (!statusMatches) {
          continue;
        }

        final boolean classMatches =
            clazz == null || clazz.isAssignableFrom(taskExecution.getTask().getClass());
        if (!classMatches) {
          continue;
        }

        result.add(taskExecution);
      }
    }
    return result;
  }

  public boolean hasTaskExecutions(final Class<? extends Task> clazz,
      final TaskExecutionStatus taskExecutionStatus) {
    final List<TaskExecutionStatus> taskExecutionStatusAsList = Arrays.asList(taskExecutionStatus);
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final boolean statusMatches = taskExecutionStatus == null
            || taskExecutionStatusAsList.contains(taskExecution.getStatus());
        final boolean classMatches =
            clazz == null || clazz.isAssignableFrom(taskExecution.getTask().getClass());
        if (statusMatches && classMatches) {
          return true;
        }
      }
    }
    return false;
  }

  private int getLowestPrio() {
    int lowestPrio = 0;
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        final Integer prio = taskExecution.getPriority();
        if (prio != null && prio > lowestPrio) {
          lowestPrio = prio;
        }
      }
    }
    return lowestPrio;
  }

  private List<TaskExecution> getRequestedBackgroundTasks() {
    final List<TaskExecution> requestedBackgroundTasks = Lists.newLinkedList();
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if (taskExecution.getStatus() == TaskExecutionStatus.EXECUTION_REQUESTED
            && taskExecution.getPriority() == null) {
          requestedBackgroundTasks.add(taskExecution);
        }
      }
    }
    return requestedBackgroundTasks;
  }

  private TaskExecution getFirstRequestedTask(final int prio) {
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if (taskExecution.getStatus() == TaskExecutionStatus.EXECUTION_REQUESTED
            && taskExecution.getPriority() != null && taskExecution.getPriority() == prio) {
          return taskExecution;
        }
      }
    }
    return null;
  }

  private TaskExecution getFirstPausingTask(final int prio) {
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if (taskExecution.getStatus() == TaskExecutionStatus.PAUSING
            && taskExecution.getPriority() != null && taskExecution.getPriority() == prio) {
          return taskExecution;
        }
      }
    }
    return null;
  }

  private boolean isAnyMoreImportantTaskActive(final int prio) {
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if ((taskExecution.getStatus() == TaskExecutionStatus.EXECUTING
            || taskExecution.getStatus() == TaskExecutionStatus.IDLE)
            && taskExecution.getPriority() != null && taskExecution.getPriority() < prio) {
          return true;
        }
      }
    }
    return false;
  }

  private List<TaskExecution> getLessImportantActiveTasks(final int prio) {
    final List<TaskExecution> lessImportantActiveTasks = new ArrayList<TaskExecution>();
    synchronized (taskExecutions) {
      for (final TaskExecution taskExecution : taskExecutions) {
        if ((taskExecution.getStatus() == TaskExecutionStatus.EXECUTING
            || taskExecution.getStatus() == TaskExecutionStatus.IDLE)
            && taskExecution.getPriority() != null && taskExecution.getPriority() > prio) {
          lessImportantActiveTasks.add(taskExecution);
        }
      }
    }
    return lessImportantActiveTasks;
  }

  private void pauseLessImportantActiveTasks(final int prio) {
    final List<TaskExecution> lessImportantActiveTasks = getLessImportantActiveTasks(prio);
    for (final TaskExecution lessImportantActiveTask : lessImportantActiveTasks) {
      lessImportantActiveTask.requestPause();
    }
  }

  private static void waitMillis(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
    }
  }

}
