package com.romeikat.datamessie.core.base.util.hibernate;

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

import java.lang.reflect.Field;
import javax.persistence.Column;
import javax.persistence.Table;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.metadata.ClassMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

public class HibernateSessionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HibernateSessionProvider.class);

  private final SessionFactory sessionFactory;

  private Session session;
  private StatelessSession statelessSession;

  public HibernateSessionProvider(final SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public synchronized Session getSession() {
    if (session == null || !session.isOpen()) {
      session = openSession();
    }
    return session;
  }

  public synchronized void closeSession() {
    if (session != null) {
      closeSession(session);
      session = null;
    }
  }

  public synchronized StatelessSession getStatelessSession() {
    if (statelessSession == null || !statelessSession.isOpen()) {
      statelessSession = openStatelessSession();
    }
    return statelessSession;
  }

  public synchronized void closeStatelessSession() {
    if (statelessSession != null) {
      closeStatelessSession(statelessSession);
      statelessSession = null;
    }
  }

  public synchronized void flush() {
    if (session != null) {
      session.flush();
    }
  }

  private Session openSession() {
    final Session session = sessionFactory.openSession();
    return session;
  }

  private StatelessSession openStatelessSession() {
    final StatelessSession statelessSession = sessionFactory.openStatelessSession();
    return statelessSession;
  }

  private void closeSession(final Session session) {
    try {
      session.flush();
      session.clear();
      session.close();
    } catch (final Exception e) {
      LOG.warn("Could not close session", e);
    }
  }

  private void closeStatelessSession(final StatelessSession statelessSession) {
    try {
      statelessSession.close();
    } catch (final Exception e) {
      LOG.warn("Could not close stateless session", e);
    }
  }

  public String getTableName(final Class<?> clazz) {
    final Table table = clazz.getAnnotation(Table.class);
    if (table == null) {
      return null;
    }

    return table.name();
  }

  public String getIdColumnName(final Class<?> clazz) {
    final ClassMetadata classMetadata = sessionFactory.getClassMetadata(clazz);
    final String identifierPropertyName = classMetadata.getIdentifierPropertyName();
    if (identifierPropertyName == null) {
      return null;
    }

    final Field field = ReflectionUtils.findField(clazz, identifierPropertyName);
    if (field == null) {
      return null;
    }

    final Column column = field.getAnnotation(Column.class);
    if (column == null) {
      return identifierPropertyName;
    }

    return column.name();
  }

}
