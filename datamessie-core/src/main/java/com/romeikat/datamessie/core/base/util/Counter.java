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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Counter<T> {

  private final Map<T, Integer> counter;

  public Counter() {
    counter = new HashMap<T, Integer>();
  }

  public synchronized void count(final T object) {
    Integer number = counter.get(object);
    if (number == null) {
      number = 0;
    }
    number++;
    counter.put(object, number);
  }

  public synchronized Integer getNumber(final T object) {
    final Integer number = counter.get(object);
    return number;
  }

  public synchronized Set<T> getObjectsWithNumberOrAbove(final int minNumber) {
    final Set<T> objects = new HashSet<T>();
    for (final T object : counter.keySet()) {
      final Integer number = counter.get(object);
      if (number != null && number >= minNumber) {
        objects.add(object);
      }
    }
    return objects;
  }

}
