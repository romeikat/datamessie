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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.FullTextSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.util.CollectionUtil;
import com.romeikat.datamessie.core.base.util.ParseUtil;

@Service
public class QueryUtil {

  private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

  @Autowired
  private ParseUtil parseUtil;

  @Autowired
  private CollectionUtil collectionUtil;

  public FullTextQuery parseQuery(final String luceneQueryString, final Analyzer analyzer) {
    LOG.debug("Parsing query: {}", luceneQueryString);
    // Check if query is "n outof abc def ghi"
    final Pattern pattern = Pattern.compile("\\s*(\\d+)\\s+outof\\s+(.*)",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    final Matcher matcher = pattern.matcher(luceneQueryString);
    // Match => OUTOF query
    if (matcher.matches()) {
      final int k = Integer.parseInt(matcher.group(1));
      final String searchString = matcher.group(2);
      final List<String> queryTerms = parseUtil.parseTerms(searchString, analyzer, true);
      final OutOfQuery query = new OutOfQuery(k, queryTerms);
      LOG.debug("Detected {}", query);
      return query;
    }
    // No match => Lucene query
    else {
      final LuceneQuery query = new LuceneQuery(luceneQueryString);
      LOG.debug("Detected {}", query);
      return query;
    }
  }

  public LuceneQuery toLuceneQuery(final OutOfQuery query, final Analyzer analyzer) {
    // Generate query string
    final List<List<String>> permutations =
        collectionUtil.fromNChooseK(query.getQueryTerms(), query.getK());
    final List<String> permutationStrings = new ArrayList<String>(permutations.size());
    for (final List<String> permutation : permutations) {
      if (!permutation.isEmpty()) {
        final String permutationString = "(" + StringUtils.join(permutation, " AND ") + ")";
        permutationStrings.add(permutationString);
      }
    }
    final String luceneQueryString = StringUtils.join(permutationStrings, " OR ");
    // Done
    LOG.debug("Transformed OutOf query into Lucene query: {}", luceneQueryString);
    return new LuceneQuery(luceneQueryString);
  }

  public List<String> getIndexTerms(final FullTextSession fullTextSession,
      final int luceneDocumentId, final Class<?> clazz, final String field) {
    final IndexReader indexReader =
        fullTextSession.getSearchFactory().getIndexReaderAccessor().open(clazz);
    try {
      final Terms terms = indexReader.getTermVector(luceneDocumentId, field);
      final List<String> termsList = Lists.newArrayListWithExpectedSize((int) terms.size());

      final TermsEnum termsEnum = terms.iterator();
      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        final String term = text.utf8ToString();
        termsList.add(term);
      }

      return termsList;
    } catch (final IOException e) {
      LOG.error("Could not determine index terms", e);
      return null;
    }
  }

}
