package com.romeikat.datamessie.core.base.service;

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
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.CrawlingDao;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.rss.init.RssCrawlingInitializer;

@Service
public class ProjectService {

  @Autowired
  @Qualifier("projectDao")
  private ProjectDao projectDao;

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;

  @Autowired
  @Qualifier("crawlingDao")
  private CrawlingDao crawlingDao;

  @Autowired
  private RssCrawlingInitializer rssCrawlingInitializer;

  public Long getProjectId(final SharedSessionContract ssc, final Document document) {
    if (document == null) {
      return null;
    }

    final long crawlingId = document.getCrawlingId();
    final Crawling crawling = crawlingDao.getEntity(ssc, crawlingId);
    if (crawling == null) {
      return null;
    }

    return crawling.getProjectId();
  }

  public void createProject(final StatelessSession statelessSession, final ProjectDto projectDto) {
    // Create
    final Project project = new Project();
    project.setName(projectDto.getName());
    project.setCrawlingEnabled(projectDto.getCrawlingEnabled());
    project.setCrawlingInterval(projectDto.getCrawlingInterval());
    project.setPreprocessingEnabled(projectDto.getPreprocessingEnabled());
    project.setCleaningMethod(projectDto.getCleaningMethod());
    // Insert
    final Long projectId = projectDao.insert(statelessSession, project);
    // Schedule crawling
    if (projectId != null) {
      rssCrawlingInitializer.scheduleCrawling(projectId);
    }
  }

  public void updateProject(final StatelessSession statelessSession, final ProjectDto projectDto) {
    // Get
    final Project project = projectDao.getEntity(statelessSession, projectDto.getId());
    // Update
    project.setName(projectDto.getName());
    project.setCrawlingEnabled(projectDto.getCrawlingEnabled());
    project.setCrawlingInterval(projectDto.getCrawlingInterval());
    project.setPreprocessingEnabled(projectDto.getPreprocessingEnabled());
    project.setCleaningMethod(projectDto.getCleaningMethod());
    projectDao.update(statelessSession, project);
  }

}
