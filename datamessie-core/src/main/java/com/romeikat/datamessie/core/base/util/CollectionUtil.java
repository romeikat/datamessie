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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.util.Function.InvalidValueException;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.EntityWithId;
import jersey.repackaged.com.google.common.collect.Sets;

@Service
public class CollectionUtil {

  public <T> Set<T> getOthers(final Collection<T> objects, final T object) {
    final Set<T> others = Sets.newHashSet(objects);
    others.remove(object);
    return others;
  }

  public List<Integer> createIntegerList(Integer min, final int max) {
    final List<Integer> result = Lists.newArrayListWithExpectedSize(max);
    if (min == null) {
      min = 0;
    }
    for (int i = 0; i < max; i++) {
      result.add(i);
    }
    return result;
  }

  public List<Long> createLongList(Integer min, final int max) {
    final List<Long> result = Lists.newArrayListWithExpectedSize(max);
    if (min == null) {
      min = 0;
    }
    for (long l = min; l < max; l++) {
      result.add(l);
    }
    return result;
  }

  public <T, U> Collection<Pair<T, U>> getPairs(final Collection<T> elements1,
      final Collection<U> elements2) {
    final List<Pair<T, U>> pairs = new LinkedList<Pair<T, U>>();
    for (final T element1 : elements1) {
      for (final U element2 : elements2) {
        pairs.add(new ImmutablePair<T, U>(element1, element2));
      }
    }
    return pairs;
  }

  public <T> Collection<Pair<T, T>> getPairsAsymmetric(final Collection<T> elements) {
    return getPairsAsymmetric(elements, false);
  }

  public <T> Collection<Pair<T, T>> getPairsAsymmetric(final Collection<T> elements,
      final boolean addSelfPairs) {
    final List<T> elementsList = new ArrayList<T>(elements);
    final List<Pair<T, T>> pairs = new LinkedList<Pair<T, T>>();
    final int numberOfElements = elementsList.size();
    if (elementsList == null || numberOfElements < 1) {
      return pairs;
    }
    for (int i = 0; i < numberOfElements; i++) {
      final T element1 = elementsList.get(i);
      if (addSelfPairs) {
        pairs.add(new ImmutablePair<T, T>(element1, element1));
      }
      for (int j = i + 1; j < numberOfElements; j++) {
        final T element2 = elementsList.get(j);
        pairs.add(new ImmutablePair<T, T>(element1, element2));
      }
    }
    return pairs;
  }

  public <T> Collection<Pair<T, T>> getPairsSymmetric(final Collection<T> elements) {
    return getPairsSymmetric(elements, false);
  }

  public <T> Collection<Pair<T, T>> getPairsSymmetric(final Collection<T> elements,
      final boolean addSelfPairs) {
    final List<T> elementsList = new ArrayList<T>(elements);
    final List<Pair<T, T>> pairs = new LinkedList<Pair<T, T>>();
    final int numberOfElements = elementsList.size();
    if (elementsList == null || numberOfElements < 1) {
      return pairs;
    }
    for (int i = 0; i < numberOfElements; i++) {
      final T element1 = elementsList.get(i);
      if (addSelfPairs) {
        pairs.add(new ImmutablePair<T, T>(element1, element1));
      }
      for (int j = 0; j < numberOfElements; j++) {
        if (i != j) {
          final T element2 = elementsList.get(j);
          pairs.add(new ImmutablePair<T, T>(element1, element2));
        }
      }
    }
    return pairs;
  }

  public <T> List<List<T>> fromNChooseK(final List<T> elements, final int k) {
    // Generate permutations
    final List<List<T>> permutations = powerSet(elements, k);
    // Done
    return permutations;
  }

  public <T> List<List<T>> powerSet(final List<T> elements) {
    return powerSet(elements, null);
  }

  public <T> List<List<T>> powerSet(final List<T> elements, final Integer sizeOfSets) {
    final List<List<T>> powerSet = new LinkedList<List<T>>();
    List<List<T>> toBeProcessed = new LinkedList<List<T>>();
    // Start with the empty set
    final List<T> emptySet = new ArrayList<T>();
    toBeProcessed.add(emptySet);
    if (sizeOfSets == null || sizeOfSets == 0) {
      powerSet.add(emptySet);
    }
    // Add remaining sets
    for (final T element : elements) {
      toBeProcessed = extend(toBeProcessed, element, sizeOfSets, new PowerSetCollector<T>() {
        @Override
        public void collectFinalSet(final List<T> set) {
          powerSet.add(set);
        }
      });
    }
    // Done
    return powerSet;
  }

  private <T> List<List<T>> extend(final List<List<T>> toBeProcessed, final T element,
      final Integer sizeOfSets, final PowerSetCollector<T> powerSetCollector) {
    final List<List<T>> toBeProcessedAfterExtension =
        new ArrayList<List<T>>(2 * toBeProcessed.size());
    // Add previous sets
    toBeProcessedAfterExtension.addAll(toBeProcessed);
    // Add element to all previous sets
    for (final List<T> set : toBeProcessed) {
      final List<T> extendedSet = new ArrayList<T>(set.size() + 1);
      extendedSet.addAll(set);
      extendedSet.add(element);
      if (sizeOfSets == null || extendedSet.size() == sizeOfSets) {
        powerSetCollector.collectFinalSet(extendedSet);
      }
      if (sizeOfSets == null || extendedSet.size() < sizeOfSets) {
        toBeProcessedAfterExtension.add(extendedSet);
      }
    }
    // Done
    return toBeProcessedAfterExtension;
  }

