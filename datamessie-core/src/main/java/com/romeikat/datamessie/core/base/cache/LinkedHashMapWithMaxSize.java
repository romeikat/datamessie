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

import java.util.LinkedHashMap;

public class LinkedHashMapWithMaxSize<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = 1L;

  static final int DEFAULT_MAX_SIZE = 1000;
  static final float LOAD_FACTOR = 0.75f;

  private final Integer maxSize;

  public LinkedHashMapWithMaxSize(final Integer maxSize) {
    super(capacity(maxSize), LOAD_FACTOR, true);

    this.maxSize = maxSize;
  }

  @Override
  protected boolean removeEldestEntry(final java.util.Map.Entry<K, V> eldest) {
    if (maxSize == null) {
      return false;
    }

    return size() > maxSize;
  }

  private static int capacity(Integer maxSize) {
    if (maxSize == null) {
      maxSize = DEFAULT_MAX_SIZE;
    }

    if (maxSize < 3) {
      return maxSize + 1;
    }

    return maxSize + maxSize / 3;
  }


}
