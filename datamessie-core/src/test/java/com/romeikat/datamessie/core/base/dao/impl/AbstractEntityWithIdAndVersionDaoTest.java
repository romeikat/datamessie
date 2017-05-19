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

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;

public class AbstractEntityWithIdAndVersionDaoTest extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityWithGeneratedIdAndVersionDao dao;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithGeneratedIdAndVersion").columns("id", "version", "name", "active")
            .values(1L, 0l, "Foo1", true).values(2L, 0l, "Foo2", true).values(3L, 0l, "Foo3", false).build());
  }

  @Test
  public void getIdsWithVersion_ids() {
    Collection<Long> ids = Lists.newArrayList();
    Map<Long, Long> idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), ids);
    assertEquals(0, idsWithVersion.size());
    assertTrue(CollectionUtils.isEqualCollection(ids, idsWithVersion.keySet()));

    ids = Lists.newArrayList(1l, 2l, 3l);
    idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), ids);
    assertEquals(3, idsWithVersion.size());
    assertTrue(CollectionUtils.isEqualCollection(ids, idsWithVersion.keySet()));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIdsWithVersion_firstResult_maxResults() {
    Map<Long, Long> idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), 0, 1);
    Collection<Long> expected = Arrays.asList(1l);
    assertTrue(CollectionUtils.isEqualCollection(expected, idsWithVersion.keySet()));

    idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), 1, 2);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, idsWithVersion.keySet()));

    idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), 1, 0);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, idsWithVersion.keySet()));

    idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), 1, 3);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, idsWithVersion.keySet()));

    idsWithVersion = dao.getIdsWithVersion(sessionProvider.getStatelessSession(), 5, 0);
    expected = Arrays.asList();
    assertTrue(CollectionUtils.isEqualCollection(expected, idsWithVersion.keySet()));

    dbSetupTracker.skipNextLaunch();
  }

}