  private interface PowerSetCollector<T> {

    void collectFinalSet(List<T> set);

  }

  public <K, V extends Comparable<? super V>> K determineIndexWithMaxValue(final Map<K, V> map) {
    K indexOfMaxValue = null;
    V maxValue = null;
    for (final K key : map.keySet()) {
      final V value = map.get(key);
      if (maxValue == null || value.compareTo(maxValue) > 0) {
        indexOfMaxValue = key;
        maxValue = value;
      }
    } ;
    return indexOfMaxValue;
  }

  public <T extends Comparable<? super T>> List<T> getCommonElementsSorted(
      final Collection<T> elements1, final Collection<T> elements2) {
    final List<T> commonElements = new ArrayList<T>(elements1);
    commonElements.retainAll(elements2);
    Collections.sort(commonElements);
    return commonElements;
  }

  public <E1, E2> Set<E2> transformSet(final Set<E1> set, final Function<E1, E2> transformation) {
    final Set<E2> transformedSet = new HashSet<E2>(set.size());
    for (final E1 element1 : set) {
      try {
        final E2 element2 = transformation.apply(element1);
        transformedSet.add(element2);
      } catch (final InvalidValueException e) {
      }
    }
    return transformedSet;
  }

  public <K, V1, V2> ConcurrentMap<K, V2> transformMap(final Map<K, V1> map,
      final Function<V1, V2> transformation) {
    final List<K> indexes = new ArrayList<K>(map.keySet());
    final ConcurrentMap<K, V2> transformedMap = new ConcurrentHashMap<K, V2>();
    new ParallelProcessing<K>(null, indexes) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider, final K index) {
        final V1 value1 = map.get(index);
        final V2 value2;
        try {
          value2 = transformation.apply(value1);
        } catch (final InvalidValueException e) {
          return;
        }
        transformedMap.put(index, value2);
      }
    };
    return transformedMap;
  }

  public <K, V> List<LinkedHashMap<K, V>> partitionMap(final Map<K, V> map, final int size) {
    final List<LinkedHashMap<K, V>> result = Lists.newArrayListWithExpectedSize(map.size() / size);

    final Collection<List<K>> keyPartitions =
        Lists.partition(Lists.newArrayList(map.keySet()), size);
    for (final List<K> keyPartition : keyPartitions) {
      final LinkedHashMap<K, V> partition =
          Maps.newLinkedHashMapWithExpectedSize(keyPartition.size());
      for (final K key : keyPartition) {
        partition.put(key, map.get(key));
      }
      result.add(partition);
    }
    return result;
  }

  public static <T> List<List<T>> splitIntoSubListsBySize(final List<T> list,
      final int sizeOfSublists) {
    final List<List<T>> subLists = new ArrayList<List<T>>();
    // Only one sublist
    if (sizeOfSublists < 1) {
      subLists.add(list);
      return subLists;
    }
    // Divide
    for (int i = 0;; i++) {
      final int fromIndex = i * sizeOfSublists;
      int toIndex = (i + 1) * sizeOfSublists;
      final boolean lastSublist = list.size() <= toIndex;
      if (lastSublist) {
        toIndex = list.size();
      }
      final List<T> subList = new ArrayList<T>(list.subList(fromIndex, toIndex));
      subLists.add(subList);
      // No more sublists to go
      if (lastSublist) {
        break;
      }
    }
    // Done
    return subLists;
  }

  public static <T> List<List<T>> splitIntoSubListsByNumber(final List<T> list,
      final int numberOfSublists) {
    final int sizeOfSublists = (int) Math.ceil((double) list.size() / (double) numberOfSublists);
    return splitIntoSubListsBySize(list, sizeOfSublists);
  }

  public String formatNumberWithLeadingZeroes(final int number, final int maxNumber) {
    final int numberOfDigits = String.valueOf(maxNumber).length();
    final String stringFormat = "%0" + numberOfDigits + "d";
    final String formattedNumber = String.format(stringFormat, number);
    return formattedNumber;
  }

  public SortedMap<String, Double> getFirstItems(final SortedMap<String, Double> map,
      final int numberOfItems) {
    if (numberOfItems <= 0) {
      return Collections.emptySortedMap();
    }

    // Limit to first N terms
    String lastTerm = null;
    int i = 0;
    for (final String term : map.keySet()) {
      if (i == numberOfItems) {
        break;
      }

      lastTerm = term;
      i++;
    }

    return map.headMap(lastTerm);
  }

  public Set<Long> getIds(final Collection<? extends EntityWithId> entities) {
    return entities.stream().map(e -> e.getId()).collect(Collectors.toSet());
  }

}
