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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;

class InternalRecursiveAction<T> extends RecursiveAction {

  private final static Logger LOG = LoggerFactory.getLogger(InternalRecursiveAction.class);

  private static final long serialVersionUID = 1L;

  private final int numberOfThreads;

  private final boolean divideAnyFurther;

  private final List<T> objectsToBeProcessed;

  private ParallelProcessing<T> parallelProcessing;

  InternalRecursiveAction(final List<T> objectsToBeProcessed, final int numberOfThreads,
      final ParallelProcessing<T> parallelProcessing) {
    this(objectsToBeProcessed, numberOfThreads, parallelProcessing, true);
  }

  private InternalRecursiveAction(final List<T> objectsToBeProcessed, final int numberOfThreads,
      final ParallelProcessing<T> parallelProcessing, final boolean divideAnyFurther) {
    this.objectsToBeProcessed = objectsToBeProcessed;
    this.numberOfThreads = numberOfThreads;
    this.parallelProcessing = parallelProcessing;
    this.divideAnyFurther = divideAnyFurther;
  }

  @Override
  protected void compute() {
    if (divideAnyFurther) {
      divideAndInvoke();
    } else {
      // Session management
      final HibernateSessionProvider sessionProvider = parallelProcessing.createSessionProvider();
      // Preprocessing
      try {
        parallelProcessing.onBeforeProcessing(sessionProvider);
      } catch (final Exception e) {
        LOG.error("Error before parallel processing", e);
        if (sessionProvider != null) {
          sessionProvider.closeSession();
          sessionProvider.closeStatelessSession();
        }
        throw e;
      }
      // Processing
      for (final T objectToBeProcessed : objectsToBeProcessed) {
        try {
          parallelProcessing.doProcessing(sessionProvider, objectToBeProcessed);
        } catch (final Exception e) {
          LOG.error("Error during parallel processing", e);
          if (sessionProvider != null) {
            sessionProvider.closeSession();
            sessionProvider.closeStatelessSession();
          }
          throw e;
        }
      }
      // Postprocessing
      try {
        parallelProcessing.onAfterProcessing(sessionProvider);
      } catch (final Exception e) {
        LOG.error("Error after parallel processing", e);
        if (sessionProvider != null) {
          sessionProvider.closeSession();
          sessionProvider.closeStatelessSession();
        }
        throw e;
      }
      // Session management
      if (sessionProvider != null) {
        sessionProvider.closeSession();
        sessionProvider.closeStatelessSession();
      }
    }
  }

  private void divideAndInvoke() {
    // Divide
    final List<List<T>> subLists =
        CollectionUtil.splitIntoSubListsByNumber(objectsToBeProcessed, numberOfThreads);
    final int numberOfSubLists = subLists.size();
    final List<InternalRecursiveAction<T>> subActions =
        new ArrayList<InternalRecursiveAction<T>>(numberOfSubLists);
    for (final List<T> subList : subLists) {
      final InternalRecursiveAction<T> subAction =
          new InternalRecursiveAction<T>(subList, numberOfThreads, parallelProcessing, false);
      subActions.add(subAction);
    }
    // Invoke
    RecursiveAction[] subActionsAsArray = new RecursiveAction[numberOfSubLists];
    subActionsAsArray = subActions.toArray(subActionsAsArray);
    invokeAll(subActionsAsArray);
  }

}
