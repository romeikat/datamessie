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

import static org.junit.Assert.assertEquals;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.scheduling.FooTask;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public class TaskManagerTest extends AbstractTest {

  private static final long TASK_MANAGEMENT_INTERVAL = 10;

  @Autowired
  private TaskManager taskManager;

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    taskManager.reset();
    taskManager.setManagementInterval(TASK_MANAGEMENT_INTERVAL);
  }

  @Test
  public void executes_successfulAndFailingTasks() throws Exception {
    executes(5, 5);
  }

  private void executes(final int numberOfSuccessfulTasks, final int numberOfFailingTasks)
      throws Exception {
    // Successful tasks
    final Collection<TaskExecution> successfulTaskExecutions =
        Sets.newHashSetWithExpectedSize(numberOfSuccessfulTasks);
    for (int i = 0; i < numberOfSuccessfulTasks; i++) {
      final Task task = Mockito.spy(new FooTask());
      final TaskExecution taskExecution = taskManager.addTask(task);
      successfulTaskExecutions.add(taskExecution);
    }

    // Failing tasks
    final Collection<TaskExecution> failingTaskExecutions =
        Sets.newHashSetWithExpectedSize(numberOfFailingTasks);
    for (int i = 0; i < numberOfFailingTasks; i++) {
      final Task task = Mockito.spy(new FooTask() {
        @Override
        public void execute(final TaskExecution taskExecution) throws Exception {
          throw new Exception();
        }
      });
      final TaskExecution taskExecution = taskManager.addTask(task);
      failingTaskExecutions.add(taskExecution);
    }

    // Wait until all executions are finished
    final int numberOfTasks = numberOfSuccessfulTasks + numberOfFailingTasks;
    final long waitUntilTaskManagementShouldBeFinished =
        TaskManager.DEFAULT_MANAGEMENT_INTERVAL + (numberOfTasks * TASK_MANAGEMENT_INTERVAL) + 1000;
    Thread.sleep(waitUntilTaskManagementShouldBeFinished);

    // Check results
    for (final TaskExecution successfulTaskExecution : successfulTaskExecutions) {
      assertEquals(TaskExecutionStatus.COMPLETED, successfulTaskExecution.getStatus());
    }
    for (final TaskExecution failingTaskExecution : failingTaskExecutions) {
      assertEquals(TaskExecutionStatus.FAILED, failingTaskExecution.getStatus());
    }
  }

}
