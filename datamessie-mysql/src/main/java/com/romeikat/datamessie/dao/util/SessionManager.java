package com.romeikat.datamessie.dao.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (mysql)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManager {

  private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

  private static final ThreadLocal<Integer> TRANSACTION_COUNT = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private static final ThreadLocal<StatelessSession> CURRENT_SESSION =
      new ThreadLocal<StatelessSession>();

  private static SessionFactory SESSION_FACTORY;

  private SessionManager() {}

  private static void initSessionFactory() {
    if (SESSION_FACTORY == null) {
      SESSION_FACTORY = SpringContext.getBean(SessionFactory.class);
    }
  }

  public static StatelessSession beginTransaction() {
    final Integer transactionCount = TRANSACTION_COUNT.get();

    // If a transaction is already in progress, reuse its session
    if (transactionCount > 0) {
      TRANSACTION_COUNT.set(transactionCount + 1);
      return CURRENT_SESSION.get();
    }

    // Otherwise, open a new session and cache it
    else {
      TRANSACTION_COUNT.set(transactionCount + 1);
      final StatelessSession statelessSession = openStatelessSession();
      CURRENT_SESSION.set(statelessSession);
      return statelessSession;
    }
  }

  public static void endTransaction() {
    final Integer transactionCount = TRANSACTION_COUNT.get();

    // If no transaction is currently in progress, warn
    if (transactionCount == 0) {
      LOG.warn("No transaction is currently in progress");
    }

    // In case of the outermost transaction, close its session
    else if (transactionCount == 1) {
      TRANSACTION_COUNT.set(transactionCount - 1);
      final StatelessSession statelessSession = CURRENT_SESSION.get();
      closeStatelessSession(statelessSession);
      CURRENT_SESSION.remove();
    }

    // Otherwise, decrement count
    else {
      TRANSACTION_COUNT.set(transactionCount - 1);
    }
  }

  public static StatelessSession getSession() {
    // If a session is already opened, reuse it
    StatelessSession statelessSession = CURRENT_SESSION.get();
    if (statelessSession != null) {
      return statelessSession;
    }

    // Otherwise, open a new session and cache
    else {
      statelessSession = openStatelessSession();
      CURRENT_SESSION.set(statelessSession);
      return statelessSession;
    }
  }

  public static void closeSession() {
    final Integer transactionCount = TRANSACTION_COUNT.get();

    // If a transaction is currently in progress, keep session open
    if (transactionCount > 0) {
    }

    // Otherwise, close current session
    else {
      final StatelessSession statelessSession = CURRENT_SESSION.get();
      if (statelessSession != null) {
        closeStatelessSession(statelessSession);
        CURRENT_SESSION.remove();
      }
    }
  }

  private static StatelessSession openStatelessSession() {
    initSessionFactory();
    final StatelessSession statelessSession = SESSION_FACTORY.openStatelessSession();
    return statelessSession;
  }

  private static void closeStatelessSession(final StatelessSession statelessSession) {
    statelessSession.close();
  }

}
