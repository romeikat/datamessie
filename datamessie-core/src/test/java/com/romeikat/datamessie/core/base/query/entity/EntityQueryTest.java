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
import static com.romeikat.datamessie.core.CommonOperations.insertIntoBarEntity;
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
import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery.ReturnMode;
import com.romeikat.datamessie.core.domain.entity.impl.BarEntity;

public class EntityQueryTest extends AbstractDbSetupBasedTest {

  @Mock
  private ResultTransformer resultTransformer;

  @Override
  protected Operation initDb() {
    final BarEntity barEntity1 = new BarEntity("BarEntity1", true, 1);
    final BarEntity barEntity2 = new BarEntity("BarEntity2", true, 2);
    final BarEntity barEntity3 = new BarEntity("BarEntity3", false, 3);
    final BarEntity barEntity4 = new BarEntity("BarEntity4", false, 4);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        // Bar entities 1, 2, 3, 4
        insertIntoBarEntity(barEntity1), insertIntoBarEntity(barEntity2),
        insertIntoBarEntity(barEntity3), insertIntoBarEntity(barEntity4));
  }

  @Test
  public void listObjects() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("name"));

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected =
        Arrays.asList("BarEntity1", "BarEntity2", "BarEntity3", "BarEntity4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void listIdsForProperty() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("name"));

    final List<Long> ids = query.listIdsForProperty(sessionProvider.getStatelessSession(), "fooId");
    final List<Long> expected = Arrays.asList(1l, 2l, 3l, 4l);
    assertTrue(ListUtils.isEqualList(expected, ids));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void listForProjection() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("name"));

    @SuppressWarnings("unchecked")
    final List<String> names = (List<String>) query
        .listForProjection(sessionProvider.getStatelessSession(), Projections.property("name"));
    final List<String> expected =
        Arrays.asList("BarEntity1", "BarEntity2", "BarEntity3", "BarEntity4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueObject() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addRestriction(Restrictions.eq("name", "BarEntity1"));

    final BarEntity object = query.uniqueObject(sessionProvider.getStatelessSession());
    assertEquals("BarEntity1", object.getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueIdForProperty() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addRestriction(Restrictions.eq("name", "BarEntity1"));

    final long fooId = query.uniqueIdForProperty(sessionProvider.getStatelessSession(), "fooId");
    assertEquals(1l, fooId);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void uniqueForProjection() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addRestriction(Restrictions.eq("name", "BarEntity1"));

    final String name = (String) query.uniqueForProjection(sessionProvider.getStatelessSession(),
        Projections.property("name"));
    assertEquals("BarEntity1", name);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void count() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);

    final long count = query.count(sessionProvider.getStatelessSession(), "name");
    assertEquals(4, count);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setFirstResult() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("name"));
    query.setFirstResult(1);

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntity2", "BarEntity3", "BarEntity4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setMaxResults() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("name"));
    query.setMaxResults(1);

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntity1");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void addOrders() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addOrder(Order.asc("active"));
    query.addOrder(Order.desc("name"));

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected =
        Arrays.asList("BarEntity4", "BarEntity3", "BarEntity2", "BarEntity1");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void addRestrictions() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.addRestriction(Restrictions.ge("fooId", 2l));
    query.addRestriction(Restrictions.eq("active", false));

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    final List<String> names = getNames(objects);
    final List<String> expected = Arrays.asList("BarEntity3", "BarEntity4");
    assertTrue(ListUtils.isEqualList(expected, names));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setResultTransformer() {
    final BarEntity barEntity5 = new BarEntity("BarEntity5", true, 5);
    doReturn(Lists.newArrayList(barEntity5)).when(resultTransformer).transformList(any());

    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.setResultTransformer(resultTransformer);

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    assertEquals(1, objects.size());
    assertEquals("BarEntity5", objects.iterator().next().getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void setReturnModeForEmptyRestrictions() {
    final EntityQuery<BarEntity> query = new EntityQuery<>(BarEntity.class);
    query.setReturnModeForEmptyRestrictions(ReturnMode.RETURN_NULL);

    final List<BarEntity> objects = query.listObjects(sessionProvider.getStatelessSession());
    assertNull(objects);

    dbSetupTracker.skipNextLaunch();
  }

  private List<String> getNames(final List<BarEntity> objects) {
    return objects.stream().map(o -> o.getName()).collect(Collectors.toList());
  }

}
