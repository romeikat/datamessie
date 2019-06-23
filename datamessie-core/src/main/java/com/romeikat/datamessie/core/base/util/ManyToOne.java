package com.romeikat.datamessie.core.base.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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
import java.util.Map;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jersey.repackaged.com.google.common.collect.Maps;

public class ManyToOne<K, V> {

  private final Map<K, V> leftToRight;
  private final Multimap<V, K> rightToLeft;

  public ManyToOne() {
    leftToRight = Maps.newHashMap();
    rightToLeft = HashMultimap.create();
  }

  public synchronized void put(final K key, final V value) {
    leftToRight.put(key, value);
    rightToLeft.put(value, key);
  }

  public synchronized boolean containsKey(final K key) {
    return leftToRight.containsKey(key);
  }

  public synchronized boolean containsValue(final V value) {
    return rightToLeft.containsKey(value);
  }

  public synchronized K getEqualKey(final K key) {
    if (!leftToRight.containsKey(key)) {
      return null;
    }

    final Collection<K> keyCandidates = getKeys(getValue(key));
    for (final K existingKey : keyCandidates) {
      if (existingKey.equals(key)) {
        return existingKey;
      }
    }

    return null;
  }

  public synchronized V getValue(final K key) {
    return leftToRight.get(key);
  }

  public synchronized Collection<K> getKeys(final V value) {
    return rightToLeft.get(value);
  }

  public synchronized void prefer(final V value) {
    final Collection<K> keys = getKeys(value);
    for (final K key : keys) {
      leftToRight.put(key, value);
    }

    rightToLeft.removeAll(value);
    for (final K key : keys) {
      rightToLeft.put(value, key);
    }
  }

}
