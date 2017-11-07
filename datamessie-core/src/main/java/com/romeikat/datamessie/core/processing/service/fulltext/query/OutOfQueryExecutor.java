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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.FullTextSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.Counter;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;

@Service
public class OutOfQueryExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(OutOfQueryExecutor.class);

  @Autowired
  private LuceneQueryExecutor luceneQueryExecutor;

  public FullTextResult executeQuery(final OutOfQuery outOfQuery,
      final FullTextSession fullTextSession, final Analyzer analyzer, final Class<?> clazz,
      final String field) {
    // Old solution: transform into Lucene query
    // final LuceneQuery luceneQuery = parseUtil.toLuceneQuery(outOfQuery, analyzer);
    // return execute(luceneQuery, fullTextSession, analyzer);

    LOG.debug("Executing {}", outOfQuery);
    final Map<String, FullTextResult> queryTermResults =
        new HashMap<String, FullTextResult>(outOfQuery.getQueryTerms().size());
    // Execute a separate Lucene query for each term
    LOG.debug("Executing separate LuceneQueries for {} terms", outOfQuery.getQueryTerms().size());
    for (final String queryTerm : outOfQuery.getQueryTerms()) {
      final LuceneQuery luceneQuery = new LuceneQuery(queryTerm);
      final FullTextResult queryTermResult =
          luceneQueryExecutor.executeQuery(luceneQuery, fullTextSession, analyzer, clazz, field);
      queryTermResults.put(queryTerm, queryTermResult);
    }
    // For each ID found, determine how many terms occur in it
    LOG.debug("Merging query results");
    final Counter<Long> counter = new Counter<Long>();
    for (final FullTextResult fullTextResult : queryTermResults.values()) {
      for (final Long id : fullTextResult.getIds()) {
        counter.count(id);
      }
    }
    // Filter IDs that occur in at least k of the queries
    final Set<Long> ids = counter.getObjectsWithNumberOrAbove(outOfQuery.getK());
    // Merge results wrt. these IDs
    final FullTextResult mergedResult = mergeFullTextResults(queryTermResults.values(), ids);
    LOG.debug("Merged to {} matches", mergedResult.size());
    // Done
    return mergedResult;
  }

  private FullTextResult mergeFullTextResults(
      final Collection<FullTextResult> singleFullTextResults, final Collection<Long> ids) {
    final FullTextResult mergedResult = new FullTextResult();
    // Process the single results
    for (final FullTextResult singleFullTextResult : singleFullTextResults) {
      // Process the desired IDs
      for (final long id : ids) {
        final Long luceneDocumentId = singleFullTextResult.getLuceneDocumentId(id);
        final List<String> matchingTerms = singleFullTextResult.getMatchingTerms(id);
        // If the single result contains the desired ID, add
        if (luceneDocumentId != null && matchingTerms != null) {
          mergedResult.addFullTextMatch(id, luceneDocumentId, matchingTerms);
        }
      }
    }
    // Done
    return mergedResult;
  }

}
