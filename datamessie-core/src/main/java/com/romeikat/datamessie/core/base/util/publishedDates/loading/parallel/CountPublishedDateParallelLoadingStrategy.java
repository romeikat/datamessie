package com.romeikat.datamessie.core.base.util.publishedDates.loading.parallel;

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

import org.apache.commons.lang3.mutable.MutableObject;
import org.hibernate.SessionFactory;

import com.romeikat.datamessie.core.base.app.shared.SharedBeanProvider;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;

public abstract class CountPublishedDateParallelLoadingStrategy
    extends PublishedDateParallelLoadingStrategy<MutableObject<Long>> {

  public CountPublishedDateParallelLoadingStrategy(final DocumentsFilterSettings dfs,
      final SessionFactory sessionFactory, final SharedBeanProvider sharedBeanProvider,
      final Double parallelismFactor) {
    super(dfs, sessionFactory, sharedBeanProvider, parallelismFactor);
  }

  @Override
  protected MutableObject<Long> initializeEmptyResult() {
    return new MutableObject<Long>(0l);
  }

  @Override
  protected void mergeResults(final MutableObject<Long> previousResult, final MutableObject<Long> nextResult) {
    final long previousValue = previousResult.getValue();
    final long nextValue = nextResult.getValue();
    final long mergedValue = previousValue + nextValue;

    previousResult.setValue(mergedValue);
  }

}
