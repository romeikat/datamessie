package com.romeikat.datamessie.core.processing.service.fulltext.query;

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
import org.apache.lucene.analysis.Analyzer;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;

@Service
public class QueryExecutor {

  @Autowired
  private LuceneQueryExecutor luceneQueryExecutor;

  @Autowired
  private OutOfQueryExecutor outOfQueryExecutor;

  @Autowired
  private QueryUtil queryUtil;

  public int executeCount(final HibernateSessionProvider sessionProvider,
      final String luceneQueryString, final Class<?> clazz, final String field) {
    final Session session = sessionProvider.getSession();
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    final SearchFactory searchFactory = fullTextSession.getSearchFactory();
    final Analyzer analyzer = searchFactory.getAnalyzer(clazz);

    // Parse query
    final FullTextQuery query = queryUtil.parseQuery(luceneQueryString, analyzer);
    int count;
    if (query instanceof LuceneQuery) {
      // Use efficient count
      count =
          luceneQueryExecutor.executeCount((LuceneQuery) query, fullTextSession, analyzer, field);

    } else if (query instanceof OutOfQuery) {
      // Execute full query and count results
      final FullTextResult fullTextResult = outOfQueryExecutor.executeQuery((OutOfQuery) query,
          fullTextSession, analyzer, clazz, field);
      count = fullTextResult.size();
    } else {
      // Unsupported query
      count = 0;
    }

    sessionProvider.closeSession();
    return count;
  }

  public FullTextResult executeQuery(final HibernateSessionProvider sessionProvider,
      final String luceneQueryString, final Class<?> clazz, final String field) {
    final Session session = sessionProvider.getSession();
    final FullTextSession fullTextSession = Search.getFullTextSession(session);
    final SearchFactory searchFactory = fullTextSession.getSearchFactory();
    final Analyzer analyzer = searchFactory.getAnalyzer(clazz);

    // Parse query
    final FullTextQuery query = queryUtil.parseQuery(luceneQueryString, analyzer);
    FullTextResult fullTextResult;
    if (query instanceof LuceneQuery) {
      fullTextResult = luceneQueryExecutor.executeQuery((LuceneQuery) query, fullTextSession,
          analyzer, clazz, field);

    } else if (query instanceof OutOfQuery) {
      fullTextResult = outOfQueryExecutor.executeQuery((OutOfQuery) query, fullTextSession,
          analyzer, clazz, field);
    } else {
      // Unsupported query
      fullTextResult = new FullTextResult();
    }

    sessionProvider.closeSession();
    return fullTextResult;
  }

}
