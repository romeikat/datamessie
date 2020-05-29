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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SharedSessionContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityCategoryDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityDao;
import com.romeikat.datamessie.core.base.dao.impl.NamedEntityOccurrenceDao;
import com.romeikat.datamessie.core.base.util.comparator.CollatorComparator;
import com.romeikat.datamessie.core.domain.dto.NamedEntityDto;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;

@Service
public class NamedEntityService {

  private static final Logger LOG = LoggerFactory.getLogger(NamedEntityService.class);

  @Autowired
  @Qualifier("namedEntityDao")
  private NamedEntityDao namedEntityDao;

  @Autowired
  @Qualifier("namedEntityOccurrenceDao")
  private NamedEntityOccurrenceDao namedEntityOccurrenceDao;

  @Autowired
  @Qualifier("namedEntityCategoryDao")
  private NamedEntityCategoryDao namedEntityCategoryDao;

  public List<NamedEntityDto> getAsDtosByDocument(final SharedSessionContract ssc,
      final long documentId) {
    final List<NamedEntityDto> dtos = new ArrayList<NamedEntityDto>();
    // Determine named entity occurrences for the document
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        namedEntityOccurrenceDao.getByDocument(ssc, documentId);

    // Load named entities
    final Map<Long, NamedEntity> namedEntitiesById =
        namedEntityDao.loadForNamedEntityOccurrences(ssc, namedEntityOccurrences);

    // Process each occurrence
    for (final NamedEntityOccurrence namedEntityOccurrence : namedEntityOccurrences) {
      final NamedEntity namedEntity =
          namedEntitiesById.get(namedEntityOccurrence.getNamedEntityId());
      if (namedEntity == null) {
        LOG.error("No named entity with ID {} found", namedEntityOccurrence.getNamedEntityId());
        continue;
      }
      final NamedEntity parentNamedEntity =
          namedEntitiesById.get(namedEntityOccurrence.getParentNamedEntityId());
      if (parentNamedEntity == null) {
        LOG.error("No parent named entity with ID {} found",
            namedEntityOccurrence.getParentNamedEntityId());
        continue;
      }

      // Build DTO
      final NamedEntityDto dto = new NamedEntityDto();
      dto.setName(namedEntity.getName());
      dto.setParentName(parentNamedEntity.getName());
      dto.setType(namedEntityOccurrence.getType());
      dto.setQuantity(namedEntityOccurrence.getQuantity());
      dto.setDocument(namedEntityOccurrence.getDocumentId());
      dtos.add(dto);
    }

    // Get categories
    for (final NamedEntityDto dto : dtos) {
      final Set<String> namedEntityCategoryNames =
          namedEntityCategoryDao.getNamedEntityCategoryNames(ssc, dto.getName());
      final List<String> namedEntityCategoryNamesSorted =
          Lists.newArrayList(namedEntityCategoryNames);
      Collections.sort(namedEntityCategoryNamesSorted, CollatorComparator.INSTANCE);
      final String categories = StringUtils.join(namedEntityCategoryNamesSorted, "/");
      dto.setCategories(categories);
    }

    // Done
    return dtos;
  }

}
