package com.romeikat.datamessie.core.rss.dao;

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
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityQuery;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;

@Repository("rssCrawlingDao")
public class CrawlingDao extends com.romeikat.datamessie.core.base.dao.impl.CrawlingDao {

  public LocalDateTime getStartOfLatestCompletedCrawling(final SharedSessionContract ssc,
      final long projectId) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    crawlingQuery.addRestriction(Restrictions.eq("projectId", projectId));
    crawlingQuery.addRestriction(Restrictions.isNotNull("started"));
    crawlingQuery.addRestriction(Restrictions.isNotNull("completed"));

    // Done
    final Projection projection = Projections.max("started");
    final LocalDateTime maxCompleted =
        (LocalDateTime) crawlingQuery.uniqueForProjection(ssc, projection);
    return maxCompleted;
  }

  public List<Crawling> getIncompleted(final SharedSessionContract ssc) {
    // Query: Crawling
    final EntityQuery<Crawling> crawlingQuery = new EntityQuery<>(Crawling.class);
    crawlingQuery.addRestriction(Restrictions.isNull("completed"));
    crawlingQuery.addOrder(Order.asc("started"));

    // Done
    final List<Crawling> crawlings = crawlingQuery.listObjects(ssc);
    return crawlings;
  }

}
