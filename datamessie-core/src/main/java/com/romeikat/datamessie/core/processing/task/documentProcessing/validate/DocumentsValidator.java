package com.romeikat.datamessie.core.processing.task.documentProcessing.validate;

import java.util.Collections;
/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.Source;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;

public class DocumentsValidator {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsValidator.class);

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  public DocumentsValidator(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput) {
    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;
  }

  /**
   * Performs the validation for the documents in {@code documentsProcessingInput}. Depending on the
   * result, {@code documentsProcessingInput} and {@code documentsProcessingInput} are modified as
   * follows.
   * <ul>
   * <li>If a document is valid, nothing is done. The document just remains in the
   * {@code documentsProcessingInput}.</li>
   * <li>If a document is invalid, it is removed from the {@code documentsProcessingInput} and is
   * added to the {@code documentsProcessingOutput}. Also, empty cleaned content, stemmed content,
   * and named entity occurrences are added to the {@code documentsProcessingOutput}.</li>
   * </ul>
   */
  public void validateDocuments() {
    for (final Document document : documentsProcessingInput.getDocuments()) {
      final RawContent rawContent = documentsProcessingInput.getRawContent(document.getId());
      final Source source = documentsProcessingInput.getSource(document.getId());
      final boolean valid = validateDocument(document, rawContent, source);

      if (!valid) {
        documentsProcessingInput.removeDocument(document);
      }
    }
  }

  private boolean validateDocument(final Document document, final RawContent rawContent,
      final Source source) {
    // A missing raw content must only occur in case of a download error
    if (rawContent == null && document.getState() != DocumentProcessingState.DOWNLOAD_ERROR) {
      LOG.warn("Raw content is missing for document {}");

      document.setState(DocumentProcessingState.TECHNICAL_ERROR);
      outputEmptyResults(document);

      return false;
    }

    // A source must be present
    if (source == null) {
      LOG.warn("Source is missing for document {}");

      document.setState(DocumentProcessingState.TECHNICAL_ERROR);
      outputEmptyResults(document);

      return false;
    }

    // In case of a download error, don't process further
    if (document.getState() == DocumentProcessingState.DOWNLOAD_ERROR) {
      outputEmptyResults(document);

      return false;
    }

    return true;
  }

  private void outputEmptyResults(final Document document) {
    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putCleanedContent(new CleanedContent(document.getId(), ""));
    documentsProcessingOutput.putStemmedContent(new StemmedContent(document.getId(), ""));
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), Collections.emptyList());
  }

}
