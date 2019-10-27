package com.romeikat.datamessie.core.base.dao.impl;

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
import org.hibernate.StatelessSession;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.cache.ILazyCache;
import com.romeikat.datamessie.core.base.dao.EntityWithIdCachingDao;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;

@Repository
public abstract class AbstractEntityWithIdAndVersionCachingDao<E extends EntityWithIdAndVersion>
    extends AbstractEntityWithIdAndVersionDao<E> implements EntityWithIdCachingDao<E> {

  public AbstractEntityWithIdAndVersionCachingDao(final Class<? extends E> entityClass) {
    super(entityClass);
  }

  protected abstract Collection<ILazyCache<?, ?, ?>> getLazyCaches();

  @Override
  public void clearCaches() {
    for (final ILazyCache<?, ?, ?> lazyCache : getLazyCaches()) {
      lazyCache.invalidateAll();
    }
  }

  @Override
  public synchronized void update(final StatelessSession statelessSession, final E entity) {
    super.update(statelessSession, entity);

    for (final ILazyCache<?, ?, ?> lazyCache : getLazyCaches()) {
      lazyCache.invalidateEntity(entity.getId());
    }
  }

  @Override
  public synchronized void delete(final StatelessSession statelessSession, final E entity) {
    super.delete(statelessSession, entity);

    for (final ILazyCache<?, ?, ?> lazyCache : getLazyCaches()) {
      lazyCache.invalidateEntity(entity.getId());
    }
  }

}
