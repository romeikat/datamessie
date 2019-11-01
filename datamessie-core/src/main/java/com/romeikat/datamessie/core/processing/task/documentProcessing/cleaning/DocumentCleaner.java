package com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning;

import java.util.List;
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
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.processing.service.cleaning.boilerpipe.BoilerplateRemover;
import com.romeikat.datamessie.core.processing.service.cleaning.extract.TagExctractor;
import com.romeikat.datamessie.model.core.Document;
import com.romeikat.datamessie.model.core.RawContent;
import com.romeikat.datamessie.model.core.TagSelectingRule;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

@Service
public class DocumentCleaner {

  @Autowired
  private TagExctractor tagExctractor;

  @Autowired
  private BoilerplateRemover boilerplateRemover;

  private DocumentCleaner() {}

  public DocumentCleaningResult clean(final Document document, final RawContent rawContent,
      final List<TagSelectingRule> tagSelectingRules) throws BoilerpipeProcessingException {
    // Extract
    final String extractedContent =
        tagExctractor.extractContent(tagSelectingRules, rawContent, document);

    // Remove boilerplate
    String cleanedContent = boilerplateRemover.removeBoilerplate(extractedContent);
    cleanedContent = StringEscapeUtils.unescapeHtml4(cleanedContent);

    // Done
    return new DocumentCleaningResult(cleanedContent);
  }

}
