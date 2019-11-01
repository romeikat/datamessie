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

import java.util.Map;
import java.util.Set;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.model.util.Identifiable;

public class IdBasedMap<V extends Identifiable> {

  private final Map<Long, V> keysToValues;

  public IdBasedMap() {
    keysToValues = Maps.newHashMap();
  }

  public synchronized V get(final long key) {
    return keysToValues.get(key);
  }

  public synchronized void add(final V value) {
    if (value == null) {
      return;
    }

    final long key = value.getId();
    keysToValues.put(key, value);
  }

  public synchronized void remove(final long key) {
    keysToValues.remove(key);
  }

  public synchronized void remove(final V value) {
    if (value == null) {
      return;
    }

    keysToValues.remove(value.getId());
  }

  public synchronized boolean containsKey(final long key) {
    return keysToValues.containsKey(key);
  }

  public synchronized boolean containsValue(final V value) {
    if (value == null) {
      return false;
    }

    final long key = value.getId();
    return containsKey(key);
  }

  public synchronized Set<Long> keySet() {
    return Sets.newHashSet(keysToValues.keySet());
  }

  public synchronized Set<V> values() {
    return Sets.newHashSet(keysToValues.values());
  }

}
