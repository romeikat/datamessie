package com.romeikat.datamessie.core.base.util;

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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LuceneQueryUtil {

  private static final String FIELD = "cleanedContent";

  @Autowired
  private CollectionUtil collectionUtil;

  public Query getAndQuery(final Query... queries) {
    if (queries.length == 0) {
      return new MatchAllDocsQuery();
    }

    final BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (final Query query : queries) {
      builder.add(query, BooleanClause.Occur.MUST);
    }
    return builder.build();
  }

  public Query getOrQuery(final Query... queries) {
    if (queries.length == 0) {
      return new MatchAllDocsQuery();
    }

    final BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (final Query query : queries) {
      builder.add(query, BooleanClause.Occur.SHOULD);
    }
    return builder.build();
  }

  public Query getTermQuery(final String term) {
    if (term == null) {
      return new MatchAllDocsQuery();
    }

    final TermQuery termQuery = new TermQuery(new Term(FIELD, term));
    return termQuery;
  }

  public Query getTermQuery(final Collection<String> terms) {
    if (terms.isEmpty()) {
      return new MatchAllDocsQuery();
    }

    final BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (final String term : terms) {
      if (term == null) {
        continue;
      }

      final Query termQuery = getTermQuery(term);
      builder.add(termQuery, BooleanClause.Occur.MUST);
    }
    return builder.build();
  }

  public Query getProximityQuery(final String term1, final String term2, final Integer slop) {
    if (term1 == null || term2 == null) {
      return new MatchAllDocsQuery();
    }

    final PhraseQuery.Builder builder = new PhraseQuery.Builder();
    builder.add(new Term(FIELD, term1));
    builder.add(new Term(FIELD, term2));
    builder.setSlop(slop == null ? Integer.MAX_VALUE : slop);
    return builder.build();
  }

  public Query getProximityQuery(final Collection<String> term1Variants,
      final Collection<String> term2Variants, final Integer slop) {
    if (term1Variants.isEmpty() || term2Variants.isEmpty()) {
      return new MatchAllDocsQuery();
    }

    final BooleanQuery.Builder builder = new BooleanQuery.Builder();
    final Collection<Pair<String, String>> variantsCombinations =
        collectionUtil.getPairs(term1Variants, term2Variants);
    for (final Pair<String, String> variantsCombination : variantsCombinations) {
      final PhraseQuery.Builder builder2 = new PhraseQuery.Builder();
      builder2.add(new Term(FIELD, variantsCombination.getLeft()));
      builder2.add(new Term(FIELD, variantsCombination.getRight()));
      builder2.setSlop(slop == null ? Integer.MAX_VALUE : slop);

      builder.add(builder2.build(), BooleanClause.Occur.MUST);
    }

    return builder.build();
  }

}
