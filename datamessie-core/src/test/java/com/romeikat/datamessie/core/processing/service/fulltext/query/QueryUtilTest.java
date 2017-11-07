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

import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.apache.lucene.analysis.Analyzer;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.processing.service.fulltext.query.LuceneQuery;
import com.romeikat.datamessie.core.processing.service.fulltext.query.OutOfQuery;
import com.romeikat.datamessie.core.processing.service.fulltext.query.QueryUtil;

public class QueryUtilTest extends AbstractTest {

  @Autowired
  private QueryUtil queryUtil;

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  public void transformsOutOfQueryToQuery() throws Exception {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final FullTextSession fullTextSession = Search.getFullTextSession(sessionProvider.getSession());
    final SearchFactory searchFactory = fullTextSession.getSearchFactory();
    final Analyzer analyzer = searchFactory.getAnalyzer(CleanedContent.class);
    final OutOfQuery outOfQuery =
        new OutOfQuery(2, Arrays.asList(new String[] {"abc", "def", "ghi"}));

    final LuceneQuery luceneQuery = queryUtil.toLuceneQuery(outOfQuery, analyzer);
    sessionProvider.closeSession();

    final String parsedLuceneQueryString = luceneQuery.getLuceneQueryString();
    final boolean abcDef = parsedLuceneQueryString.contains("abc AND def")
        || parsedLuceneQueryString.contains("def AND abc");
    final boolean abcGhi = parsedLuceneQueryString.contains("abc AND ghi")
        || parsedLuceneQueryString.contains("ghi AND abc");
    final boolean defGhi = parsedLuceneQueryString.contains("def AND ghi")
        || parsedLuceneQueryString.contains("ghi AND def");
    assertTrue(abcDef);
    assertTrue(abcGhi);
    assertTrue(defGhi);
  }

}
