package com.romeikat.datamessie.core.base.util.sparsetable;

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

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;

public class SparseSingleTableTest extends AbstractTest {

  private static final int NUMBER_OF_ITERATIONS = 100;

  @Test
  public void serializeAndDeserialize() throws Exception {

    final SparseSingleTable<Double, Double, Double> table = new SparseSingleTable<Double, Double, Double>();
    for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
      final double rowHeader = Math.random();
      final double columnHeader = Math.random();
      final double value = Math.random();
      table.putValue(rowHeader, columnHeader, value);
    }

    final byte[] serializedTable = SerializationUtils.serialize(table);
    @SuppressWarnings("unchecked")
    final SparseSingleTable<Double, Double, Double> deserializedTable =
        (SparseSingleTable<Double, Double, Double>) SerializationUtils.deserialize(serializedTable);

    assertEquals(table, deserializedTable);

  }

}
