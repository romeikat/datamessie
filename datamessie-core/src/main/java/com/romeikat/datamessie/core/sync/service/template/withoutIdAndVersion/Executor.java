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

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romeikat.datamessie.core.base.dao.EntityDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.Entity;
import com.romeikat.datamessie.core.sync.util.SyncMode;

public abstract class Executor<E extends Entity> {

  private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

  private final DecisionResults<E> decisionResults;
  private final SyncMode syncMode;
  private final EntityDao<E> entityDao;
  private final Class<E> clazz;
  private final SessionFactory sessionFactory;
  private final Double parallelismFactor;;

  public Executor(final DecisionResults<E> decisionResults, final SyncMode syncMode, final EntityDao<E> dao,
      final Class<E> clazz, final SessionFactory sessionFactory, final Double parallelismFactor) {
    this.decisionResults = decisionResults;
    this.syncMode = syncMode;
    this.entityDao = dao;
    this.clazz = clazz;
    this.sessionFactory = sessionFactory;
    this.parallelismFactor = parallelismFactor;
  }

  protected abstract void copyProperties(E source, E target);

  public void executeDecisons() {
    // Delete non-existing RHS
    if (syncMode.shouldDeleteData()) {
      delete();
    }

    // Create LHS -> RHS
    if (syncMode.shouldCreateAndUpdateData()) {
      create();
    }
  }

  private void delete() {
    final Collection<E> rhsEntities = decisionResults.getToBeDeleted();
    new ParallelProcessing<E>(sessionFactory, rhsEntities, parallelismFactor) {
      @Override
      protected HibernateSessionProvider createSessionProvider() {
        return new HibernateSessionProvider(sessionFactory);
      }

      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider, final E rhsEntity) {
        delete(rhsEntity, rhsSessionProvider);
      }
    };
  }

  private void delete(final E rhsEntity, final HibernateSessionProvider rhsSessionProvider) {
    entityDao.delete(rhsSessionProvider.getStatelessSession(), rhsEntity);
  }

  private void create() {
    final Collection<E> lhsEntities = decisionResults.getToBeCreated();
    new ParallelProcessing<E>(sessionFactory, lhsEntities, parallelismFactor) {
      @Override
      protected HibernateSessionProvider createSessionProvider() {
        return new HibernateSessionProvider(sessionFactory);
      }

      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider, final E lhsEntity) {
        create(lhsEntity, rhsSessionProvider);
      }
    };
  }

  private void create(final E lhsEntity, final HibernateSessionProvider rhsSessionProvider) {
    // Create
    E rhsEntity;
    try {
      rhsEntity = clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      LOG.warn("Could not create RHS {} for LHS {}", clazz.getSimpleName(), lhsEntity);
      return;
    }

    // Copy
    copyProperties(lhsEntity, rhsEntity);

    // Save
    entityDao.insert(rhsSessionProvider.getStatelessSession(), rhsEntity);
  }

}
