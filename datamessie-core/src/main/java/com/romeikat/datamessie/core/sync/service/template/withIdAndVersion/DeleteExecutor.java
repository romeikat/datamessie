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
import java.util.List;
import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.util.execute.ExecuteWithTransaction;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.EntityWithIdAndVersion;

public class DeleteExecutor<E extends EntityWithIdAndVersion> {

  private final DeleteDecisionResults decisionResults;
  private final int batchSizeEntities;
  private final Class<E> clazz;
  private final SessionFactory sessionFactory;
  private final Double parallelismFactor;

  public DeleteExecutor(final DeleteDecisionResults decisionResults, final int batchSizeEntities,
      final Class<E> clazz, final SessionFactory sessionFactory, final Double parallelismFactor) {
    this.decisionResults = decisionResults;
    this.batchSizeEntities = batchSizeEntities;
    this.clazz = clazz;
    this.sessionFactory = sessionFactory;
    this.parallelismFactor = parallelismFactor;
  }

  public int executeDecisons() {
    final int numberOfDeletedEntities = delete();
    return numberOfDeletedEntities;
  }

  private int delete() {
    final MutableInt numberOfDeletedEntities = new MutableInt(0);

    final List<Long> rhsIds = decisionResults.getToBeDeleted();
    final List<List<Long>> rhsIdsBatches = Lists.partition(rhsIds, batchSizeEntities);
    new ParallelProcessing<List<Long>>(sessionFactory, rhsIdsBatches, parallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider rhsSessionProvider,
          final List<Long> rhsIdsBatch) {
        new ExecuteWithTransaction(rhsSessionProvider.getStatelessSession()) {

          @Override
          protected void execute(final StatelessSession statelessSession) {
            final int numberOfDeletedEntitiesBatch =
                delete(rhsSessionProvider.getStatelessSession(), rhsIdsBatch);
            synchronized (numberOfDeletedEntities) {
              numberOfDeletedEntities.add(numberOfDeletedEntitiesBatch);
            }
          }
        }.execute();
      }
    };

    return numberOfDeletedEntities.intValue();
  }

  private int delete(final StatelessSession rhsStatelessSession, final Collection<Long> rhsIds) {
    if (rhsIds.isEmpty()) {
      return 0;
    }

    final String queryString = "DELETE FROM " + clazz.getSimpleName() + " WHERE id IN :_rhsIds";
    final Query<?> query = rhsStatelessSession.createQuery(queryString);
    query.setParameterList("_rhsIds", rhsIds);
    final int numberOfDeletedEntities = query.executeUpdate();
    return numberOfDeletedEntities;
  }

}
