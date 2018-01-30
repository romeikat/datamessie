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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithId2;
import jersey.repackaged.com.google.common.collect.Lists;

public class AbstractEntityWithId2DaoTest extends AbstractDbSetupBasedTest {

  private static final long NEW_ID = 999l;

  @Autowired
  private FooEntityWithId2Dao dao;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithId2").columns("id2", "name", "active").values(1L, "Foo1", true)
            .values(2L, "Foo2", true).values(3L, "Foo3", false).build());
  }

  @Test
  public void getEntity_existing() {
    final FooEntityWithId2 foo = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(1l, foo.getId());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntity_nonExisting() {
    final FooEntityWithId2 foo = dao.getEntity(sessionProvider.getStatelessSession(), -1);
    assertNull(foo);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntities() {
    final Collection<Long> ids = Lists.newArrayList(1l, 2l, 3l, NEW_ID);
    final Collection<FooEntityWithId2> foos =
        dao.getEntities(sessionProvider.getStatelessSession(), ids);
    assertEquals(3, foos.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIdsWithEntities() {
    final Collection<Long> ids = Lists.newArrayList(1l, 2l, 3l, NEW_ID);
    final Map<Long, FooEntityWithId2> idsWithFoos =
        dao.getIdsWithEntities(sessionProvider.getStatelessSession(), ids);
    assertEquals(3, idsWithFoos.size());
    for (long id = 1; id <= 3l; id++) {
      final FooEntityWithId2 foo = idsWithFoos.get(id);
      assertEquals(id, foo.getId());
    }

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIds() {
    final Collection<Long> ids = dao.getIds(sessionProvider.getStatelessSession());
    final Collection<Long> expected = Arrays.asList(1l, 2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIds_firstResult_maxResults() {
    Collection<Long> ids = dao.getIds(sessionProvider.getStatelessSession(), null, null);
    Collection<Long> expected = Arrays.asList(1l, 2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 0l, 1);
    expected = Arrays.asList(1l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 1l, 1);
    expected = Arrays.asList(1l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 2l, 2);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 2l, 0);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 2l, 3);
    expected = Arrays.asList(2l, 3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    ids = dao.getIds(sessionProvider.getStatelessSession(), 5l, 0);
    expected = Arrays.asList();
    assertTrue(CollectionUtils.isEqualCollection(expected, ids));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIds_ids() {
    Collection<Long> ids = Arrays.asList();
    Collection<Long> ids2 = dao.getIds(sessionProvider.getStatelessSession(), ids);
    Collection<Long> expected = ids;
    assertTrue(CollectionUtils.isEqualCollection(expected, ids2));

    ids = Arrays.asList(1l, 2l, 3l);
    ids2 = dao.getIds(sessionProvider.getStatelessSession(), ids);
    expected = ids;
    assertTrue(CollectionUtils.isEqualCollection(expected, ids2));

    ids = Arrays.asList(3l, 4l);
    ids2 = dao.getIds(sessionProvider.getStatelessSession(), ids);
    expected = Arrays.asList(3l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids2));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getIds_ids_firstResult_maxResults() {
    Collection<Long> ids = Arrays.asList();
    Collection<Long> ids2 = dao.getIds(sessionProvider.getStatelessSession(), ids);
    Collection<Long> expected = ids;
    assertTrue(CollectionUtils.isEqualCollection(expected, ids2));

    ids = Arrays.asList(1l, 2l, 3l);
    ids2 = dao.getIds(sessionProvider.getStatelessSession(), ids, 2l, 1);
    expected = Arrays.asList(2l);
    assertTrue(CollectionUtils.isEqualCollection(expected, ids2));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getMaxId() {
    final Long maxId = dao.getMaxId(sessionProvider.getStatelessSession());
    assertEquals(3l, maxId.longValue());

    dbSetupTracker.skipNextLaunch();
  }

  @Test(expected = Exception.class)
  public void insert_stateless_with_existing_id() {
    final FooEntityWithId2 foo = new FooEntityWithId2(1);
    dao.insert(sessionProvider.getStatelessSession(), foo);
  }

  @Test
  public void insert_stateless_with_new_id() {
    FooEntityWithId2 foo = new FooEntityWithId2(NEW_ID);
    dao.insert(sessionProvider.getStatelessSession(), foo);
    assertEquals(NEW_ID, foo.getId());
    sessionProvider.closeStatelessSession();

    final Collection<FooEntityWithId2> foos =
        dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(4, foos.size());
    foo = dao.getEntity(sessionProvider.getStatelessSession(), NEW_ID);
    assertNotNull(foo);
  }

  @Test
  public void update_stateless_with_existing_id() {
    FooEntityWithId2 foo = new FooEntityWithId2(1);
    foo.setName("Updated Foo1");
    dao.update(sessionProvider.getStatelessSession(), foo);
    sessionProvider.closeStatelessSession();

    foo = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals("Updated Foo1", foo.getName());
  }

  @Test(expected = Exception.class)
  public void update_stateless_with_new_id() {
    final FooEntityWithId2 foo = new FooEntityWithId2(NEW_ID);
    dao.update(sessionProvider.getStatelessSession(), foo);
  }

  @Test(expected = Exception.class)
  public void delete_stateless_with_new_id() {
    final FooEntityWithId2 foo = new FooEntityWithId2(NEW_ID);
    dao.delete(sessionProvider.getStatelessSession(), foo);
  }

  @Test
  public void delete_stateless_with_existing_id() {
    final FooEntityWithId2 foo = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    dao.delete(sessionProvider.getStatelessSession(), foo);
    sessionProvider.closeStatelessSession();

    final long count = dao.countAll(sessionProvider.getStatelessSession());
    assertEquals(2, count);
  }

}
