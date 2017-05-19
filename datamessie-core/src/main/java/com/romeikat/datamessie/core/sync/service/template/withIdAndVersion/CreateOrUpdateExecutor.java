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

import org.apache.wicket.util.lang.Objects;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.converter.IntegerConverter;
import com.romeikat.datamessie.core.base.util.converter.PercentageConverter;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;

public abstract class CreateOrUpdateExecutor<E extends EntityWithIdAndVersion> {

  private static final Logger LOG = LoggerFactory.getLogger(CreateOrUpdateExecutor.class);

  private final CreateOrUpdateDecisionResults decisionResults;
  private final int batchSizeEntities;
  private final EntityWithIdAndVersionDao<E> dao;
  private final Class<E> clazz;
  private final StatelessSession lhsStatelessSession;
  private final StatelessSession rhsStatelessSession;
  private final SessionFactory sessionFactory;
  private final Double parallelismFactor;
  private final TaskExecution taskExecution;

  private final List<Long> failedLhsIds;

  public CreateOrUpdateExecutor(final CreateOrUpdateDecisionResults decisionResults, final int batchSizeEntities,
      final EntityWithIdAndVersionDao<E> dao, final Class<E> clazz, final StatelessSession lhsStatelessSession,
      final StatelessSession rhsStatelessSession, final SessionFactory sessionFactory, final Double parallelismFactor,
      final TaskExecution taskExecution) {
    this.decisionResults = decisionResults;
    this.batchSizeEntities = batchSizeEntities;
    this.dao = dao;
    this.clazz = clazz;
    this.lhsStatelessSession = lhsStatelessSession;
    this.rhsStatelessSession = rhsStatelessSession;
    this.sessionFactory = sessionFactory;
    this.parallelismFactor = parallelismFactor;
    this.taskExecution = taskExecution;

    failedLhsIds = Collections.synchronizedList(Lists.newArrayList());
  }

  protected abstract void copyProperties(E source, E target);

  public void executeDecisons() throws TaskCancelledException {
    create();
    update();

    while (!failedLhsIds.isEmpty()) {
      final int numberOfFailedLhsIdsBefore = failedLhsIds.size();
      createFailed();
      final int numberOfFailedLhsIdsAfter = failedLhsIds.size();

      if (numberOfFailedLhsIdsAfter >= numberOfFailedLhsIdsBefore) {
        LOG.warn("Number of failed LHS IDs could not be reduced");
        break;
      }
    }
  }

  private void create() throws TaskCancelledException {
    final List<Long> lhsIds = decisionResults.getToBeCreated();
    create(lhsIds);
  }

