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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoNamedEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.domain.entity.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityImpl;
import jersey.repackaged.com.google.common.collect.Lists;

public class NamedEntityDaoTest extends AbstractDbSetupBasedTest {

  @Autowired
  private NamedEntityDao namedEntityDao;

  @Override
  protected Operation initDb() {
    final NamedEntity namedEntity1 = new NamedEntityImpl(1, "NamedEntity1");
    final NamedEntity namedEntity2 = new NamedEntityImpl(2, "NamedEntity2");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertIntoNamedEntity(namedEntity1), insertIntoNamedEntity(namedEntity2));
  }

  @Test
  public void getOrCreateNamedEntities_existing() {
    final long namedEntityId = namedEntityDao
        .getOrCreate(sessionProvider.getStatelessSession(), Lists.newArrayList("NamedEntity1"))
        .values().iterator().next();
    final NamedEntity namedEntity =
        namedEntityDao.getEntity(sessionProvider.getStatelessSession(), namedEntityId);
    assertEquals("NamedEntity1", namedEntity.getName());

    final List<NamedEntity> all =
        namedEntityDao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(2, all.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getOrCreateNamedEntities_new() {
    final long namedEntityId = namedEntityDao
        .getOrCreate(sessionProvider.getStatelessSession(), Lists.newArrayList("NamedEntity-1"))
        .values().iterator().next();
    final NamedEntity namedEntity =
        namedEntityDao.getEntity(sessionProvider.getStatelessSession(), namedEntityId);
    assertEquals("NamedEntity-1", namedEntity.getName());

    final List<NamedEntity> all =
        namedEntityDao.getAllEntites(sessionProvider.getStatelessSession());
    assertEquals(3, all.size());
  }

  @Test
  public void invalidatesCache() {
    final NamedEntity namedEntity =
        namedEntityDao.get(sessionProvider.getStatelessSession(), "NamedEntity3");
    assertNull(namedEntity);

    NamedEntity namedEntity3 = new NamedEntityImpl();
    namedEntity3.setName("NamedEntity3");
    namedEntityDao.insert(sessionProvider.getStatelessSession(), namedEntity3);

    namedEntity3 = namedEntityDao.get(sessionProvider.getStatelessSession(), "NamedEntity3");
    assertNotNull(namedEntity3);
  }

}
