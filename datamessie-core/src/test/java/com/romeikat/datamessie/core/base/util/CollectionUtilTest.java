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

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.AbstractTest;

public class CollectionUtilTest extends AbstractTest {

  @Autowired
  private CollectionUtil collectionUtil;

  @Test
  public void calculatesPowerSet1() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> powerSet = collectionUtil.powerSet(elements);
    assertEquals(8, powerSet.size());
  }

  @Test
  public void calculatesPowerSet2() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> powerSet = collectionUtil.powerSet(elements, 1);
    assertEquals(3, powerSet.size());
  }

  @Test
  public void splitsIntoSubListsByNumber1() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsByNumber(elements, 3);
    assertEquals(3, subLists.size());
    assertEquals(Lists.newArrayList(1), subLists.get(0));
    assertEquals(Lists.newArrayList(2), subLists.get(1));
    assertEquals(Lists.newArrayList(3), subLists.get(2));
  }

  @Test
  public void splitsIntoSubListsByNumber2() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsByNumber(elements, 1);
    assertEquals(1, subLists.size());
    assertEquals(elements, subLists.get(0));
  }

  @Test
  public void splitsIntoSubListsByNumber3() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsByNumber(elements, 2);
    assertEquals(2, subLists.size());
    assertEquals(Lists.newArrayList(1, 2), subLists.get(0));
    assertEquals(Lists.newArrayList(3), subLists.get(1));
  }

  @Test
  public void splitsIntoSubListsBySize1() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsBySize(elements, 3);
    assertEquals(1, subLists.size());
    assertEquals(elements, subLists.get(0));
  }

  @Test
  public void splitsIntoSubListsBySize2() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsBySize(elements, 1);
    assertEquals(3, subLists.size());
    assertEquals(Lists.newArrayList(1), subLists.get(0));
    assertEquals(Lists.newArrayList(2), subLists.get(1));
    assertEquals(Lists.newArrayList(3), subLists.get(2));
  }

  @Test
  public void splitsIntoSubListsBySize3() throws Exception {
    final List<Integer> elements = Lists.newArrayList(1, 2, 3);
    final List<List<Integer>> subLists = CollectionUtil.splitIntoSubListsBySize(elements, 2);
    assertEquals(2, subLists.size());
    assertEquals(Lists.newArrayList(1, 2), subLists.get(0));
    assertEquals(Lists.newArrayList(3), subLists.get(1));
  }

}
