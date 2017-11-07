package com.romeikat.datamessie.core.base.dao.impl;

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
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.util.sparsetable.Cell;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsSparseTable;
import com.romeikat.datamessie.core.domain.entity.impl.Statistics;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;

@Repository
public class StatisticsDao extends AbstractEntityWithIdAndVersionDao<Statistics> {

  public StatisticsDao() {
    super(Statistics.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return null;
  }

  public int deleteStatistics(final SharedSessionContract ssc, final Long sourceId,
      final LocalDate published) {
    if (sourceId == null && published == null) {
      return 0;
    }

    // Query
    final StringBuilder hql = new StringBuilder();
    hql.append("DELETE Statistics s ");
    hql.append("WHERE ");
    if (sourceId != null) {
      hql.append("s.sourceId = :_sourceId ");
    }
    if (sourceId != null && published != null) {
      hql.append("AND ");
    }
    if (published != null) {
      hql.append("s.published = :_published ");
    }
    final Query<?> query = ssc.createQuery(hql.toString());
    if (sourceId != null) {
      query.setParameter("_sourceId", sourceId);
    }
    if (published != null) {
      query.setParameter("_published", published);
    }

    // Execute
    final int updated = query.executeUpdate();
    return updated;
  }

  public void saveStatistics(final StatelessSession statelessSession,
      final StatisticsSparseTable statistics) {
    for (final Cell<Long, LocalDate, DocumentsPerState> cell : statistics.getCells()) {
      final long sourceId = cell.getRowHeader();
      final LocalDate published = cell.getColumnHeader();
      final DocumentsPerState documentsPerState = cell.getValue();
      for (final DocumentProcessingState state : documentsPerState.getStates()) {
        final long documents = documentsPerState.get(state);
        saveStatistics(statelessSession, sourceId, published, state, documents);
      }
    }
  }

  private void saveStatistics(final StatelessSession statelessSession, final long sourceId,
      final LocalDate published, final DocumentProcessingState state, final long documents) {
    if (documents == 0) {
      return;
    }

    final Statistics statistics = new Statistics();
    statistics.setSourceId(sourceId);
    statistics.setPublished(published);
    statistics.setState(state);
    statistics.setDocuments(documents);
    insert(statelessSession, statistics);
  }

  public StatisticsSparseTable getStatistics(final SharedSessionContract ssc,
      final Collection<Long> sourceIds, final LocalDate published) {
    if (CollectionUtils.isEmpty(sourceIds) || published == null) {
      return new StatisticsSparseTable();
    }

    // Query
    final StringBuilder hql = new StringBuilder();
    hql.append("SELECT s.sourceId, s.state, documents ");
    hql.append("FROM Statistics s ");
    hql.append("WHERE s.sourceId IN :_sourceIds ");
    hql.append("AND s.published = :_published ");
    hql.append("GROUP BY s.sourceId, s.state ");
    final Query<Object[]> query = ssc.createQuery(hql.toString(), Object[].class);
    query.setParameterList("_sourceIds", sourceIds);
    query.setParameter("_published", published);

    // Execute
    final List<Object[]> records = query.list();

    // Postprocess
    final StatisticsSparseTable statistics = new StatisticsSparseTable();
    for (final Object[] record : records) {
      final long sourceId = (Long) record[0];
      final DocumentProcessingState state = (DocumentProcessingState) record[1];
      final long number = (Long) record[2];

      final DocumentsPerState documentsForState = new DocumentsPerState();
      documentsForState.put(state, number);
      statistics.putValue(sourceId, published, documentsForState);
    }

    // Done
    return statistics;
  }

}
