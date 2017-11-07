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
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.FooEntityWithoutGeneratedIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithoutIdAndVersion;

public class EntityWithoutIdAndVersionSynchronizerStressTest extends AbstractDbSetupBasedTest {

  @Autowired
  private FooEntityWithoutGeneratedIdAndVersionDao dao;

  @Autowired
  private ApplicationContext ctx;

  @Mock
  private TaskExecution taskExecution;

  private static final int NUMBER_OF_ENTITIES = 1234;

  private static List<String> getNames() {
    final List<String> names = Lists.newArrayListWithExpectedSize(NUMBER_OF_ENTITIES);
    for (long id = 1; id <= NUMBER_OF_ENTITIES; id++) {
      names.add("Foo" + id);
    }
    return names;
  }

  @Override
  protected Operation initDb() {
    final List<Operation> operations = Lists.newArrayListWithExpectedSize(NUMBER_OF_ENTITIES);
    for (final String name : getNames()) {
      operations
          .add(insertInto("fooEntityWithoutIdAndVersion").columns("name").values(name).build());
    }
    final Operation initDb = sequenceOf(operations);

    // Done
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, initDb);
  }

  @Override
  protected Operation initDbSyncSource() {
    final List<Operation> operations = Lists.newArrayListWithExpectedSize(NUMBER_OF_ENTITIES);
    for (final String name : getNames()) {
      operations
          .add(insertInto("fooEntityWithoutIdAndVersion").columns("name").values(name).build());
    }
    final Operation initDb = sequenceOf(operations);

    // Done
    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, initDb);
  }

  @Test
  public void synchronize_no_changes() throws Exception {
    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // LHS
    final Collection<FooEntityWithoutIdAndVersion> lhs =
        dao.getAllEntites(syncSourceSessionProvider.getStatelessSession());
    assertEquals(NUMBER_OF_ENTITIES, lhs.size());

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs =
        dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(NUMBER_OF_ENTITIES, rhs.size());
  }

  @Test
  public void sync_create() throws Exception {
    // LHS
    for (int i = 1; i <= NUMBER_OF_ENTITIES; i++) {
      final FooEntityWithoutIdAndVersion lhsEntity =
          new FooEntityWithoutIdAndVersion("New Foo" + i);
      dao.insert(syncSourceSessionProvider.getStatelessSession(), lhsEntity);
    }

    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs =
        dao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(2 * NUMBER_OF_ENTITIES, rhs.size());
  }

  @Test
  public void sync_delete() throws Exception {
    // LHS
    for (final String name : getNames()) {
      final FooEntityWithoutIdAndVersion lhsEntity = dao
          .getUniqueEntityByProperty(syncSourceSessionProvider.getStatelessSession(), "name", name);
      dao.delete(syncSourceSessionProvider.getStatelessSession(), lhsEntity);
    }

    new FooEntityWithoutGeneratedIdAndVersionSynchronizer(ctx).synchronize(taskExecution);

    // RHS
    final Collection<FooEntityWithoutIdAndVersion> rhs =
        dao.getAllEntites(sessionProvider.getStatelessSession());
    assertTrue(rhs.isEmpty());
  }

}
