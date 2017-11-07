package com.romeikat.datamessie.core.processing.task.documentProcessing.stemming;

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

import java.util.Collections;
import java.util.List;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.util.HtmlUtil;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import com.romeikat.datamessie.core.processing.service.stemming.namedEntity.NamedEntitiesDetector;
import com.romeikat.datamessie.core.processing.service.stemming.text.TextStemmer;

@Service
public class DocumentStemmer {

  @Autowired
  private NamedEntitiesDetector namedEntitiesDetector;

  @Autowired
  private TextStemmer textStemmer;

  @Autowired
  private HtmlUtil htmlUtil;

  private DocumentStemmer() {}

  public DocumentStemmingResult stem(final StatelessSession statelessSession,
      final Document document, final String cleanedContent, final Language language)
      throws Exception {
    // Stem title
    final String titleWithoutTags = htmlUtil.removeTags(document.getTitle());
    final String stemmedTitle =
        textStemmer.stemText(statelessSession, titleWithoutTags, Collections.emptySet(), language);

    // Stem description
    final String descriptionWithoutTags = htmlUtil.removeTags(document.getDescription());
    final String stemmedDescription = textStemmer.stemText(statelessSession, descriptionWithoutTags,
        Collections.emptySet(), language);

    // Stem content
    final List<NamedEntityDetectionDto> namedEntityDetections =
        namedEntitiesDetector.detectNamedEntities(cleanedContent);
    final List<String> namedEntityNames =
        namedEntitiesDetector.getNamedEntityNames(namedEntityDetections);
    final String stemmedContent =
        textStemmer.stemText(statelessSession, cleanedContent, namedEntityNames, language);

    // Done
    return new DocumentStemmingResult(stemmedTitle, stemmedDescription, stemmedContent,
        namedEntityDetections);
  }

}
