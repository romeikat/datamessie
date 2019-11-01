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

import java.util.Collection;
import java.util.Map;
import com.google.common.base.Function;
import com.romeikat.datamessie.model.Entity;
import jersey.repackaged.com.google.common.collect.Maps;

public class EntitiesById<T extends Entity> {

  private final Map<Long, T> entitiesWithId;

  public EntitiesById(final Collection<T> entities, final Function<? super T, Long> keyFunction) {
    this.entitiesWithId = Maps.newHashMap();

    for (final T entity : entities) {
      final Long id = keyFunction.apply(entity);
      entitiesWithId.put(id, entity);
    }
  }

  public synchronized T poll(final Long id) {
    if (id == null) {
      return null;
    }

    final T entity = entitiesWithId.remove(id);
    return entity;
  }

  public synchronized Collection<T> getObjects() {
    return entitiesWithId.values();
  }

}
