package com.romeikat.datamessie.core.processing.task.documentProcessing.namedEntities;

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
import java.util.Map;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.domain.entity.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;

public class NamedEntityOccurrencesCreator {

  private final NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  public NamedEntityOccurrencesCreator(final ApplicationContext ctx) {
    namedEntityOccurrenceDao = ctx.getBean(NamedEntityOccurrenceDao.class);
  }

  public List<NamedEntityOccurrence> createNamedEntityOccurrences(final long documentId,
      final Collection<NamedEntityDetectionDto> namedEntityDetections,
      final Map<String, Long> namedEntityNames2NamedEntityId) {
    if (namedEntityDetections == null) {
      return null;
    }

    final List<NamedEntityOccurrence> namedEntityOccurrences =
        new ArrayList<NamedEntityOccurrence>();
    for (final NamedEntityDetectionDto namedEntityDetection : namedEntityDetections) {
      final String namedEntityName = namedEntityDetection.getName();
      final String namedEntityParentName = namedEntityDetection.getParentName();
      if (namedEntityName == null || namedEntityParentName == null) {
        continue;
      }

      final Long namedEntityId = namedEntityNames2NamedEntityId.get(namedEntityName);
      final Long parentNamedEntityId = namedEntityNames2NamedEntityId.get(namedEntityParentName);
      if (namedEntityId == null || parentNamedEntityId == null) {
        continue;
      }

      // Transform detection
      final NamedEntityType type = namedEntityDetection.getType();
      final Integer quantity = namedEntityDetection.getQuantity();
      // Create occurrence
      final NamedEntityOccurrence namedEntityOccurrence = namedEntityOccurrenceDao.create();
      namedEntityOccurrence.setNamedEntityId(namedEntityId);
      namedEntityOccurrence.setParentNamedEntityId(parentNamedEntityId);
      namedEntityOccurrence.setType(type);
      namedEntityOccurrence.setQuantity(quantity);
      namedEntityOccurrence.setDocumentId(documentId);
      namedEntityOccurrences.add(namedEntityOccurrence);
    }
    return namedEntityOccurrences;
  }

}
