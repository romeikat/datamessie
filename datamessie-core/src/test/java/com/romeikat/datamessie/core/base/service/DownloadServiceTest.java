package com.romeikat.datamessie.core.base.service;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCrawling;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDownload;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource;
import static org.junit.Assert.assertEquals;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Sets;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Source;

public class DownloadServiceTest extends AbstractDbSetupBasedTest {

  private static final String URL_1 = "http://www.url_1.de/";
  private static final String URL_2_1 = "http://www.url_2_1.de/";
  private static final String URL_2_2 = "http://www.url_2_2.de/";

  @Autowired
  private DownloadService downloadService;

  @Override
  protected Operation initDb() {
    final Project project = new Project(1, "Project1", false, false);
    final Crawling crawling = new Crawling(1, project.getId());
    final Source source =
        new Source(1, "Source1", "http://www.source1.de/", null, true, true, false);

    final Document document1 = new Document(1, crawling.getId(), source.getId());
    final Download download1 =
        new Download(1, source.getId(), document1.getId(), true).setUrl(URL_1);

    final Document document2 = new Document(2, crawling.getId(), source.getId());
    final Download download21 =
        new Download(21, source.getId(), document2.getId(), true).setUrl(URL_2_1);
    final Download download22 =
        new Download(22, source.getId(), document2.getId(), true).setUrl(URL_2_2);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, insertIntoProject(project),
        insertIntoCrawling(crawling), insertIntoSource(source), insertIntoDocument(document1),
        insertIntoDownload(download1), insertIntoDocument(document2),
        insertIntoDownload(download21), insertIntoDownload(download22));
  }

  @Test
  public void getDocumentsWithDownloads_singleUrl_singleDocument_singleDownload() {
    final Set<String> urls = Sets.newHashSet(URL_1);
    final Collection<DocumentWithDownloads> documentsWithDownloads =
        downloadService.getDocumentsWithDownloads(sessionProvider.getStatelessSession(), 1, urls);
    assertEquals(1, documentsWithDownloads.size());

    final DocumentWithDownloads documentWithDownloads = documentsWithDownloads.iterator().next();
    assertEquals(1l, documentWithDownloads.getDocumentId());
    assertEquals(Sets.newHashSet(1l), documentWithDownloads.getDownloadIds());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentsWithDownloads_singleUrl_singleDocument_multipleDownloads() {
    final Set<String> urls = Sets.newHashSet(URL_2_1);
    final Collection<DocumentWithDownloads> documentsWithDownloads =
        downloadService.getDocumentsWithDownloads(sessionProvider.getStatelessSession(), 1, urls);
    assertEquals(1, documentsWithDownloads.size());

    final DocumentWithDownloads documentWithDownloads = documentsWithDownloads.iterator().next();
    assertEquals(2l, documentWithDownloads.getDocumentId());
    assertEquals(Sets.newHashSet(21l, 22l), documentWithDownloads.getDownloadIds());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentsWithDownloads_multipleUrls_singleDocument_multipleDownloads() {
    final Set<String> urls = Sets.newHashSet(URL_2_1, URL_2_2);
    final Collection<DocumentWithDownloads> documentsWithDownloads =
        downloadService.getDocumentsWithDownloads(sessionProvider.getStatelessSession(), 1, urls);
    assertEquals(1, documentsWithDownloads.size());

    final DocumentWithDownloads documentWithDownloads = documentsWithDownloads.iterator().next();
    assertEquals(2l, documentWithDownloads.getDocumentId());
    assertEquals(Sets.newHashSet(21l, 22l), documentWithDownloads.getDownloadIds());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentsWithDownloads_multipleUrls_multipleDocuments_multipleDownloads() {
    final Set<String> urls = Sets.newHashSet(URL_1, URL_2_1);
    final List<DocumentWithDownloads> documentsWithDownloads =
        downloadService.getDocumentsWithDownloads(sessionProvider.getStatelessSession(), 1, urls);
    assertEquals(2, documentsWithDownloads.size());

    final DocumentWithDownloads documentWithDownloads1 = documentsWithDownloads.get(0);
    assertEquals(1l, documentWithDownloads1.getDocumentId());
    assertEquals(Sets.newHashSet(1l), documentWithDownloads1.getDownloadIds());

    final DocumentWithDownloads documentWithDownloads2 = documentsWithDownloads.get(1);
    assertEquals(2l, documentWithDownloads2.getDocumentId());
    assertEquals(Sets.newHashSet(21l, 22l), documentWithDownloads2.getDownloadIds());

    dbSetupTracker.skipNextLaunch();
  }

}
