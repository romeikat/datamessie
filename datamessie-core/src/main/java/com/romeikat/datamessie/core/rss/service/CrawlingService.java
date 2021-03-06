package com.romeikat.datamessie.core.rss.service;

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
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;

@Service
public class CrawlingService {

  @Autowired
  @Qualifier("rssCrawlingDao")
  private CrawlingDao crawlingDao;

  public Crawling createCrawling(final StatelessSession statelessSession,
      final LocalDateTime started, final long ProjectId) {
    // Create
    final Crawling crawling = new Crawling();
    crawling.setStarted(started);
    // Associate
    crawling.setProjectId(ProjectId);
    // Insert
    crawlingDao.insert(statelessSession, crawling);
    // Done
    return crawling;
  }

}
