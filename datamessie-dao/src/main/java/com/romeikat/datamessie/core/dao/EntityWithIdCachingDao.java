package com.romeikat.datamessie.core.dao;

import com.romeikat.datamessie.core.domain.entity.EntityWithId;


public interface EntityWithIdCachingDao<E extends EntityWithId> extends EntityWithIdDao<E> {

  void clearCaches();

  @Override
  void update(E entity);

  @Override
  void delete(E entity);

}
