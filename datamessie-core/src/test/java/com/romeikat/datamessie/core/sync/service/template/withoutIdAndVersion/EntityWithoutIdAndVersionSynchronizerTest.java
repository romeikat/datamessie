package com.romeikat.datamessie.core.sync.service.template.withoutIdAndVersion;

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

import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.FooEntityWithoutGeneratedIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithoutIdAndVersion;

public class EntityWithoutIdAndVersionSynchronizerTest extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityWithoutGeneratedIdAndVersionDao dao;

  @Autowired
  private ApplicationContext ctx;

  @Mock
  private TaskExecution taskExecution;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithoutIdAndVersion").columns("name").values("Foo1").build());
  }

  @Override
  protected Operation initDbSyncSource() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithoutIdAndVersion").columns("name").values("Foo1").build());
  }

  @Test
  public void synchronize_no_changes() throws Exception {
    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // LHS
    final Collection<FooEntityWithoutIdAndVersion> lhs =
        dao.getAllEntites(syncSourceSessionProvider.getStatelessSession());
    assertEquals(1, lhs.size());

    final FooEntityWithoutIdAndVersion lhs1 = lhs.iterator().next();
    assertEquals("Foo1", lhs1.getName());

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(1, rhs.size());

    final FooEntityWithoutIdAndVersion rhs1 =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    assertEquals("Foo1", rhs1.getName());
  }

  @Test
  public void sync_create() throws Exception {
    // LHS
    final FooEntityWithoutIdAndVersion lhs2 = new FooEntityWithoutIdAndVersion("Foo2");
    dao.insert(syncSourceSessionProvider.getStatelessSession(), lhs2);

    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(2, rhs.size());

    final FooEntityWithoutIdAndVersion rhs2 =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo2");
    assertEquals(lhs2.getName(), rhs2.getName());
  }

  @Test
  public void sync_delete() throws Exception {
    // LHS
    final FooEntityWithoutIdAndVersion lhs1 =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    dao.delete(syncSourceSessionProvider.getStatelessSession(), lhs1);

    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(0, rhs.size());

    final FooEntityWithoutIdAndVersion rhs1 =
        dao.getUniqueEntityByProperty(sessionProvider.getStatelessSession(), "name", "Foo1");
    assertNull(rhs1);
  }

}
