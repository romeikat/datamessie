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
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.domain.dto.CrawlingDto;
import com.romeikat.datamessie.core.domain.dto.CrawlingOverviewDto;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;

@Repository
public class CrawlingDao extends AbstractEntityWithIdAndVersionDao<Crawling> {

  public CrawlingDao() {
    super(Crawling.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "started";
  }

  public List<Crawling> getForProject(final SharedSessionContract ssc, final long projectId) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    crawlingQuery.addRestriction(Restrictions.eq("projectId", projectId));
    crawlingQuery.addOrder(Order.desc("started"));

    // Done
    final List<Crawling> objects = crawlingQuery.listObjects(ssc);
    return objects;
  }

  public Map<Document, Crawling> getForDocuments(final SharedSessionContract ssc,
      final Collection<Document> documents) {
    final Set<Long> crawlingIds = documents.stream().map(d -> d.getCrawlingId()).collect(Collectors.toSet());
    final Map<Long, Crawling> crawlingsById = getIdsWithEntities(ssc, crawlingIds);

    final Map<Document, Crawling> result = Maps.newHashMapWithExpectedSize(documents.size());
    for (final Document document : documents) {
      final Crawling crawling = crawlingsById.get(document.getCrawlingId());
      result.put(document, crawling);
    }
    return result;
  }

  public long getCount(final SharedSessionContract ssc, final long projectId) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    crawlingQuery.addRestriction(Restrictions.eq("projectId", projectId));

    // Done
    final long count = crawlingQuery.count(ssc);
    return count;
  }

  public List<CrawlingDto> getAsDtos(final SharedSessionContract ssc, final Long projectId) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    if (projectId != null) {
      crawlingQuery.addRestriction(Restrictions.eq("projectId", projectId));
    }
    crawlingQuery.addOrder(Order.desc("started"));
    crawlingQuery.setResultTransformer(new AliasToBeanResultTransformer(CrawlingDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.groupProperty("id"), "id");
    projectionList.add(Projections.property("started"), "started");
    projectionList.add(Projections.property("completed"), "completed");
    @SuppressWarnings("unchecked")
    final List<CrawlingDto> dtos = (List<CrawlingDto>) crawlingQuery.listForProjection(ssc, projectionList);

    // Set duration
    setDuration(dtos);

    return dtos;
  }

  public List<CrawlingOverviewDto> getAsOverviewDtos(final SharedSessionContract ssc, final Long projectId) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    if (projectId != null) {
      crawlingQuery.addRestriction(Restrictions.eq("projectId", projectId));
    }
    crawlingQuery.addOrder(Order.desc("started"));
    crawlingQuery.setResultTransformer(new AliasToBeanResultTransformer(CrawlingOverviewDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.groupProperty("id"), "id");
    projectionList.add(Projections.property("started"), "started");
    @SuppressWarnings("unchecked")
    final List<CrawlingOverviewDto> dtos =
        (List<CrawlingOverviewDto>) crawlingQuery.listForProjection(ssc, projectionList);

    return dtos;
  }

  private void setDuration(final List<CrawlingDto> crawlings) {
    for (final CrawlingDto crawling : crawlings) {
      setDuration(crawling);
    }
  }

  private void setDuration(final CrawlingDto crawling) {
    if (crawling.getStarted() == null || crawling.getCompleted() == null) {
      return;
    }
    final Duration duration = Duration.between(crawling.getStarted(), crawling.getCompleted());
    crawling.setDuration(duration);
  }

}
