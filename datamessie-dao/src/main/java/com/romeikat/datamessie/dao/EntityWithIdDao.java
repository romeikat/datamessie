package com.romeikat.datamessie.dao;

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
import java.util.List;
import java.util.Map;
import com.romeikat.datamessie.model.EntityWithId;


public interface EntityWithIdDao<E extends EntityWithId> extends EntityDao<E> {

  E getEntity(long id);

  List<E> getEntities(Collection<Long> ids);

  Map<Long, E> getIdsWithEntities();

  Map<Long, E> getIdsWithEntities(Collection<Long> ids);

  /**
   * Provides the ids of all entities.
   *
   * @return
   */
  List<Long> getIds();

  /**
   * Provides the ids of the respective entities.
   *
   * @param firstId
   * @param maxResults
   * @return
   */
  List<Long> getIds(Long firstId, Integer maxResults);

  /**
   * Provides the ids of the respective entities.
   *
   * @param ids
   * @return
   */
  List<Long> getIds(Collection<Long> ids);

  /**
   * Provides the ids of the respective entities.
   *
   * @param ids
   * @param firstId
   * @param maxResults
   * @return
   */
  List<Long> getIds(Collection<Long> ids, Long firstId, Integer maxResults);

  /**
   * Provides the ids of all entities.
   *
   * @return
   */
  Long getMaxId();

  /**
   * If the entity does not exist in the database, it is inserted. If the entity already exists in
   * the database, it is updated.
   *
   * @param entity
   */
  void insertOrUpdate(E entity);

}
