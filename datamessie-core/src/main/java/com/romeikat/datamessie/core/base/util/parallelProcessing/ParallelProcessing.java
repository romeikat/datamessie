package com.romeikat.datamessie.core.base.util.parallelProcessing;

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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;

import jersey.repackaged.com.google.common.collect.Lists;

public abstract class ParallelProcessing<T> {

  private final static Logger LOG = LoggerFactory.getLogger(ParallelProcessing.class);

  private final static double DEFAULT_PARALLELISM_FACTOR = 3d;

  private final int numberOfThreads;

  private final ForkJoinPool forkJoinPool;

  private final SessionFactory sessionFactory;

  private final List<T> objectsToBeProcessed;

  public ParallelProcessing(final SessionFactory sessionFactory, final Collection<T> objectsToBeProcessed) {
    this(sessionFactory, Lists.newArrayList(objectsToBeProcessed));
  }

  public ParallelProcessing(final SessionFactory sessionFactory, final Collection<T> objectsToBeProcessed,
      final Double parallelismFactor) {
    this(sessionFactory, Lists.newArrayList(objectsToBeProcessed), DEFAULT_PARALLELISM_FACTOR);
  }

  public ParallelProcessing(final SessionFactory sessionFactory, final List<T> objectsToBeProcessed) {
    this(sessionFactory, objectsToBeProcessed, DEFAULT_PARALLELISM_FACTOR);
  }

  public ParallelProcessing(final SessionFactory sessionFactory, final List<T> objectsToBeProcessed,
      final Double parallelismFactor) {
    // Create pool
    numberOfThreads = getNumberOfThreads(parallelismFactor);
    if (numberOfThreads == 1) {
      LOG.debug("Processing with one single thread");
    } else {
      final String threadsString = numberOfThreads == 1 ? "thread" : "threads";
      LOG.debug("Processing with {} {}", numberOfThreads, threadsString);
    }
    forkJoinPool = new ForkJoinPool(numberOfThreads);
    this.sessionFactory = sessionFactory;
    this.objectsToBeProcessed = objectsToBeProcessed;

    // Process
    doParallelProcessing();

    // Shutdown pool
    forkJoinPool.shutdownNow();
  }

  protected HibernateSessionProvider createSessionProvider() {
    if (sessionFactory == null) {
      return null;
    }

    return new HibernateSessionProvider(sessionFactory);
  }

  private void doParallelProcessing() {
    // Do parallel processing
    final InternalRecursiveAction<T> action =
        new InternalRecursiveAction<T>(objectsToBeProcessed, numberOfThreads, this);
    forkJoinPool.invoke(action);
  }

  protected void onBeforeProcessing(final HibernateSessionProvider sessionProvider) {}

  public abstract void doProcessing(HibernateSessionProvider sessionProvider, T objectToBeProcessed);

  protected void onAfterProcessing(final HibernateSessionProvider sessionProvider) {}

  public static int getNumberOfThreads(final Double parallelismFactor) {
    if (parallelismFactor == null || parallelismFactor == 0) {
      final int numberOfThreads = 1;
      return numberOfThreads;
    }

    final int numberOfAvailableCores = Runtime.getRuntime().availableProcessors();
    final int numberOfThreads = (int) Math.floor(parallelismFactor * numberOfAvailableCores);
    return numberOfThreads;
  }

}
