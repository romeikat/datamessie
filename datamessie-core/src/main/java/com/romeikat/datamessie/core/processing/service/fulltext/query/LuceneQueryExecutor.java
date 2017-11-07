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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.ParseUtil;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;

@Service
public class LuceneQueryExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(LuceneQueryExecutor.class);

  @Autowired
  private CollectionUtil collectionUtil;

  @Autowired
  private ParseUtil parseUtil;

  @Autowired
  private QueryUtil queryUtil;

  public int executeCount(final LuceneQuery luceneQuery, final FullTextSession fullTextSession,
      final Analyzer analyzer, final String field) {
    LOG.debug("Executing count: {}", luceneQuery);

    try {
      final FullTextQuery fullTextQuery =
          createFullTextQuery(luceneQuery, fullTextSession, analyzer, field);
      fullTextQuery.setProjection(FullTextQuery.ID, FullTextQuery.DOCUMENT_ID);
      final int resultSize = fullTextQuery.getResultSize();
      LOG.debug("Found {} matches", resultSize);
      return resultSize;
    } catch (final Exception e) {
      LOG.error("Could not execute " + luceneQuery, e);
      return 0;
    }
  }

  public FullTextResult executeQuery(final LuceneQuery luceneQuery,
      final FullTextSession fullTextSession, final Analyzer analyzer, final Class<?> clazz,
      final String field) {
    LOG.debug("Executing query: {}", luceneQuery);

    List<Object[]> queryResult = new LinkedList<Object[]>();
    try {
      final FullTextQuery fullTextQuery =
          createFullTextQuery(luceneQuery, fullTextSession, analyzer, field);
      fullTextQuery.setProjection(FullTextQuery.ID, FullTextQuery.DOCUMENT_ID);
      queryResult = fullTextQuery.list();
      LOG.debug("Found {} matches", queryResult.size());
    } catch (final Exception e) {
      LOG.error("Could not execute " + luceneQuery, e);
      return new FullTextResult();
    }
    // Generate results
    final FullTextResult fullTextResult = new FullTextResult();
    final List<String> queryTerms =
        parseUtil.parseTerms(luceneQuery.getLuceneQueryString(), analyzer, true);
    for (final Object[] fullTextRow : queryResult) {
      final long id = (long) fullTextRow[0];
      final int luceneDocumentId = (int) fullTextRow[1];
      final List<String> matchingTerms =
          getMatchingTerms(fullTextSession, queryTerms, luceneDocumentId, clazz, field);
      // Add to result
      fullTextResult.addFullTextMatch(id, luceneDocumentId, matchingTerms);
    }
    // Done
    return fullTextResult;
  }

  private FullTextQuery createFullTextQuery(final LuceneQuery luceneQuery,
      final FullTextSession fullTextSession, final Analyzer analyzer, final String field)
      throws ParseException {
    BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    final AnalyzingQueryParser queryParser = new AnalyzingQueryParser(field, analyzer);
    final Query query = queryParser.parse(luceneQuery.getLuceneQueryString());
    final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(query);
    fullTextQuery.setProjection(FullTextQuery.ID, FullTextQuery.DOCUMENT_ID);
    return fullTextQuery;
  }

  private List<String> getMatchingTerms(final FullTextSession fullTextSession,
      final Collection<String> queryTerms, final int luceneDocumentId, final Class<?> clazz,
      final String field) {
    // If query was for one term only, this is already the matching term
    if (queryTerms.size() == 1) {
      final List<String> matchingTerms = new ArrayList<String>(1);
      matchingTerms.addAll(queryTerms);
      return matchingTerms;
    }
    // Otherwise, the matching terms must be determined via the index
    // (however, this decreases the performance)
    else {
      final List<String> indexTerms =
          queryUtil.getIndexTerms(fullTextSession, luceneDocumentId, clazz, field);
      final List<String> matchingTerms =
          collectionUtil.getCommonElementsSorted(queryTerms, indexTerms);
      return matchingTerms;

    }
  }

}
