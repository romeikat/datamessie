package com.romeikat.datamessie.core.base.query.entity;

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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoBarEntityWithId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery.ReturnMode;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.domain.entity.impl.BarEntityWithId;

public class EntityWithIdQueryTest extends AbstractDbSetupBasedTest {

  @Autowired
  private CollectionUtil collectionUtil;

  @Mock
  private ResultTransformer resultTransformer;

  @Override
  protected Operation initDb() {
    final BarEntityWithId barEntityWithId1 = new BarEntityWithId(1, "BarEntityWithId1", true, 1);
    final BarEntityWithId barEntityWithId2 = new BarEntityWithId(2, "BarEntityWithId2", true, 2);
    final BarEntityWithId barEntityWithId3 = new BarEntityWithId(3, "BarEntityWithId3", false, 3);
    final BarEntityWithId barEntityWithId4 = new BarEntityWithId(4, "BarEntityWithId4", false, 4);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        // Bar entities 1, 2, 3, 4
        insertIntoBarEntityWithId(barEntityWithId1), insertIntoBarEntityWithId(barEntityWithId2),
        insertIntoBarEntityWithId(barEntityWithId3), insertIntoBarEntityWithId(barEntityWithId4));
  }

  @Test
  public void listObjects() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntityWithId1", "BarEntityWithId2",
        "BarEntityWithId3", "BarEntityWithId4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void listIds() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));

    final List<Long> ids = query.listIds(sessionProvider.getStatelessSession());
    final List<Long> expected = Arrays.asList(1l, 2l, 3l, 4l);
    assertTrue(ListUtils.isEqualList(expected, ids));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void listIdsForProperty() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));

    final List<Long> ids = query.listIdsForProperty(sessionProvider.getStatelessSession(), "fooId");
    final List<Long> expected = Arrays.asList(1l, 2l, 3l, 4l);
    assertTrue(ListUtils.isEqualList(expected, ids));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void listForProjection() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));

    @SuppressWarnings("unchecked")
    final List<String> names = (List<String>) query
        .listForProjection(sessionProvider.getStatelessSession(), Projections.property("name"));
    final List<String> expected = Arrays.asList("BarEntityWithId1", "BarEntityWithId2",
        "BarEntityWithId3", "BarEntityWithId4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueObject() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addRestriction(Restrictions.eq("name", "BarEntityWithId1"));

    final BarEntityWithId object = query.uniqueObject(sessionProvider.getStatelessSession());
    assertEquals("BarEntityWithId1", object.getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueIdForProperty() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addRestriction(Restrictions.eq("name", "BarEntityWithId1"));

    final long fooId = query.uniqueIdForProperty(sessionProvider.getStatelessSession(), "fooId");
    assertEquals(1l, fooId);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueId() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addRestriction(Restrictions.eq("name", "BarEntityWithId1"));

    final long fooId = query.uniqueId(sessionProvider.getStatelessSession());
    assertEquals(1l, fooId);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueForProjection() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addRestriction(Restrictions.eq("name", "BarEntityWithId1"));

    final String name = (String) query.uniqueForProjection(sessionProvider.getStatelessSession(),
        Projections.property("name"));
    assertEquals("BarEntityWithId1", name);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void count() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);

    final long count = query.count(sessionProvider.getStatelessSession());
    assertEquals(4, count);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setFirstResult() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));
    query.setFirstResult(1);

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected =
        Arrays.asList("BarEntityWithId2", "BarEntityWithId3", "BarEntityWithId4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setMaxResults() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("name"));
    query.setMaxResults(1);

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntityWithId1");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void addOrders() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addOrder(Order.asc("active"));
    query.addOrder(Order.desc("name"));

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntityWithId4", "BarEntityWithId3",
        "BarEntityWithId2", "BarEntityWithId1");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void addRestrictions() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addRestriction(Restrictions.ge("fooId", 2l));
    query.addRestriction(Restrictions.eq("active", false));
    query.addOrder(Order.asc("name"));

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntityWithId3", "BarEntityWithId4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void addIdRestriction() {
    final int maxId = 10000;
    final List<Long> ids = collectionUtil.createLongList(2, maxId);

    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.addIdRestriction(ids);
    query.addOrder(Order.asc("name"));

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected =
        Arrays.asList("BarEntityWithId2", "BarEntityWithId3", "BarEntityWithId4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setResultTransformer() {
    final BarEntityWithId barEntityWithId5 = new BarEntityWithId(5, "BarEntityWithId5", true, 5);
    doReturn(Lists.newArrayList(barEntityWithId5)).when(resultTransformer).transformList(any());

    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.setResultTransformer(resultTransformer);

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(1, objects.size());
    assertEquals("BarEntityWithId5", objects.iterator().next().getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setReturnModeForEmptyRestrictions() {
    final EntityWithIdQuery<BarEntityWithId> query = new EntityWithIdQuery<>(BarEntityWithId.class);
    query.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);

    final List<BarEntityWithId> objects = query.listObjects(sessionProvider.getStatelessSession());
    assertNull(objects);

    dbSetupTracker.skipNextLaunch();
  }

  private List<String> getNames(final List<BarEntityWithId> objects) {
    return objects.stream().map(o -> o.getName()).collect(Collectors.toList());
  }

}
