package com.romeikat.datamessie.core.base.cache;

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
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import com.romeikat.datamessie.core.AbstractTest;

public class LinkedHashMapWithMaxSizeTest extends AbstractTest {

  private static final int MAX_SIZE = 10;

  @Test
  public void add_removesEldestEntry() {
    final LinkedHashMapWithMaxSize<Integer, Object> map =
        new LinkedHashMapWithMaxSize<Integer, Object>(MAX_SIZE);

    // Fill up map
    for (int i = 1; i <= MAX_SIZE; i++) {
      map.put(i, null);
    }

    // Access 1, so 2 becomes the eldest
    map.get(1);

    // Put another one (in order to remove eldest)
    map.put(MAX_SIZE + 1, null);

    assertEquals(MAX_SIZE, map.size());
    assertFalse(map.containsKey(2));
  }

}
