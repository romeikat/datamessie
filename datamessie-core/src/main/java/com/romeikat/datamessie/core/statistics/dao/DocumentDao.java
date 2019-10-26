package com.romeikat.datamessie.core.statistics.dao;

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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.impl.DocumentImpl;
import com.romeikat.datamessie.core.statistics.dto.DocumentStatisticsDto;

@Repository("statisticsDocumentDao")
public class DocumentDao extends com.romeikat.datamessie.core.base.dao.impl.DocumentDao {

  public List<DocumentStatisticsDto> getAsDocumentStatisticsDtos(final SharedSessionContract ssc,
      final Long sourceId, final LocalDate published) {
    final LocalDateTime minPublished =
        published == null ? null : LocalDateTime.of(published, LocalTime.MIDNIGHT);
    final LocalDateTime maxPublished =
        published == null ? null : LocalDateTime.of(published.plusDays(1), LocalTime.MIDNIGHT);

    // Query: Document
    final EntityWithIdQuery<Document> documentQuery = new EntityWithIdQuery<>(DocumentImpl.class);
    if (sourceId != null) {
      documentQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    }
    if (minPublished != null) {
      documentQuery.addRestriction(Restrictions.ge("published", minPublished));
    }
    if (maxPublished != null) {
      documentQuery.addRestriction(Restrictions.lt("published", maxPublished));
    }
    documentQuery
        .setResultTransformer(new AliasToBeanResultTransformer(DocumentStatisticsDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "documentId");
    projectionList.add(Projections.property("sourceId"), "sourceId");
    projectionList.add(Projections.property("published"), "published");
    projectionList.add(Projections.property("state"), "state");
    @SuppressWarnings("unchecked")
    final List<DocumentStatisticsDto> dtos =
        (List<DocumentStatisticsDto>) documentQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

}
