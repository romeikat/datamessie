package com.romeikat.datamessie.dao;

/*-
 * ============================LICENSE_START============================
 * data.messie (dao)
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

import com.romeikat.datamessie.model.EntityWithId;


public interface EntityWithIdCachingDao<E extends EntityWithId> extends EntityWithIdDao<E> {

  void clearCaches();

  @Override
  void update(E entity);

  @Override
  void delete(E entity);

}
