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

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static org.junit.Assert.assertEquals;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.SortedMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import jersey.repackaged.com.google.common.collect.Lists;

public class DocumentDaoTest extends AbstractDbSetupBasedTest {

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;


  @Override
  protected Operation initDb() {
    final Project project = new Project(1, "Project1", false, false);
    final Source source = new Source(1, "Source1", "http://www.source1.de/", true, false);
    final Crawling crawling = new Crawling(1, project.getId());

    final LocalDateTime downloaded1 = LocalDate.of(2019, 11, 1).atStartOfDay();
    final DocumentProcessingState state1 = DocumentProcessingState.REDIRECTED;
    final Document document1 = new Document(1, crawling.getId(), source.getId())
        .setDownloaded(downloaded1).setState(state1);

    final LocalDateTime downloaded2 = LocalDate.of(2019, 11, 2).atStartOfDay();
    final DocumentProcessingState state2 = DocumentProcessingState.CLEANED;
    final Document document2 = new Document(2, crawling.getId(), source.getId())
        .setDownloaded(downloaded2).setState(state2);

    final LocalDateTime downloaded3 = LocalDate.of(2019, 11, 3).atStartOfDay();
    final DocumentProcessingState state3 = DocumentProcessingState.STEMMED;
    final Document document3 = new Document(3, crawling.getId(), source.getId())
        .setDownloaded(downloaded3).setState(state3);

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, insertIntoDocument(document1),
        insertIntoDocument(document2), insertIntoDocument(document3));
  }

  @Test
  public void getDownloadedDatesWithDocuments() {
    final List<DocumentProcessingState> states =
        Lists.newArrayList(DocumentProcessingState.CLEANED, DocumentProcessingState.STEMMED);
    final SortedMap<LocalDate, Long> downloadedDates =
        documentDao.getDownloadedDatesWithNumberOfDocuments(sessionProvider.getStatelessSession(),
            LocalDate.of(2019, 11, 1), states, Lists.newArrayList(1l));
    assertEquals(2, downloadedDates.size());
    assertEquals(Long.valueOf(1), downloadedDates.get(LocalDate.of(2019, 11, 2)));
    assertEquals(Long.valueOf(1), downloadedDates.get(LocalDate.of(2019, 11, 3)));

    dbSetupTracker.skipNextLaunch();
  }

}
