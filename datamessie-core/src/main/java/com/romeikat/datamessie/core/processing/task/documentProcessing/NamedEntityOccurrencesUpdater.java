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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.util.IdBasedMap;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;

@Service
public class NamedEntityOccurrencesUpdater {

  @Autowired
  @Qualifier("namedEntityDao")
  private NamedEntityDao namedEntityDao;

  @Autowired
  @Qualifier("namedEntityOccurrenceDao")
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  public Set<NamedEntityOccurrence> updateNamedEntityOccurrences(
      final StatelessSession statelessSession, final long documentId,
      final Collection<NamedEntityDetectionDto> namedEntityDetections) {
    // Delete old named entity occurrences
    deleteNamedEntityOccurrences(statelessSession, documentId);
    if (namedEntityDetections == null) {
      return null;
    }

    // Transform each detection into an occurrence
    final Collection<NamedEntityOccurrence> namedEntityOccurrences =
        createNamedEntityOccurrences(statelessSession, documentId, namedEntityDetections);
    // Save new named entity occurrences
    final IdBasedMap<NamedEntityOccurrence> savedNamedEntityOccurrences =
        saveNamedEntityOccurrences(statelessSession, documentId, namedEntityOccurrences);
    return savedNamedEntityOccurrences.values();
  }

  private void deleteNamedEntityOccurrences(final StatelessSession statelessSession,
      final long documentId) {
    final List<NamedEntityOccurrence> oldNamedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(statelessSession, documentId);
    for (final NamedEntityOccurrence oldNamedEntityOccurrence : oldNamedEntityOccurrences) {
      namedEntityOccurrenceDao.delete(statelessSession, oldNamedEntityOccurrence);
    }
  }

  private Collection<NamedEntityOccurrence> createNamedEntityOccurrences(
      final StatelessSession statelessSession, final long documentId,
      final Collection<NamedEntityDetectionDto> namedEntityDetections) {
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        new ArrayList<NamedEntityOccurrence>();
    for (final NamedEntityDetectionDto namedEntityDetection : namedEntityDetections) {
      // Transform detection
      final long namedEntityId =
          namedEntityDao.getOrCreate(statelessSession, namedEntityDetection.getName());
      final long parentNamedEntityId =
          namedEntityDao.getOrCreate(statelessSession, namedEntityDetection.getParentName());
      final NamedEntityType type = namedEntityDetection.getType();
      final Integer quantity = namedEntityDetection.getQuantity();
      // Create occurrence
      final NamedEntityOccurrence namedEntityOccurrence = new NamedEntityOccurrence();
      namedEntityOccurrence.setNamedEntityId(namedEntityId);
      namedEntityOccurrence.setParentNamedEntityId(parentNamedEntityId);
      namedEntityOccurrence.setType(type);
      namedEntityOccurrence.setQuantity(quantity);
      namedEntityOccurrence.setDocumentId(documentId);
      namedEntityOccurrences.add(namedEntityOccurrence);
    }
    return namedEntityOccurrences;
  }

  private IdBasedMap<NamedEntityOccurrence> saveNamedEntityOccurrences(
      final StatelessSession statelessSession, final long documentId,
      final Collection<NamedEntityOccurrence> namedEntityOccurrences) {
    final IdBasedMap<NamedEntityOccurrence> savedNamedEntityOccurrences = new IdBasedMap<>();

    // Create new named entity occurrences
    for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
      final NamedEntityOccurrence savedNamedEntityOccurrence =
          saveNamedEntityOccurrence(statelessSession, documentId, namedEntityOccurrence);
      savedNamedEntityOccurrences.add(savedNamedEntityOccurrence);
    }

    return savedNamedEntityOccurrences;
  }

  private NamedEntityOccurrence saveNamedEntityOccurrence(final StatelessSession statelessSession,
      final long documentId, final NamedEntityOccurrence namedEntityOccurrence) {
    // Two NamedEntityOccurrences might be considered the same by the database (due to its
    // collation), so we ensure uniqueness for each NamedEntityOccurrence explicitly

    final long namedEntityId = namedEntityOccurrence.getNamedEntityId();
    final NamedEntityType type = namedEntityOccurrence.getType();
    final int quantity = namedEntityOccurrence.getQuantity();
    final NamedEntityOccurrence existingNamedEntityOccurrence = namedEntityOccurrenceDao
        .getByNamedEntityAndTypeAndDocument(statelessSession, namedEntityId, type, documentId);
    // New occurrence
    if (existingNamedEntityOccurrence == null) {
      namedEntityOccurrenceDao.insert(statelessSession, namedEntityOccurrence);
      return namedEntityOccurrence;
    }
    // Existing occurrence
    else {
      final int oldQuantity = existingNamedEntityOccurrence.getQuantity();
      final int newQuantity = oldQuantity + quantity;
      existingNamedEntityOccurrence.setQuantity(newQuantity);
      namedEntityOccurrenceDao.update(statelessSession, existingNamedEntityOccurrence);
      return existingNamedEntityOccurrence;
    }
  }

}
