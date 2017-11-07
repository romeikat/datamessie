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

public class EntityWithIdAndVersionSynchronizerTest2 extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityWithGeneratedIdAndVersionDao dao;

  @Autowired
  private ApplicationContext ctx;

  @Mock
  private TaskExecution taskExecution;

  @Override
  protected Operation initDb() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithGeneratedIdAndVersion").columns("id", "version", "name", "active")
            .values(1L, 0L, "Foo1", true).values(2L, 0L, "Foo2", true).build());
  }

  @Override
  protected Operation initDbSyncSource() {
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertInto("fooEntityWithGeneratedIdAndVersion").columns("id", "version", "name", "active")
            .values(1L, 0L, "Foo1", true).values(2L, 0L, "Foo2", true).build());
  }

  @Test
  public void synchronize_switch_names() throws Exception {
    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // LHS
    final FooEntityWithGeneratedIdAndVersion lhs1 =
        dao.getEntity(syncSourceSessionProvider.getStatelessSession(), 1);
    assertEquals("Foo1", lhs1.getName());
    final FooEntityWithGeneratedIdAndVersion lhs2 =
        dao.getEntity(syncSourceSessionProvider.getStatelessSession(), 2);
    assertEquals("Foo2", lhs2.getName());

    // RHS
    FooEntityWithGeneratedIdAndVersion rhs1 =
        dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals("Foo1", rhs1.getName());
    FooEntityWithGeneratedIdAndVersion rhs2 =
        dao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals("Foo2", rhs2.getName());

    // LHS: switch names
    lhs2.setName("Temporary name");
    dao.update(syncSourceSessionProvider.getStatelessSession(), lhs2);
    lhs1.setName("Foo2");
    dao.update(syncSourceSessionProvider.getStatelessSession(), lhs1);
    lhs2.setName("Foo1");
    dao.update(syncSourceSessionProvider.getStatelessSession(), lhs2);

    new FooEntityWithGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS: names are also switched
    rhs1 = dao.getEntity(sessionProvider.getStatelessSession(), 1);
    assertEquals("Foo2", rhs1.getName());
    rhs2 = dao.getEntity(sessionProvider.getStatelessSession(), 2);
    assertEquals("Foo1", rhs2.getName());
  }

}
