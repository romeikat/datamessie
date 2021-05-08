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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.mutable.MutableLong;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.converter.LongConverter;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;
import com.romeikat.datamessie.core.sync.service.SyncService;
import com.romeikat.datamessie.core.sync.service.template.ISynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;
import com.romeikat.datamessie.core.sync.util.SyncMode;


public abstract class EntityWithIdAndVersionSynchronizer<E extends EntityWithIdAndVersion>
    implements ISynchronizer {

  private static final Logger LOG =
      LoggerFactory.getLogger(EntityWithIdAndVersionSynchronizer.class);

  private final Class<E> clazz;
  private final EntityWithIdAndVersionDao<E> dao;
  private final SyncMode syncMode;
  private final SyncData syncData;
  private final boolean syncFilterEnabled;

  private final SessionFactory sessionFactorySyncSource;
  private final SessionFactory sessionFactory;
  private final HibernateSessionProvider lhsSessionProvider;
  private final HibernateSessionProvider rhsSessionProvider;

  private int batchSizeIds;
  private final int batchSizeEntities;
  private final Double parallelismFactor;
  private final long sleepingInterval = 60000;
  private Predicate<Long> lhsIdFilter;
  private Predicate<E> lhsEntityFilter;

  public EntityWithIdAndVersionSynchronizer(final Class<E> clazz, final ApplicationContext ctx) {
    this.clazz = clazz;
    this.dao = getDao(ctx);
    syncMode = SyncMode.valueOf(SpringUtil.getPropertyValue(ctx, "sync.mode"));
    syncData = SyncData.valueOf(SpringUtil.getPropertyValue(ctx, "sync.data"));
    syncFilterEnabled = Boolean.valueOf(SpringUtil.getPropertyValue(ctx, "sync.filter.enabled"));

    sessionFactorySyncSource = ctx.getBean("sessionFactorySyncSource", SessionFactory.class);
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    lhsSessionProvider = new HibernateSessionProvider(sessionFactorySyncSource);
    rhsSessionProvider = new HibernateSessionProvider(sessionFactory);

    batchSizeIds = Integer.valueOf(SpringUtil.getPropertyValue(ctx, "sync.batch.size.ids"));
    if (SyncService.MAX_RESULTS != null) {
      batchSizeIds = Math.min(batchSizeIds, SyncService.MAX_RESULTS);
    }
    batchSizeEntities =
        Integer.valueOf(SpringUtil.getPropertyValue(ctx, "sync.batch.size.entities"));
    parallelismFactor = Double.valueOf(SpringUtil.getPropertyValue(ctx, "sync.parallelism.factor"));
  }

  protected abstract boolean appliesFor(SyncData syncData);

  protected abstract EntityWithIdAndVersionDao<E> getDao(ApplicationContext ctx);

  protected Predicate<Long> getLhsIdFilter() {
    // Per default, all available IDs are synchronized
    return id -> true;
  }

  protected Predicate<E> getLhsEntityFilter() {
    // Per default, all available entities are synchronized
    return e -> true;
  }

  @Override
  public void synchronize(final TaskExecution taskExecution) throws TaskCancelledException {
    if (!appliesFor(syncData)) {
      return;
    }

    final String msg = String.format("Synchronizing %s", clazz.getSimpleName());
    final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

    lhsIdFilter = getLhsIdFilter();
    lhsEntityFilter = getLhsEntityFilter();

    // Delete non-existing RHS
    if (syncMode.shouldDeleteData()) {
      delete(taskExecution);
    }

    // Create and update LHS -> RHS
    if (syncMode.shouldCreateAndUpdateData()) {
      createOrUpdate(taskExecution);
    }

    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
  }

  private void delete(final TaskExecution taskExecution) throws TaskCancelledException {
    final String msg = String.format("Deleting RHS for %s", clazz.getSimpleName());
    taskExecution.reportWork(msg);

    // Process in batches
    final MutableLong firstId = new MutableLong(0);
    while (true) {
      try {
        final boolean moreBatches = deleteBatch(taskExecution, firstId);
        if (!moreBatches) {
          return;
        }
      } catch (final Exception e) {
        retry(taskExecution, e);
      }
    }
  }

  private boolean deleteBatch(final TaskExecution taskExecution, final MutableLong firstId)
      throws TaskCancelledException {
    final TaskExecutionWork work = taskExecution.startWork();

    // Load RHS
    final List<Long> rhsIds =
        dao.getIds(rhsSessionProvider.getStatelessSession(), firstId.getValue(), batchSizeIds);
    if (rhsIds.isEmpty()) {
      rhsSessionProvider.closeStatelessSession();
      return false;
    }

    // Feedback
    final long lastId = rhsIds.get(rhsIds.size() - 1);
    final String msg = String.format("Processing batch %s to %s",
        LongConverter.INSTANCE.convertToString(firstId.getValue()),
        LongConverter.INSTANCE.convertToString(lastId));
    taskExecution.reportWorkStart(work, msg);

    // Delete RHS
    delete(rhsIds);
    firstId.setValue(lastId + 1);

    rhsSessionProvider.closeStatelessSession();
    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
    return true;
  }

  private void delete(final List<Long> rhsIds) {
    // Load corresponding LHS
    List<Long> lhsIds = loadLhsIds(rhsIds);

    // Filter IDs
    if (syncFilterEnabled) {
      lhsIds = lhsIds.stream().filter(lhsIdFilter).collect(Collectors.toList());
    }

    // Decide
    final DeleteDecisionResults decisionResults =
        new DeleteDecider(lhsIds, rhsIds).makeDecisions().getDecisionResults();

    // Execute
    new DeleteExecutor<E>(decisionResults, batchSizeEntities, clazz, sessionFactory,
        parallelismFactor).executeDecisons();
  }

  private List<Long> loadLhsIds(final List<Long> rhsIds) {
    final List<Long> lhsIds =
        Collections.synchronizedList(Lists.newArrayListWithExpectedSize(rhsIds.size()));
    final List<List<Long>> rhsIdsBatches = Lists.partition(rhsIds, batchSizeEntities);
    new ParallelProcessing<List<Long>>(sessionFactorySyncSource, rhsIdsBatches, parallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider lhsSessionProvider,
          final List<Long> rhsIdsBatch) {
        final Collection<Long> lhsIdsBatch =
            dao.getIds(lhsSessionProvider.getStatelessSession(), rhsIdsBatch);
        lhsIds.addAll(lhsIdsBatch);
      }
    };
    return lhsIds;
  }

  private void createOrUpdate(final TaskExecution taskExecution) throws TaskCancelledException {
    final String msg = String.format("Creating/updating LHS > RHS for %s", clazz.getSimpleName());
    taskExecution.reportWork(msg);

    // Process in batches
    final MutableLong firstId = new MutableLong(0);
    while (true) {
      try {
        final boolean moreBatches = createOrUpdateBatch(taskExecution, firstId);
        if (!moreBatches) {
          return;
        }
      } catch (final PersistenceException e) {
        retry(taskExecution, e);
      }
    }
  }

  private boolean createOrUpdateBatch(final TaskExecution taskExecution, final MutableLong firstId)
      throws TaskCancelledException {
    final TaskExecutionWork work = taskExecution.startWork();

    // Load LHS
    final TreeMap<Long, Long> lhsIdsWithVersion = dao.getIdsWithVersion(
        lhsSessionProvider.getStatelessSession(), firstId.getValue(), batchSizeIds);
    if (lhsIdsWithVersion.isEmpty()) {
      lhsSessionProvider.closeStatelessSession();
      rhsSessionProvider.closeStatelessSession();
      return false;
    }

    // Feedback
    final long lastId = lhsIdsWithVersion.lastKey();
    final String msg = String.format("Processing batch from %s to %s",
        LongConverter.INSTANCE.convertToString(firstId.getValue()),
        LongConverter.INSTANCE.convertToString(lastId));
    taskExecution.reportWorkStart(work, msg);

    // Create or update RHS
    createOrUpdate(lhsIdsWithVersion, lhsSessionProvider.getStatelessSession(),
        rhsSessionProvider.getStatelessSession(), taskExecution);
    firstId.setValue(lastId + 1);

    lhsSessionProvider.closeStatelessSession();
    rhsSessionProvider.closeStatelessSession();
    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();
    return true;
  }

  private void createOrUpdate(Map<Long, Long> lhsIdsWithVersion,
      final StatelessSession lhsStatelessSession, final StatelessSession rhsStatelessSession,
      final TaskExecution taskExecution) throws TaskCancelledException {
    // Filter IDs
    if (syncFilterEnabled) {
      lhsIdsWithVersion =
          lhsIdsWithVersion.entrySet().stream().filter(e -> lhsIdFilter.test(e.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Load corresponding RHS (in batches)
    final Map<Long, Long> rhsIdsWithVersion = loadRhsIdsWithVersion(lhsIdsWithVersion);

    // Decide
    final CreateOrUpdateDecisionResults decisionResults =
        new CreateOrUpdateDecider(lhsIdsWithVersion, rhsIdsWithVersion).makeDecisions()
            .getDecisionResults();

    // Execute
    new CreateOrUpdateExecutor<E>(decisionResults, batchSizeEntities, dao, clazz, syncFilterEnabled,
        lhsEntityFilter, lhsStatelessSession, rhsStatelessSession, sessionFactory,
        parallelismFactor, taskExecution) {
      @Override
      protected void copyProperties(final E source, final E target) {
        EntityWithIdAndVersionSynchronizer.this.copyProperties(source, target);
      }
    }.executeDecisons();
  }

  private Map<Long, Long> loadRhsIdsWithVersion(final Map<Long, Long> lhsIdsWithVersion) {
    final Map<Long, Long> rhsIdsWithVersion = new ConcurrentHashMap<>(lhsIdsWithVersion.size());

    final List<Long> lhsIds = Lists.newArrayList(lhsIdsWithVersion.keySet());
    final List<List<Long>> lhsIdsBatches = Lists.partition(lhsIds, batchSizeEntities);
    new ParallelProcessing<List<Long>>(sessionFactory, lhsIdsBatches, parallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider,
          final List<Long> lhsIdsBatch) {
        final Map<Long, Long> rhsIdsWithVersionBatch =
            dao.getIdsWithVersion(rhsSessionProvider.getStatelessSession(), lhsIdsBatch);
        rhsIdsWithVersion.putAll(rhsIdsWithVersionBatch);
      }
    };

    return rhsIdsWithVersion;
  }

  private void retry(final TaskExecution taskExecution, final Exception e) {
    lhsSessionProvider.closeStatelessSession();
    rhsSessionProvider.closeStatelessSession();

    final String msg =
        String.format("Retrying in %,d seconds due to connection issues", sleepingInterval / 1000);
    LOG.error(msg, e);
    taskExecution.reportWork(msg);
    try {
      Thread.sleep(sleepingInterval);
    } catch (final InterruptedException e2) {
    }
  }

  protected abstract void copyProperties(E source, E target);

}
