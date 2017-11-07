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
import static org.junit.Assert.assertNull;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntity;

public class AbstractEntityDaoTest extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityDao dao;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntity").columns("name", "active").values("Foo1", true).values("Foo2", true)
            .values("Foo3", false).build());
  }

  @Test
  public void getAllEntites() {
    final Collection<FooEntity> foos = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(3, foos.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getAllEntites_firstResult_maxResults() {
    final Collection<FooEntity> foos = dao.getEntites(sessionProvider.getStatelessSession(), 1, 1);
    assertEquals(1, foos.size());
    assertEquals("Foo2", foos.iterator().next().getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntitesByProperty_unique() {
    final Collection<FooEntity> foos =
        dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "active", false);
    assertEquals(1, foos.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntitesByProperty_multiple() {
    final Collection<FooEntity> foos =
        dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "active", true);
    assertEquals(2, foos.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntitesByProperty_none() {
    final Collection<FooEntity> foos =
        dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "name", "Foo-1");
    assertEquals(0, foos.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getEntitesByProperty_firstResult_maxResults() {
    final Collection<FooEntity> foos =
        dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "active", true, 1, 1);
    assertEquals(1, foos.size());
    assertEquals("Foo2", foos.iterator().next().getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getUniqueEntityByProperty_unique() {
    final FooEntity foo =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "active", false);
    assertEquals("Foo3", foo.getName());

    dbSetupTracker.skipNextLaunch();
  }

  @Test(expected = Exception.class)
  public void getUniqueEntityByProperty_ambiguous() {
    dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "active", true);
  }

  @Test
  public void getUniqueEntityByProperty_nonExisting() {
    final FooEntity foo =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo-1");
    assertNull(foo);

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void countAll() {
    final long count = dao.countAll(sessionProvider.getStatelessSession());
    assertEquals(3, count);

    dbSetupTracker.skipNextLaunch();
  }

  @Test(expected = Exception.class)
  public void insert_stateless_existing() {
    final FooEntity foo =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    dao.insert(sessionProvider.getStatelessSession(), foo);
  }

  @Test
  public void insert_stateless_new() {
    FooEntity foo = new FooEntity("Foo4", true);
    dao.insert(sessionProvider.getStatelessSession(), foo);
    sessionProvider.closeStatelessSession();

    final List<FooEntity> foos = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(4, foos.size());

    foo = foos.get(foos.size() - 1);
    assertEquals("Foo4", foo.getName());
  }

  @Test
  public void update_stateless_existing() {
    final FooEntity foo =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    foo.setActive(false);
    dao.update(sessionProvider.getStatelessSession(), foo);
    sessionProvider.closeStatelessSession();

    Collection<FooEntity> foos =
        dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "active", true);
    assertEquals(1, foos.size());
    foos = dao.getEntitesByProperty(sessionProvider.getStatelessSession(), "active", false);
    assertEquals(2, foos.size());

  }

  @Test(expected = Exception.class)
  public void update_stateless_new() {
    final FooEntity foo = new FooEntity("Foo4", true);
    dao.update(sessionProvider.getStatelessSession(), foo);
  }

  @Test(expected = Exception.class)
  public void delete_stateless_new() {
    final FooEntity foo = new FooEntity("Foo4", true);
    dao.delete(sessionProvider.getStatelessSession(), foo);
  }

  @Test
  public void delete_stateless_existing() {
    final FooEntity foo =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    dao.delete(sessionProvider.getStatelessSession(), foo);
    sessionProvider.closeStatelessSession();

    final long count = dao.countAll(sessionProvider.getStatelessSession());
    assertEquals(2, count);
  }

}
