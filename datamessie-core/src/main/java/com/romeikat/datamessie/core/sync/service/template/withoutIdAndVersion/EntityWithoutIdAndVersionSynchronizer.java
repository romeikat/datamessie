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

import java.util.Collection;
import javax.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.EntityDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.Entity;
import com.romeikat.datamessie.core.sync.service.SyncService;
import com.romeikat.datamessie.core.sync.service.template.ISynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;
import com.romeikat.datamessie.core.sync.util.SyncMode;

public abstract class EntityWithoutIdAndVersionSynchronizer<E extends Entity>
    implements ISynchronizer {

  private final Class<E> clazz;
  private final EntityDao<E> entityDao;
  private final SyncMode syncMode;
  private final SyncData syncData;

  private final SessionFactory sessionFactorySyncSource;
  private final SessionFactory sessionFactory;
  private final HibernateSessionProvider lhsSessionProvider;
  private final HibernateSessionProvider rhsSessionProvider;
  private final Double parallelismFactor;
  private final long sleepingInterval = 60000;

  public EntityWithoutIdAndVersionSynchronizer(final Class<E> clazz, final ApplicationContext ctx) {
    this.clazz = clazz;
    this.entityDao = getDao(ctx);
    syncMode = SyncMode.valueOf(SpringUtil.getPropertyValue(ctx, "sync.mode"));
    syncData = SyncData.valueOf(SpringUtil.getPropertyValue(ctx, "sync.data"));

    sessionFactorySyncSource = ctx.getBean("sessionFactorySyncSource", SessionFactory.class);
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    lhsSessionProvider = new HibernateSessionProvider(sessionFactorySyncSource);
    rhsSessionProvider = new HibernateSessionProvider(sessionFactory);
    parallelismFactor = Double.valueOf(SpringUtil.getPropertyValue(ctx, "sync.parallelism.factor"));
  }

  protected abstract boolean appliesFor(SyncData syncData);

  protected abstract EntityDao<E> getDao(ApplicationContext ctx);

  @Override
  public void synchronize(final TaskExecution taskExecution) throws TaskCancelledException {
    if (!appliesFor(syncData)) {
      return;
    }

    final String msg = String.format("Synchronizing %s", clazz.getSimpleName());
    final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

    while (true) {
      try {
        synchronizeAll(taskExecution);
        break;
      } catch (final PersistenceException e) {
        retry(taskExecution);
      }
    }

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
  }

  private void synchronizeAll(final TaskExecution taskExecution) {
    // Load
    final Collection<E> lhsEntities = SyncService.MAX_RESULTS == null
        ? entityDao.getAllEntites(lhsSessionProvider.getStatelessSession())
        : entityDao.getEntites(lhsSessionProvider.getStatelessSession(), 0,
            SyncService.MAX_RESULTS);
    final Collection<E> rhsEntities =
        entityDao.getAllEntites(rhsSessionProvider.getStatelessSession());

    // Decide
    final DecisionResults<E> decisionResults =
        new Decider<E>(lhsEntities, rhsEntities).makeDecisions().getDecisionResults();

    // Execute
    new Executor<E>(decisionResults, syncMode, entityDao, clazz, sessionFactory,
        parallelismFactor) {
      @Override
      protected void copyProperties(final E source, final E target) {
        EntityWithoutIdAndVersionSynchronizer.this.copyProperties(source, target);
      }
    }.executeDecisons();

    lhsSessionProvider.closeStatelessSession();
    rhsSessionProvider.closeStatelessSession();
  }

  private void retry(final TaskExecution taskExecution) {
    lhsSessionProvider.closeStatelessSession();
    rhsSessionProvider.closeStatelessSession();

    final String msg =
        String.format("Retrying in %,d seconds due to connection issues", sleepingInterval / 1000);
    taskExecution.reportWork(msg);
    try {
      Thread.sleep(sleepingInterval);
    } catch (final InterruptedException e) {
    }
  }

  protected abstract void copyProperties(E source, E target);

}
