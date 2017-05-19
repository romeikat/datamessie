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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;

public class ParallelProcessingTest extends AbstractTest {

  @Autowired
  private CollectionUtil collectionUtil;

  @Test
  public void process_withoutException() {
    final int numberOfIndexes = 1000000;
    final List<Integer> indices = collectionUtil.createIntegerList(null, numberOfIndexes);

    final ConcurrentMap<Integer, Object> indicesMap = new ConcurrentHashMap<Integer, Object>();
    new ParallelProcessing<Integer>(null, indices) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final Integer index) {
        indicesMap.put(index, index);
      }
    };

    assertEquals(numberOfIndexes, indicesMap.keySet().size());
  }

  @Test(expected = Exception.class)
  public void process_withException() {
    final int numberOfIndexes = 10;
    final List<Integer> indexes = collectionUtil.createIntegerList(null, numberOfIndexes);
    for (int i = 0; i < numberOfIndexes; i++) {
      indexes.add(i);
    }

    final ConcurrentMap<Integer, Object> indicesMap = new ConcurrentHashMap<Integer, Object>();
    new ParallelProcessing<Integer>(null, indexes) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final Integer index) {
        // Null values in a ConcurrentHashMap cause a NullPointerException
        indicesMap.put(index, null);
      }
    };
  }

}
