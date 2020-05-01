package com.romeikat.datamessie.core.base.dao;

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
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;


public interface EntityWithIdDao<E extends EntityWithId> extends EntityDao<E> {

  E getEntity(SharedSessionContract ssc, long id);

  List<E> getEntities(SharedSessionContract ssc, Collection<Long> ids);

  Map<Long, E> getIdsWithEntities(SharedSessionContract ssc);

  Map<Long, E> getIdsWithEntities(SharedSessionContract ssc, Collection<Long> ids);

  /**
   * Provides the ids of all entities.
   *
   * @param ssc
   * @return
   */
  List<Long> getIds(SharedSessionContract ssc);

  /**
   * Provides the ids of the respective entities.
   *
   * @param ssc
   * @param firstId
   * @param maxResults
   * @return
   */
  List<Long> getIds(SharedSessionContract ssc, Long firstId, Integer maxResults);

  /**
   * Provides the ids of the respective entities.
   *
   * @param ssc
   * @param ids
   * @return
   */
  List<Long> getIds(SharedSessionContract ssc, Collection<Long> ids);

  /**
   * Provides the ids of the respective entities.
   *
   * @param ssc
   * @param ids
   * @param firstId
   * @param maxResults
   * @return
   */
  List<Long> getIds(SharedSessionContract ssc, Collection<Long> ids, Long firstId,
      Integer maxResults);

  /**
   * Provides the ids of all entities.
   *
   * @param ssc
   * @return
   */
  Long getMaxId(SharedSessionContract ssc);

  /**
   * Determines whether an entity already exists in the database.
   *
   * @param ssc
   * @param id
   * @return
   */
  boolean exists(SharedSessionContract ssc, long id);

  /**
   * Inserts a new entity. If the entity already exists in the database, a copy of it is saved with
   * a new id.
   *
   * @param statelessSession
   * @param entity
   * @return The ID of the entity
   */
  @Override
  Long insert(StatelessSession statelessSession, E entity);

  /**
   * If the entity does not exist in the database, it is inserted. If the entity already exists in
   * the database, it is updated.
   *
   * @param statelessSession
   * @param entity
   * @return The ID of the entity
   */
  Long insertOrUpdate(StatelessSession statelessSession, E entity);

}
