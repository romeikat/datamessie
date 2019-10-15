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

import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import com.romeikat.datamessie.core.domain.entity.Entity;


public interface EntityDao<E extends Entity> {

  E create();

  List<E> getAllEntites(SharedSessionContract ssc);

  List<E> getEntites(SharedSessionContract ssc, Integer firstResult, Integer maxResults);

  /**
   * Provides all entities by a given property.
   *
   * @param ssc
   * @param property
   * @param value
   * @return
   */
  List<E> getEntitesByProperty(SharedSessionContract ssc, String property, Object value);

  /**
   * Provides all entities by a given property.
   *
   * @param ssc
   * @param property
   * @param value
   * @param firstResult
   * @param maxResults
   * @return
   */
  List<E> getEntitesByProperty(SharedSessionContract ssc, String property, Object value,
      Integer firstResult, Integer maxResults);

  /**
   * Provides a unique entity by a given property value.
   *
   * @param ssc
   * @param property
   * @param value
   * @return
   */
  E getUniqueEntityByProperty(SharedSessionContract ssc, String property, Object value);

  /**
   * Counts all entities.
   *
   * @param ssc
   * @return
   */
  long countAll(SharedSessionContract ssc);

  /**
   * Inserts a new entity. If the entity already exists in the database, a copy of it is saved with
   * a new id.
   *
   * @param statelessSession
   * @param entity
   */
  void insert(StatelessSession statelessSession, E entity);

  /**
   * Updates an existing entity. If the entity does not exist in the database, an exception is
   * thrown.
   *
   * @param statelessSession
   * @param entity
   */
  void update(StatelessSession statelessSession, E entity);

  /**
   * Deletes an existing entity. If the entity does not exist in the database, an exception is
   * thrown.
   *
   * @param statelessSession
   * @param entity
   */
  void delete(StatelessSession statelessSession, E entity);

}
