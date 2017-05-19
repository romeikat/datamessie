package com.romeikat.datamessie.core.sync.service.template.withIdAndVersion;

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
import com.romeikat.datamessie.core.base.dao.impl.FooEntityWithGeneratedIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithGeneratedIdAndVersion;

public class EntityWithIdAndVersionSynchronizerTest extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityWithGeneratedIdAndVersionDao dao;

  @Autowired
  private ApplicationContext ctx;

  @Mock
  private TaskExecution taskExecution;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, insertInto("fooEntityWithGeneratedIdAndVersion")
        .columns("id", "version", "name", "active").values(1L, 0L, "Foo1", true).build());
  }

  @Override
  protected Operation initDbSyncSource() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, insertInto("fooEntityWithGeneratedIdAndVersion")
        .columns("id", "version", "name", "active").values(1L, 0L, "Foo1", true).build());
  }

  @Test
  public void synchronize_no_changes() throws Exception {
    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // LHS
    final Collection<FooEntityWithGeneratedIdAndVersion> lhs =
        dao.getAllEntites(syncSourceSessionProvider.getStatelessSession());
    assertEquals(1, lhs.size());

    final FooEntityWithGeneratedIdAndVersion lhs1 = lhs.iterator().next();
    assertEquals(1, lhs1.getId());
    assertEquals(0, lhs1.getVersion().longValue());
    assertEquals("Foo1", lhs1.getName());

    // RHS
    final Collection<FooEntityWithGeneratedIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(1, rhs.size());

    final FooEntityWithGeneratedIdAndVersion rhs1 = rhs.iterator().next();
    assertEquals(1, rhs1.getId());
    assertEquals(0, rhs1.getVersion().longValue());
    assertEquals("Foo1", rhs1.getName());
  }

  @Test
  public void sync_create() throws Exception {
    // Create and delete on LHS to create a skipped ID on the LHS
    final FooEntityWithGeneratedIdAndVersion lhs2 = new FooEntityWithGeneratedIdAndVersion();
    lhs2.setName("Foo2");
    dao.insert(syncSourceSessionProvider.getStatelessSession(), lhs2);
    dao.delete(syncSourceSessionProvider.getStatelessSession(), lhs2);

    // LHS
    final FooEntityWithGeneratedIdAndVersion lhs3 = new FooEntityWithGeneratedIdAndVersion();
    lhs3.setName("Foo3");
    dao.insert(syncSourceSessionProvider.getStatelessSession(), lhs3);

    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithGeneratedIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(2, rhs.size());

    final FooEntityWithGeneratedIdAndVersion rhs3 = dao.getEntity(sessionProvider.getStatelessSession(), lhs3.getId());
    assertEquals(lhs3.getId(), rhs3.getId());
    assertEquals(lhs3.getVersion().longValue(), rhs3.getVersion().longValue());
    assertEquals(lhs3.getName(), rhs3.getName());
  }

  @Test
  public void sync_update() throws Exception {
    // LHS
    final FooEntityWithGeneratedIdAndVersion lhs1 = dao.getEntity(syncSourceSessionProvider.getStatelessSession(), 1);
    lhs1.setName("Foo1 updated");
    dao.update(syncSourceSessionProvider.getStatelessSession(), lhs1);
    assertEquals(1, lhs1.getVersion().longValue());
    lhs1.setName("Foo1 updated again");
    dao.update(syncSourceSessionProvider.getStatelessSession(), lhs1);
    assertEquals(2, lhs1.getVersion().longValue());

    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithGeneratedIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(1, rhs.size());

    final FooEntityWithGeneratedIdAndVersion rhs1 = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals(lhs1.getId(), rhs1.getId());
    assertEquals(lhs1.getVersion(), rhs1.getVersion());
    assertEquals(lhs1.getName(), rhs1.getName());
  }

  @Test
  public void sync_delete() throws Exception {
    // LHS
    final FooEntityWithGeneratedIdAndVersion lhs1 = dao.getEntity(syncSourceSessionProvider.getStatelessSession(), 1);
    dao.delete(syncSourceSessionProvider.getStatelessSession(), lhs1);

    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithGeneratedIdAndVersion> rhs = dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(0, rhs.size());

    final FooEntityWithGeneratedIdAndVersion rhs1 = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertNull(rhs1);
  }

}
