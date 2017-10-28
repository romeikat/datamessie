package com.romeikat.datamessie.core.base.util.execute;

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

import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecuteWithTransactionAndResult<T> {

  private static final Logger LOG = LoggerFactory.getLogger(ExecuteWithTransactionAndResult.class);

  private final StatelessSession statelessSession;

  public ExecuteWithTransactionAndResult(final StatelessSession statelessSession) {
    this.statelessSession = statelessSession;
  }

  public T execute() {
    Transaction tx = null;
    try {
      tx = beginTransaction(statelessSession);
      final T result = executeWithResult(statelessSession);
      commitTransaction(tx);
      return result;
    } catch (final Exception e) {
      onException(e);
      try {
        rollbackTransaction(tx);
      } catch (final Exception e2) {
        onException(e2);
      }
      statelessSession.close();
      return null;
    }
  }

  protected abstract T executeWithResult(StatelessSession statelessSession);

  protected void onException(final Exception e) {
    LOG.error("Could not execute transaction", e);
  }

  private Transaction beginTransaction(final StatelessSession statelessSession) {
    return statelessSession.beginTransaction();
  }

  private void commitTransaction(final Transaction tx) {
    if (tx == null) {
      return;
    }

    tx.commit();
  }

  private void rollbackTransaction(final Transaction tx) {
    if (tx == null) {
      return;
    }

    tx.rollback();
  }

}
