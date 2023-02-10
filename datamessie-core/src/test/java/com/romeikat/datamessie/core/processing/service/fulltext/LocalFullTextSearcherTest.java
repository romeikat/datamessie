package com.romeikat.datamessie.core.processing.service.fulltext;

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
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCleanedContent;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCrawling;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject2Source;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.util.fullText.FullTextResult;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.processing.app.shared.LocalFullTextSearcher;

public class LocalFullTextSearcherTest extends AbstractDbSetupBasedTest {

  @Autowired
  private LocalFullTextSearcher localFullTextSearcher;

  @Override
  protected Operation initDb() {
    final Source source1 =
        new Source(1, "Source1", "http://www.source1.de/", null, null, true, true, false);
    final Project project1 = new Project(1, "Project1", false, false);
    final Project2Source project2Source = new Project2Source(1, 1);
    final Crawling crawling1 = new Crawling(1, project1.getId());
    final Document document1 = new Document(1, crawling1.getId(), source1.getId());
    final Document document2 = new Document(2, crawling1.getId(), source1.getId());
    final CleanedContent cleanedContent1 = new CleanedContent(document1.getId(), "Foo is good");
    final CleanedContent cleanedContent2 = new CleanedContent(document2.getId(), "Bar is better");

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE, insertIntoSource(source1),
        insertIntoProject(project1), insertIntoProject2Source(project2Source),
        insertIntoCrawling(crawling1), insertIntoDocument(document1), insertIntoDocument(document2),
        insertIntoCleanedContent(cleanedContent1), insertIntoCleanedContent(cleanedContent2));
  }

  @Test
  public void getDocumentIds_withLuceneQuery_allResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "is";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(2, documentIds.size());
    assertTrue(documentIds.getIds().contains(1l));
    assertTrue(documentIds.getIds().contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentIds_withLuceneQuery_someResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "Foo";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(1, documentIds.size());
    assertTrue(documentIds.getIds().contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentIds_withLuceneQuery_noResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "whatsup";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(0, documentIds.size());

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentIds_withOutOfQuery_allResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "1 OUTOF foo is good";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(2, documentIds.size());
    assertTrue(documentIds.getIds().contains(1l));
    assertTrue(documentIds.getIds().contains(2l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentIds_withOutOfQuery_someResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "2 OUTOF foo is good";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(1, documentIds.size());
    assertTrue(documentIds.getIds().contains(1l));

    dbSetupTracker.skipNextLaunch();
  }

  @Test
  public void getDocumentIds_withOutOfQuery_noResults() throws Exception {
    rebuildDataMessieIndex();

    final String luceneQueryString = "3 OUTOF foo is better";

    final FullTextResult documentIds =
        localFullTextSearcher.searchForCleanedContent(luceneQueryString);
    assertEquals(0, documentIds.size());

    dbSetupTracker.skipNextLaunch();
  }

}
