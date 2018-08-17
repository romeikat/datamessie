package com.romeikat.datamessie.core.processing.task.documentProcessing;

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
import static com.romeikat.datamessie.core.CommonOperations.insertIntoCrawling;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoDocument;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoProject;
import static com.romeikat.datamessie.core.CommonOperations.insertIntoSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.AbstractDbSetupBasedTest;
import com.romeikat.datamessie.core.CommonOperations;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;

public class NamedEntityOccurrencesUpdaterTest extends AbstractDbSetupBasedTest {

  @Autowired
  private NamedEntityOccurrencesUpdater namedEntityOccurrencesUpdater;

  @Autowired
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  private NamedEntityDao namedEntityDao;

  @Override
  protected Operation initDb() {
    final Project project1 = new Project(1, "Project1", false, false);
    final Source source1 = new Source(1, "Source1", "http://www.source1.de/", true);
    final Crawling crawling1 = new Crawling(1, project1.getId());
    final Document document1 = new Document(1, crawling1.getId(), source1.getId());

    return sequenceOf(CommonOperations.DELETE_ALL_FOR_DATAMESSIE,
        sequenceOf(insertIntoProject(project1), insertIntoSource(source1),
            insertIntoCrawling(crawling1), insertIntoDocument(document1)));
  }

  @Test
  public void updateNamedEntityOccurrences() {
    final NamedEntityDetectionDto locationDetection1 = new NamedEntityDetectionDto().setName("L1")
        .setParentName("L1 L2 L3").setType(NamedEntityType.LOCATION).setQuantity(1);
    final NamedEntityDetectionDto locationDetection2 = new NamedEntityDetectionDto().setName("L2")
        .setParentName("L1 L2 L3").setType(NamedEntityType.LOCATION).setQuantity(2);
    final NamedEntityDetectionDto locationDetection123 =
        new NamedEntityDetectionDto().setName("L1 L2 L3").setParentName("L1 L2 L3")
            .setType(NamedEntityType.LOCATION).setQuantity(123);
    final NamedEntityDetectionDto personDetection1 = new NamedEntityDetectionDto().setName("P1")
        .setParentName("P1").setType(NamedEntityType.PERSON).setQuantity(1);
    final Collection<NamedEntityDetectionDto> namedEntityDetections = Lists.newArrayList(
        locationDetection1, locationDetection2, locationDetection123, personDetection1);

    // Update
    namedEntityOccurrencesUpdater.updateNamedEntityOccurrences(
        sessionProvider.getStatelessSession(), 1l, namedEntityDetections);

    // Named entities are created
    final NamedEntity location1 = namedEntityDao.get(sessionProvider.getStatelessSession(), "L1");
    final NamedEntity location2 = namedEntityDao.get(sessionProvider.getStatelessSession(), "L2");
    final NamedEntity location123 =
        namedEntityDao.get(sessionProvider.getStatelessSession(), "L1 L2 L3");
    final NamedEntity person1 = namedEntityDao.get(sessionProvider.getStatelessSession(), "P1");
    assertNotNull(location1);
    assertNotNull(location2);
    assertNotNull(location123);
    assertNotNull(person1);

    // Occurrences are created
    final Collection<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(sessionProvider.getStatelessSession(), 1l);
    assertEquals(4, namedEntityOccurrences.size());

    final Map<Long, NamedEntityOccurrence> namedEntityOccurrencesByNamedEntityId =
        namedEntityOccurrences.stream()
            .collect(Collectors.toMap(neo -> neo.getNamedEntityId(), neo -> neo));
    final NamedEntityOccurrence location1Occurrence =
        namedEntityOccurrencesByNamedEntityId.get(location1.getId());
    final NamedEntityOccurrence location2Occurrence =
        namedEntityOccurrencesByNamedEntityId.get(location2.getId());
    final NamedEntityOccurrence location123Occurrence =
        namedEntityOccurrencesByNamedEntityId.get(location123.getId());
    final NamedEntityOccurrence person1Occurrence =
        namedEntityOccurrencesByNamedEntityId.get(person1.getId());

    // Occurrences are counted and assigned
    assertEquals(NamedEntityType.LOCATION, location1Occurrence.getType());
    assertEquals(1, location1Occurrence.getQuantity());
    assertEquals(location123.getId(), location1Occurrence.getParentNamedEntityId());
    assertEquals(NamedEntityType.LOCATION, location2Occurrence.getType());
    assertEquals(2, location2Occurrence.getQuantity());
    assertEquals(location123.getId(), location2Occurrence.getParentNamedEntityId());
    assertEquals(NamedEntityType.LOCATION, location123Occurrence.getType());
    assertEquals(123, location123Occurrence.getQuantity());
    assertEquals(location123.getId(), location123Occurrence.getParentNamedEntityId());
    assertEquals(NamedEntityType.PERSON, person1Occurrence.getType());
    assertEquals(1, person1Occurrence.getQuantity());
    assertEquals(person1.getId(), person1Occurrence.getParentNamedEntityId());
  }

}
