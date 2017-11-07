package com.romeikat.datamessie.core;

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

import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.processing.init.DatamessieIndexingInitializer;

public abstract class AbstractDbSetupBasedTest extends AbstractTest {

  protected static DbSetupTracker dbSetupTracker = new DbSetupTracker();

  protected HibernateSessionProvider sessionProvider;
  protected HibernateSessionProvider syncSourceSessionProvider;

  @Autowired
  protected DatamessieIndexingInitializer datamessieIndexingInitializer;

  @Autowired
  private NamedEntityDao namedEntityDao;

  @Autowired
  private NamedEntityCategoryDao namedEntityCategoryDao;

  @Autowired
  protected DataSource dataSource;

  @Autowired
  protected DataSource dataSourceSyncSource;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private SessionFactory sessionFactorySyncSource;

  @BeforeClass
  public static void beforeClass() {
    dbSetupTracker = new DbSetupTracker();
  }

  @Override
  @Before
  public void before() throws Exception {
    super.before();

    // DB
    Operation operation = initDb();
    if (operation == null) {
      operation = NopOperation.INSTANCE;
    }
    final DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);
    dbSetup.launch();

    // DB (sync source)
    Operation operationSyncSource = initDbSyncSource();
    if (operationSyncSource == null) {
      operationSyncSource = NopOperation.INSTANCE;
    }
    final DbSetup dbSetupSyncSource =
        new DbSetup(new DataSourceDestination(dataSourceSyncSource), operationSyncSource);
    dbSetupSyncSource.launch();

    // Clear caches
    clearCaches();

    sessionProvider = new HibernateSessionProvider(sessionFactory);
    syncSourceSessionProvider = new HibernateSessionProvider(sessionFactorySyncSource);
  }

  protected Operation initDb() {
    return null;
  }

  protected Operation initDbSyncSource() {
    return null;
  }

  private void clearCaches() {
    namedEntityDao.clearCaches();
    namedEntityCategoryDao.clearCaches();
  }

  protected void rebuildDataMessieIndex() {
    datamessieIndexingInitializer.startIndexing();
    datamessieIndexingInitializer.waitUntilIndexesInitialized(null);
  }

  @Override
  @After
  public void after() throws Exception {
    super.after();

    sessionProvider.closeSession();
    sessionProvider.closeStatelessSession();
    syncSourceSessionProvider.closeSession();
    syncSourceSessionProvider.closeStatelessSession();
  }

}
