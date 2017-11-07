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
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;

public class NamedEntityCategoryDaoTest extends AbstractDbSetupBasedTest {

  @Autowired
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Override
  protected Operation initDb() {
    final NamedEntity namedEntity1 = new NamedEntity(1, "NamedEntity1");
    final NamedEntity namedEntity2 = new NamedEntity(2, "NamedEntity2");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        insertIntoNamedEntity(namedEntity1), insertIntoNamedEntity(namedEntity2));
  }

  @Test
  public void invalidatesCache() {
    Set<String> namedEntityCategoryNames = namedEntityCategoryDao
        .getNamedEntityCategoryNames(sessionProvider.getStatelessSession(), "NamedEntity1");
    assertEquals(0, namedEntityCategoryNames.size());

    final NamedEntityCategory namedEntityCategory = new NamedEntityCategory();
    namedEntityCategory.setNamedEntityId(1l);
    namedEntityCategory.setCategoryNamedEntityId(2l);
    namedEntityCategoryDao.insert(sessionProvider.getStatelessSession(), namedEntityCategory);

    namedEntityCategoryNames = namedEntityCategoryDao
        .getNamedEntityCategoryNames(sessionProvider.getStatelessSession(), "NamedEntity1");
    assertEquals(1, namedEntityCategoryNames.size());
  }

}
