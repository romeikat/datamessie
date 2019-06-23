package com.romeikat.datamessie.core.base.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.collect.Maps;

/**
 * A disjoint-set data structure. Implements the common optimizations union by rank and path
 * compression. The implementation is tread-safe.
 *
 * @author Dr. Raphael Romeikat
 *
 * @param <T> The item type.
 */
public class DisjointSet<T> {

  /**
   * Parent relationships.
   */
  private final Map<T, T> parents = Maps.newHashMap();

  /**
   * Child relationships for the roots.
   */
  private final Multimap<T, T> children = HashMultimap.create();

  /**
   * Depth of trees.
   */
  private final Map<T, Integer> rank = Maps.newHashMap();

  /**
   * Creates an empty disjoint-set.
   *
   * @param items The items of the disjoint-set.
   */
  public DisjointSet() {
    this(Collections.emptySet());
  }

  /**
   * Creates a disjoint-set.
   *
   * @param items The items of the disjoint-set.
   */
  public DisjointSet(final Collection<T> items) {
    addItems(items);
  }

  public synchronized void addItem(final T item) {
    if (parents.containsKey(item)) {
      return;
    }

    parents.put(item, item);
    children.put(item, item);
    rank.put(item, 0);
  }

  public synchronized void addItems(final Collection<T> items) {
    for (final T item : items) {
      addItem(item);
    }
  }

  public synchronized Set<T> getAllItems() {
    return parents.keySet();
  }

  public synchronized Set<T> getItemsOfSet(final T item) {
    final T root = find(item);
    if (root == null) {
      return Collections.emptySet();
    }

    return (Set<T>) children.get(root);
  }

  public synchronized Set<Set<T>> getSubsets() {
    final Set<Set<T>> result = Sets.newHashSet();
    for (final T root : children.keySet()) {
      final Set<T> subset = (Set<T>) children.get(root);
      result.add(subset);
    }
    return result;
  }

  /**
   * Performs the union of two subsets.
   *
   * @param item1 An item of the first subset.
   * @param item2 An item of the second subset.
   */
  public synchronized void union(final T item1, final T item2) {
    // Find the roots of the two sets
    final T root1 = find(item1);
    final T root2 = find(item2);

    // item1 and item2 are already in the same set
    if (root1 == root2) {
      return;
    }

    // Attach smaller tree under the root of the deeper tree

    // Put root2 under root1
    if (rank.get(root1) > rank.get(root2)) {
      merge(root2, root1);
    }
    // Put root1 under root2
    else if (rank.get(root1) < rank.get(root2)) {
      merge(root1, root2);
    }
    // Does not matter; put root1 under root2
    else {
      merge(root1, root2);
      rank.put(root2, rank.get(root2) + 1);
    }
  }

  /**
   * Determines the representative of an item.
   *
   * @param item The item.
   * @return The root of the set in which an <code>item</code> belongs.
   */
  private T find(final T item) {
    if (!parents.containsKey(item)) {
      return null;
    }

    // Item is not root
    if (parents.get(item) != item) {
      // Path compression
      parents.put(item, find(parents.get(item)));
    }

    return parents.get(item);
  }

  private void merge(final T oldRoot, final T newRoot) {
    parents.put(oldRoot, newRoot);
    for (final T child : children.get(oldRoot)) {
      children.put(newRoot, child);
    }
    children.removeAll(oldRoot);
  }

}
