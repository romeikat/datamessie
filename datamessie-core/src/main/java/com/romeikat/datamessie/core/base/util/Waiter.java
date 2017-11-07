package com.romeikat.datamessie.core.base.util;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;

public abstract class Waiter {

  private static final long DEFAULT_SLEEPING_INTERVAL = 5000;

  private static final Logger LOG = LoggerFactory.getLogger(Waiter.class);

  private String feedbackMessage;

  private TaskExecution taskExecution;

  private long sleepingInterval;

  public Waiter() {
    sleepingInterval = DEFAULT_SLEEPING_INTERVAL;
  }

  public Waiter setFeedbackMessage(final String feedbackMessage) {
    this.feedbackMessage = feedbackMessage;
    return this;
  }

  public Waiter setTaskExecution(final TaskExecution taskExecution) {
    this.taskExecution = taskExecution;
    return this;
  }

  public Waiter setSleepingInterval(final long sleepingInterval) {
    this.sleepingInterval = sleepingInterval;
    return this;
  }

  public void waitUntilConditionIsFulfilled() {
    boolean feedbackProvided = false;
    while (true) {
      // If pipeline is available, return
      if (isConditionFulfilled()) {
        return;
      }
      // Provide feedback once
      if (!feedbackProvided) {
        if (feedbackMessage != null) {
          // Feedback via logger
          LOG.debug(feedbackMessage);
          // Feedback via task, if available
          if (taskExecution != null) {
            taskExecution.reportWork(feedbackMessage);
            taskExecution.reportEmptyWork();
          }
        }
        feedbackProvided = true;
      }
      // Wait
      try {
        // Wait via task, if available
        if (taskExecution != null) {
          taskExecution.checkpoint(sleepingInterval);
        }
        // Wait via thread
        else {
          Thread.sleep(sleepingInterval);
        }
      } catch (final TaskCancelledException e) {
      } catch (final InterruptedException e) {
      }
    }
  }

  public abstract boolean isConditionFulfilled();

}
