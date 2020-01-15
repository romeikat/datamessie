package com.romeikat.datamessie.core.base.util.parallelProcessing;

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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequentialAsynchronousTask {

  private static final Logger LOG = LoggerFactory.getLogger(SequentialAsynchronousTask.class);

  private static final boolean ASYNC = false;

  private final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private Future<?> future;

  public void submit(final Runnable runnable) {
    // Wait until previous execution is completed
    waitUntilCompleted();

    // Trigger new execution
    future = executor.submit(runnable);

    // If sync mode, wait until completed
    if (!ASYNC) {
      waitUntilCompleted();
    }
  }

  public void waitUntilCompleted() {
    try {
      // No execution in progress
      if (future == null) {
        return;
      }

      // Wait until current execution is done
      future.get();
    } catch (final InterruptedException e) {
    } catch (final ExecutionException e) {
      LOG.error("Could not execute task asynchronously", e);
      throw new RuntimeException(e);
    }
  }

}
