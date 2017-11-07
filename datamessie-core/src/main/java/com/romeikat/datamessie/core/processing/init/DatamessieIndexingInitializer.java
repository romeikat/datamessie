package com.romeikat.datamessie.core.processing.init;

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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;

@Service
public class DatamessieIndexingInitializer extends AbstractIndexingInitializer {

  @Value("${processing.module.enabled}")
  private boolean moduleEnabled;

  @Value("${documents.indexing.rebuildIndexAtStartup}")
  private boolean reindexAtStartup;

  @Value("${documents.indexing.batch.size}")
  private int batchSize;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  protected boolean shouldReindexAtStartup() {
    return moduleEnabled && reindexAtStartup;
  }

  @Override
  protected int getBatchSize() {
    return batchSize;
  }

  @Override
  protected HibernateSessionProvider getHibernateSessionProvider() {
    return new HibernateSessionProvider(sessionFactory);
  }

  @Override
  protected Class<?>[] getClassesToIndex() {
    return new Class<?>[] {CleanedContent.class};
  }

}
