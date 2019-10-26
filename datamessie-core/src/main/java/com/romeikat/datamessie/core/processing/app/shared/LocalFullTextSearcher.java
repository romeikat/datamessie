package com.romeikat.datamessie.core.processing.app.shared;

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
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.shared.IFullTextSearcher;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContentImpl;
import com.romeikat.datamessie.core.processing.service.fulltext.query.QueryExecutor;

@Service
public class LocalFullTextSearcher implements IFullTextSearcher {

  @Autowired
  private QueryExecutor queryExecutor;

  @Autowired
  private SessionFactory sessionFactory;


  @Override
  public int getOrder() {
    return 1;
  }

  @Override
  public FullTextResult searchForCleanedContent(final String luceneQueryString) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    final FullTextResult fullTextResult = queryExecutor.executeQuery(sessionProvider,
        luceneQueryString, CleanedContentImpl.class, "content");

    return fullTextResult;
  }

}
