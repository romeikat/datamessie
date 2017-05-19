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

import java.util.Comparator;
import java.util.Map;

public class MapValueKeyComparator<K, V> implements Comparator<K> {

  private final Map<K, V> map;

  private final Comparator<? super K> keyComparator;

  private final Comparator<? super V> valueComparator;

  public MapValueKeyComparator(final Map<K, V> map, final Comparator<? super K> keyComparator,
      final Comparator<? super V> valueComparator) {
    this.map = map;
    this.keyComparator = keyComparator;
    this.valueComparator = valueComparator;
  }

  @Override
  public int compare(final K key1, final K key2) {
    final V value1 = map.get(key1);
    final V value2 = map.get(key2);
    // Compare by value
    final int valuesCompared = valueComparator.compare(value1, value2);
    if (valuesCompared != 0) {
      return valuesCompared;
    }
    // Compare by key
    return keyComparator.compare(key1, key2);
  }

}