  private void create(final List<Long> lhsIds) throws TaskCancelledException {
    final List<List<Long>> lhsIdsBatches = Lists.partition(lhsIds, batchSizeEntities);
    final int lhsCount = lhsIds.size();
    int firstEntity = 0;

    for (final List<Long> lhsIdsBatch : lhsIdsBatches) {
      // Feedback
      final int lastEntity = firstEntity + lhsIdsBatch.size();
      final double progress = (double) lastEntity / (double) lhsCount;
      final String msg = String.format("Creating %s to %s of %s (%s)",
          IntegerConverter.INSTANCE.convertToString(firstEntity + 1),
          IntegerConverter.INSTANCE.convertToString(lastEntity), IntegerConverter.INSTANCE.convertToString(lhsCount),
          PercentageConverter.INSTANCE_2.convertToString(progress));
      final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

      createBatch(lhsIdsBatch);

      firstEntity += batchSizeEntities;

      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private void createBatch(final List<Long> lhsIds) {
    final Collection<E> lhsEntities = dao.getEntities(lhsStatelessSession, lhsIds);
    new ParallelProcessing<E>(sessionFactory, lhsEntities, parallelismFactor) {
      @Override
      protected HibernateSessionProvider createSessionProvider() {
        return new HibernateSessionProvider(sessionFactory);
      }

      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider, final E lhsEntity) {
        try {
          create(rhsSessionProvider.getStatelessSession(), lhsEntity);
        } catch (final ConstraintViolationException e) {
          LOG.info("Could not create entity {} at RHS", lhsEntity.getId());
          rhsSessionProvider.closeStatelessSession();
          failedLhsIds.add(lhsEntity.getId());
        }
      }
    };
  }

  private void create(final StatelessSession rhsStatelessSession, final E lhsEntity) {
    // Create
    E rhsEntity;
    try {
      rhsEntity = clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOG.warn("Could not create RHS {} with ID {}", clazz.getSimpleName(), lhsEntity.getId());
      return;
    }

    // Copy
    rhsEntity.setId(lhsEntity.getId());
    rhsEntity.setVersion(lhsEntity.getVersion());
    copyProperties(lhsEntity, rhsEntity);

    // Save
    dao.insert(rhsStatelessSession, rhsEntity);
  }

  private void update() throws TaskCancelledException {
    final List<Long> lhsIds = decisionResults.getToBeUpdated();
    update(lhsIds);
  }

  private void update(final List<Long> lhsIds) throws TaskCancelledException {
    final List<List<Long>> lhsIdsBatches = Lists.partition(lhsIds, batchSizeEntities);
    final int lhsCount = lhsIds.size();
    int firstEntity = 0;

    for (final List<Long> lhsIdsBatch : lhsIdsBatches) {
      // Feedback
      final int lastEntity = firstEntity + lhsIdsBatch.size();
      final double progress = (double) lastEntity / (double) lhsCount;
      final String msg = String.format("Updating %s to %s of %s (%s)",
          IntegerConverter.INSTANCE.convertToString(firstEntity + 1),
          IntegerConverter.INSTANCE.convertToString(lastEntity), IntegerConverter.INSTANCE.convertToString(lhsCount),
          PercentageConverter.INSTANCE_2.convertToString(progress));
      final TaskExecutionWork work = taskExecution.reportWorkStart(msg);

      updateBatch(lhsIdsBatch);

      firstEntity += batchSizeEntities;

      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }
  }

  private void updateBatch(final List<Long> lhsIds) {
    final Collection<E> lhsEntities = dao.getEntities(lhsStatelessSession, lhsIds);
    final Map<Long, E> rhsEntities = new ConcurrentHashMap<>(dao.getIdsWithEntities(rhsStatelessSession, lhsIds));
    new ParallelProcessing<E>(sessionFactory, lhsEntities, parallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider, final E lhsEntity) {
        final E rhsEntity = rhsEntities.get(lhsEntity.getId());
        if (rhsEntity == null) {
          LOG.warn("Could not load RHS {} with ID {}", clazz.getSimpleName(), lhsEntity.getId());
          return;
        }

        new ExecuteWithTransaction(rhsSessionProvider.getStatelessSession()) {
          @Override
          protected void execute(final StatelessSession statelessSession) {
            update(rhsSessionProvider, lhsEntity, rhsEntity);
          }
        }.execute();
      }
    };
  }

  private void update(final HibernateSessionProvider rhsSessionProvider, final E lhsEntity, final E rhsEntity) {
    // Copy
    copyProperties(lhsEntity, rhsEntity);

    // Update
    try {
      dao.update(rhsSessionProvider.getStatelessSession(), rhsEntity);

      // Update version
      updateVersion(rhsSessionProvider.getStatelessSession(), lhsEntity, rhsEntity);
    } catch (final ConstraintViolationException e) {
      LOG.info("Could not update entity {} at RHS", lhsEntity.getId());
      rhsSessionProvider.closeStatelessSession();
      failedLhsIds.add(lhsEntity.getId());
      deleteFailed(rhsSessionProvider.getStatelessSession(), lhsEntity.getId());
    }
  }

  private void updateVersion(final StatelessSession rhsStatelessSession, final E lhsEntity, final E rhsEntity) {
    if (Objects.equal(lhsEntity.getVersion(), rhsEntity.getVersion())) {
      return;
    }

    final String queryString = "UPDATE " + clazz.getSimpleName() + " SET version = :_version WHERE id = :_id";
    final Query<?> query = rhsStatelessSession.createQuery(queryString);
    query.setParameter("_id", rhsEntity.getId());
    query.setParameter("_version", lhsEntity.getVersion());
    query.executeUpdate();
  }

  private void deleteFailed(final StatelessSession rhsStatelessSession, final long lhsId) {
    final E rhsEntity = dao.getEntity(rhsStatelessSession, lhsId);
    if (rhsEntity == null) {
      return;
    }

    dao.delete(rhsStatelessSession, rhsEntity);
  }

  private void createFailed() throws TaskCancelledException {
    final List<Long> lhsIds = Lists.newArrayList(failedLhsIds);
    failedLhsIds.clear();
    create(lhsIds);
  }

}
