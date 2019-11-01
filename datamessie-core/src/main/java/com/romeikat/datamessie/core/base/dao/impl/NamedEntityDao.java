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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.cache.ILazyCache;
import com.romeikat.datamessie.core.base.cache.NamedEntityName2NamedEntityIdCache;
import com.romeikat.datamessie.core.base.service.NamedEntityService;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityImpl;
import com.romeikat.datamessie.model.core.NamedEntity;
import com.romeikat.datamessie.model.core.NamedEntityOccurrence;
import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

@Repository
public class NamedEntityDao extends AbstractEntityWithIdAndVersionCachingDao<NamedEntity> {

  // Cache: named entity name -> named entity
  private final NamedEntityName2NamedEntityIdCache namedEntityName2NamedEntityIdCache;

  @Autowired
  @Qualifier("namedEntityService")
  private NamedEntityService namedEntityService;

  public NamedEntityDao() {
    super(NamedEntityImpl.class);

    namedEntityName2NamedEntityIdCache = new NamedEntityName2NamedEntityIdCache();
  }

  public NamedEntity create(final long id, final String name) {
    return new NamedEntityImpl(id, name);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  @Override
  protected Collection<ILazyCache<?, ?, ?>> getLazyCaches() {
    return Sets.newHashSet(namedEntityName2NamedEntityIdCache);
  }

  @Override
  public void insert(final StatelessSession statelessSession, final NamedEntity namedEntity) {
    super.insert(statelessSession, namedEntity);

    invalidateNamedEntityName(statelessSession, namedEntity);
  }

  private void invalidateNamedEntityName(final SharedSessionContract ssc,
      final NamedEntity namedEntity) {
    final String namedEntityName = namedEntity.getName();
    namedEntityName2NamedEntityIdCache.invalidateKey(namedEntityName);
  }

  /**
   * Provides the named entity with a given name.
   *
   * @param statelessSession
   * @param name
   * @return
   */
  public NamedEntity get(final StatelessSession statelessSession, final String name) {
    final Long namedEntityId = namedEntityName2NamedEntityIdCache.getValue(statelessSession, name);
    if (namedEntityId == null) {
      return null;
    }

    return getEntity(statelessSession, namedEntityId);
  }

  /**
   * Provides the names and IDs of the named entities with given names. Named entities that do not
   * yet exist are being created and saved.
   *
   * @param statelessSession
   * @param names
   * @return
   */
  public Map<String, Long> getOrCreate(final StatelessSession statelessSession,
      final Collection<String> names) {
    final Map<String, Long> result = Maps.newHashMapWithExpectedSize(names.size());

    // Look for named entities with that name in the cache
    final Map<String, Long> existingNamedEntityNames = getFromCache(statelessSession, names);
    result.putAll(existingNamedEntityNames);

    // Create missing named entities
    final Set<String> missingNamedEntityNames = Sets.newHashSet(names);
    missingNamedEntityNames.removeAll(existingNamedEntityNames.keySet());
    for (final String name : missingNamedEntityNames) {
      try {
        final NamedEntity namedEntity = insertIntoDb(statelessSession, name);
        result.put(name, namedEntity.getId());
      }
      // If another thread has inserted the same named entity in the meantime, load it from the
      // cache
      catch (final ConstraintViolationException e) {
        final Long namedEntityId = getFromCache(statelessSession, name);
        if (namedEntityId != null) {
          result.put(name, namedEntityId);
        } else {
          throw e;
        }
      }
    }

    return result;
  }

  private Long getFromCache(final StatelessSession statelessSession, final String name) {
    final Long namedEntityId = namedEntityName2NamedEntityIdCache.getValue(statelessSession, name);
    return namedEntityId;
  }

  private Map<String, Long> getFromCache(final StatelessSession statelessSession,
      final Collection<String> names) {
    final Map<String, Long> namedEntityIds =
        namedEntityName2NamedEntityIdCache.getValues(statelessSession, names);
    return namedEntityIds;
  }

  private NamedEntity insertIntoDb(final StatelessSession statelessSession, final String name) {
    final NamedEntity namedEntity = create();
    namedEntity.setName(name);
    insert(statelessSession, namedEntity);
    return namedEntity;
  }

  public Map<Long, NamedEntity> loadForNamedEntityOccurrences(
      final SharedSessionContract sharedSessionContract,
      final Collection<NamedEntityOccurrence> namedEntityOccurrences) {
    final Set<Long> namedEntityIds = namedEntityOccurrences.stream()
        .map(neo -> neo.getNamedEntityId()).collect(Collectors.toSet());
    namedEntityIds.addAll(namedEntityOccurrences.stream().map(neo -> neo.getParentNamedEntityId())
        .collect(Collectors.toSet()));
    final Map<Long, NamedEntity> namedEntitiesById =
        getIdsWithEntities(sharedSessionContract, namedEntityIds);
    return namedEntitiesById;
  }

}
