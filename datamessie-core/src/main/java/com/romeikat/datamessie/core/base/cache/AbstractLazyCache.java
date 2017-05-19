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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.SharedSessionContract;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;

public abstract class AbstractLazyCache<K, V, S extends SharedSessionContract> implements ILazyCache<K, V, S> {

  private final LinkedHashMapWithMaxSize<K, V> cachedMappings;
  private final HashMultimap<Long, K> dependencies;

  private ReadWriteLock readWriteLock;

  public AbstractLazyCache() {
    this(null);
  }

  public AbstractLazyCache(final Integer maxSize) {
    cachedMappings = new LinkedHashMapWithMaxSize<K, V>(maxSize);
    dependencies = HashMultimap.create();
    readWriteLock = new ReentrantReadWriteLock();

    putInitialMappings();
  }

  protected Map<K, V> getInitialMappings() {
    return Collections.emptyMap();
  }

  protected abstract V loadValue(S session, K key);

  @Override
  public Set<K> getKnownKeys() {
    readWriteLock.readLock().lock();
    final Set<K> knownKeys = Sets.newHashSet(cachedMappings.keySet());
    readWriteLock.readLock().unlock();
    return knownKeys;
  }

  @Override
  public Set<V> getKnownValues() {
    readWriteLock.readLock().lock();
    final Set<V> knownValues = Sets.newHashSet(cachedMappings.values());
    readWriteLock.readLock().unlock();
    return knownValues;
  }

  @Override
  public V getValue(final S session, final K key) {
    // First: get value from cache
    readWriteLock.readLock().lock();
    if (cachedMappings.containsKey(key)) {
      final V value = cachedMappings.get(key);
      readWriteLock.readLock().unlock();
      return value;
    }
    readWriteLock.readLock().unlock();

    // Second, load value and cache new mapping
    final V value = loadValue(session, key);
    readWriteLock.writeLock().lock();
    cachedMappings.put(key, value);
    readWriteLock.writeLock().unlock();

    return value;
  }

  private void putInitialMappings() {
    final Map<K, V> initialMappings = getInitialMappings();
    for (final K key : initialMappings.keySet()) {
      final V value = initialMappings.get(key);
      putMapping(key, value);
    }
  }

  private void putMapping(final K key, final V value) {
    if (key == null) {
      return;
    }

    cachedMappings.put(key, value);
  }

  @Override
  public void registerDependency(final Long id, final K key) {
    readWriteLock.writeLock().lock();
    dependencies.put(id, key);
    readWriteLock.writeLock().unlock();
  }

  @Override
  public void registerDependencies(final Collection<Long> ids, final K key) {
    readWriteLock.writeLock().lock();
    for (final Long id : ids) {
      dependencies.put(id, key);
    }
    readWriteLock.writeLock().unlock();
  }

  @Override
  public void invalidateEntity(final Long id) {
    readWriteLock.writeLock().lock();
    final Set<K> keys = dependencies.removeAll(id);
    for (final K key : keys) {
      cachedMappings.remove(key);
    }
    readWriteLock.writeLock().unlock();
  }

  @Override
  public void invalidateKey(final K key) {
    readWriteLock.writeLock().lock();
    for (final Long id : dependencies.keySet()) {
      final Set<K> keys = dependencies.get(id);
      keys.remove(key);
    }
    cachedMappings.remove(key);
    readWriteLock.writeLock().unlock();
  }

  @Override
  public void invalidateAll() {
    readWriteLock.writeLock().lock();
    dependencies.clear();
    cachedMappings.clear();
    readWriteLock.writeLock().unlock();
  }

}
