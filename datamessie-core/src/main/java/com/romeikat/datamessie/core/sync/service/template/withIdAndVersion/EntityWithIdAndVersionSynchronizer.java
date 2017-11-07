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
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.converter.IntegerConverter;
import com.romeikat.datamessie.core.base.util.converter.LongConverter;
import com.romeikat.datamessie.core.base.util.converter.PercentageConverter;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;
import com.romeikat.datamessie.core.sync.service.SyncService;
import com.romeikat.datamessie.core.sync.service.template.ISynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;
import com.romeikat.datamessie.core.sync.util.SyncMode;

public abstract class EntityWithIdAndVersionSynchronizer<E extends EntityWithIdAndVersion>
    implements ISynchronizer {

  private final Class<E> clazz;
  private final EntityWithIdAndVersionDao<E> dao;
  private final SyncMode syncMode;
  private final SyncData syncData;

  private final SessionFactory sessionFactorySyncSource;
  private final SessionFactory sessionFactory;
  private final HibernateSessionProvider lhsSessionProvider;
  private final HibernateSessionProvider rhsSessionProvider;

  private int batchSizeIds;
  private final int batchSizeEntities;
  private final Double parallelismFactor;

  public EntityWithIdAndVersionSynchronizer(final Class<E> clazz, final ApplicationContext ctx) {
    this.clazz = clazz;
    this.dao = getDao(ctx);
    syncMode = SyncMode.valueOf(SpringUtil.getPropertyValue(ctx, "sync.mode"));
    syncData = SyncData.valueOf(SpringUtil.getPropertyValue(ctx, "sync.data"));

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

  @Override
  public void synchronize(final TaskExecution taskExecution) throws TaskCancelledException {
    if (!appliesFor(syncData)) {
      return;
    }

    final String msg = String.format("Synchronizing %s", clazz.getSimpleName());
    final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

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
    String msg = String.format("Deleting RHS for %s", clazz.getSimpleName());
    taskExecution.reportWork(msg);

    // Process in batches
    final long rhsCount = dao.countAll(rhsSessionProvider.getStatelessSession());
    int firstResult = 0;
    while (true) {
      // Don't update rhsCount, i.e. only process objects that existed in the beginning;
      // this is to avoid endless processing of an entity without proceeding to the next one
      if (firstResult >= rhsCount) {
        rhsSessionProvider.closeStatelessSession();
        return;
      }

      // Feedback
      final long lastResult = Math.min(firstResult + batchSizeIds, rhsCount);
      final double progress = (double) lastResult / (double) rhsCount;
      msg = String.format("Processing %s to %s of %s (%s)",
          IntegerConverter.INSTANCE.convertToString(firstResult + 1),
          LongConverter.INSTANCE.convertToString(lastResult),
          LongConverter.INSTANCE.convertToString(rhsCount),
          PercentageConverter.INSTANCE_2.convertToString(progress));
      final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

      // Load RHS
      final List<Long> rhsIds =
          dao.getIds(rhsSessionProvider.getStatelessSession(), firstResult, batchSizeIds);
      if (rhsIds.isEmpty()) {
        rhsSessionProvider.closeStatelessSession();
        return;
      }

      // Delete RHS
      final int numberOfDeletedEntities = delete(rhsIds);
      firstResult += rhsIds.size() - numberOfDeletedEntities;

      rhsSessionProvider.closeStatelessSession();
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private int delete(final List<Long> rhsIds) {
    // Load corresponding LHS
    final Collection<Long> lhsIds = loadLhsIds(rhsIds);

    // Decide
    final DeleteDecisionResults decisionResults =
        new DeleteDecider(lhsIds, rhsIds).makeDecisions().getDecisionResults();

    // Execute
    final int numberOfDeletedEntities = new DeleteExecutor<E>(decisionResults, batchSizeEntities,
        clazz, sessionFactory, parallelismFactor).executeDecisons();
    return numberOfDeletedEntities;
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
    String msg = String.format("Creating/updating LHS > RHS for %s", clazz.getSimpleName());
    taskExecution.reportWork(msg);

    // Process in batches
    final long lhsCount =
        SyncService.MAX_RESULTS == null ? dao.countAll(lhsSessionProvider.getStatelessSession())
            : SyncService.MAX_RESULTS;
    int firstResult = 0;
    while (true) {
      if (firstResult >= lhsCount) {
        // Don't update rhsCount, i.e. only process objects that existed in the beginning;
        // this is to avoid endless processing of an entity without proceeding to the next one
        lhsSessionProvider.closeStatelessSession();
        rhsSessionProvider.closeStatelessSession();
        return;
      }

      // Feedback
      final long lastResult = Math.min(firstResult + batchSizeIds, lhsCount);
      final double progress = (double) lastResult / (double) lhsCount;
      msg = String.format("Processing %s to %s of %s (%s)",
          IntegerConverter.INSTANCE.convertToString(firstResult + 1),
          LongConverter.INSTANCE.convertToString(lastResult),
          LongConverter.INSTANCE.convertToString(lhsCount),
          PercentageConverter.INSTANCE_2.convertToString(progress));
      final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

      // Load LHS
      final Map<Long, Long> lhsIdsWithVersion = dao
          .getIdsWithVersion(lhsSessionProvider.getStatelessSession(), firstResult, batchSizeIds);
      if (lhsIdsWithVersion.isEmpty()) {
        lhsSessionProvider.closeStatelessSession();
        rhsSessionProvider.closeStatelessSession();
        return;
      }

      // Create or update RHS
      createOrUpdate(lhsIdsWithVersion, lhsSessionProvider.getStatelessSession(),
          rhsSessionProvider.getStatelessSession(), taskExecution);
      firstResult += lhsIdsWithVersion.size();

      lhsSessionProvider.closeStatelessSession();
      rhsSessionProvider.closeStatelessSession();
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private void createOrUpdate(final Map<Long, Long> lhsIdsWithVersion,
      final StatelessSession lhsStatelessSession, final StatelessSession rhsStatelessSession,
      final TaskExecution taskExecution) throws TaskCancelledException {
    // Load corresponding RHS (in batches)
    final Map<Long, Long> rhsIdsWithVersion = loadRhsIdsWithVersion(lhsIdsWithVersion);

    // Decide
    final CreateOrUpdateDecisionResults decisionResults =
        new CreateOrUpdateDecider(lhsIdsWithVersion, rhsIdsWithVersion).makeDecisions()
            .getDecisionResults();

    // Execute
    new CreateOrUpdateExecutor<E>(decisionResults, batchSizeEntities, dao, clazz,
        lhsStatelessSession, rhsStatelessSession, sessionFactory, parallelismFactor,
        taskExecution) {
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

  protected abstract void copyProperties(E source, E target);

}
