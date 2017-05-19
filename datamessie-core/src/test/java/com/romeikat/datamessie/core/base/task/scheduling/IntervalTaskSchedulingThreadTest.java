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

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskManager;

public class IntervalTaskSchedulingThreadTest extends AbstractTest {

  private static final long TASK_MANAGEMENT_INTERVAL = 10;

  private static final int NUMBER_OF_TASK_EXECUTIONS = 5;

  @Autowired
  private TaskManager taskManager;

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    taskManager.setManagementInterval(TASK_MANAGEMENT_INTERVAL);
  }

  @Test
  public void run_overlappingModeWithoutOverlap() throws Exception {
    run(50, 0, true, false);
  }

  @Test
  public void run_overlappingModeWithOverlap() throws Exception {
    run(50, 100, true, false);
  }

  @Test
  public void run_nonOverlappingModeWithoutOverlap() throws Exception {
    run(50, 0, false, false);
  }

  @Test
  public void run_nonOverlappingModeWithOverlap() throws Exception {
    run(50, 100, false, false);
  }

  @Test
  public void run_overlappingModeWithExceptions() throws Exception {
    run(50, 50, true, true);
  }

  @Test
  public void run_nonOverlappingModeWithExceptions() throws Exception {
    run(50, 50, false, true);
  }

  private void run(final int taskExecutionInterval, final long taskExecutionDuration, final boolean allowOverlap,
      final boolean throwException) throws Exception {

    final MutableInt numberOfTaskExecutions = new MutableInt(0);
    final MutableInt numberOfTaskTriggerings = new MutableInt(0);
    final Task task = Mockito.spy(new FooTask() {
      @Override
      public void execute(final TaskExecution taskExecution) throws Exception {
        if (throwException) {
          throw new Exception();
        }
        numberOfTaskExecutions.add(1);
        Thread.sleep(taskExecutionDuration);
      }
    });
    final String taskName = task.getName();

    // Schedule task for multiple execution
    final MutableObject<LocalDateTime> startOfLatestCompletedTask = new MutableObject<LocalDateTime>();
    final FooIntervalTaskSchedulingThread intervalTaskSchedulingThread =
        new FooIntervalTaskSchedulingThread(task, taskName, taskExecutionInterval, allowOverlap, taskManager) {
          @Override
          protected boolean shouldStopTaskExecution() {
            return numberOfTaskTriggerings.intValue() == NUMBER_OF_TASK_EXECUTIONS;
          }

          @Override
          protected void onAfterTriggeringTask(final TaskExecution latestTaskExecution) {
            startOfLatestCompletedTask.setValue(LocalDateTime.now());
            numberOfTaskTriggerings.add(1);
          }

          @Override
          protected LocalDateTime getActualStartOfLatestCompletedTask() {
            return startOfLatestCompletedTask.getValue();
          }
        };
    intervalTaskSchedulingThread.start();

    // Wait until all executions are finished
    final long additionalWaiting = TaskManager.DEFAULT_MANAGEMENT_INTERVAL + 1000;
    final long timePerTaskExecution =
        throwException ? taskExecutionInterval : taskExecutionInterval + taskExecutionDuration;
    final long waitUntilTaskExecutionShouldBeFinished =
        NUMBER_OF_TASK_EXECUTIONS * timePerTaskExecution + additionalWaiting;
    Thread.sleep(waitUntilTaskExecutionShouldBeFinished);

    // Check executions
    final int expectedNumberOfTaskTriggerings = NUMBER_OF_TASK_EXECUTIONS;
    assertEquals(expectedNumberOfTaskTriggerings, numberOfTaskTriggerings.intValue());
    final int expectedNumbeOfTaskExecutions = throwException ? 0 : NUMBER_OF_TASK_EXECUTIONS;
    assertEquals(expectedNumbeOfTaskExecutions, numberOfTaskExecutions.intValue());
  }

}
