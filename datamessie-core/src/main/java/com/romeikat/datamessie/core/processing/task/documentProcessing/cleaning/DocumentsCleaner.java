package com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning;

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

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.domain.enums.CleaningMethod;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.CleanCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.PersistDocumentProcessingOutputCallback;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

public class DocumentsCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsCleaner.class);

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  private final CleanCallback cleanCallback;
  private final PersistDocumentProcessingOutputCallback persistDocumentProcessingOutputCallback;

  public DocumentsCleaner(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput, final CleanCallback cleanCallback,
      final PersistDocumentProcessingOutputCallback persistDocumentProcessingOutputCallback,
      final ApplicationContext ctx) {
    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;

    this.persistDocumentProcessingOutputCallback = persistDocumentProcessingOutputCallback;
    this.cleanCallback = cleanCallback;
  }

  /**
   * Performs the cleaning for the documents in {@code documentsProcessingInput}.
   *
   * Depending on the result, {@code documentsProcessingInput} and {@code documentsProcessingOutput}
   * are modified as follows.
   * <ul>
   * <li>Documents whose state is not {@code DocumentProcessingState.REDIRECTED} are ignored.</li>
   * <li>If cleaning was successful, the state of the document is set to
   * {@code DocumentProcessingState.CLEANED}. Hence, the document is added to the
   * {@code documentsProcessingOutput}. Also, a cleaned content is created and added to the
   * {@code documentsProcessingOutput}.</li>
   * <li>If cleaning failed, the document is removed from the {@code documentsProcessingInput}. Its
   * state is set to {@code DocumentProcessingState.CLEANING_ERROR} or
   * {@code DocumentProcessingState.TECHNICAL_ERROR}. Hence, the document is added to the
   * {@code documentsProcessingOutput}. Also, empty cleaned content, stemmed content, and named
   * entity occurrences are added to the {@code documentsProcessingOutput}.</li>
   * </ul>
   *
   * @param documents
   */
  public void cleanDocuments(final Collection<Document> documents) {
    for (final Document document : documents) {
      try {
        cleanDocument(document);
      } catch (final Exception e) {
        final String msg = String.format("Could not clean document %s", document.getId());
        LOG.error(msg, e);

        document.setState(DocumentProcessingState.TECHNICAL_ERROR);

        documentsProcessingInput.removeDocument(document);

        outputEmptyResults(document);
      }
    }
  }

  private void cleanDocument(final Document document) throws BoilerpipeProcessingException {
    if (document.getState() != DocumentProcessingState.REDIRECTED) {
      return;
    }

    final RawContent rawContent = documentsProcessingInput.getRawContent(document.getId());

    // Determine active rules
    final LocalDate downloadDate = document.getDownloaded().toLocalDate();
    final List<DeletingRule> deletingRules =
        documentsProcessingInput.getActiveDeletingRules(document, downloadDate);
    final List<TagSelectingRule> tagSelectingRules =
        documentsProcessingInput.getActiveTagSelectingRules(document, downloadDate);

    // Clean
    final CleaningMethod cleaningMethod = getCleaningMethod(document.getId());
    final DocumentCleaningResult documentCleaningResult =
        cleanCallback.clean(document, rawContent, deletingRules, tagSelectingRules, cleaningMethod);

    // Interpret result
    interpretCleaningResult(documentCleaningResult, document);
  }

  private CleaningMethod getCleaningMethod(final long documentId) {
    final Project project = documentsProcessingInput.getProject(documentId);
    if (project == null) {
      return null;
    }

    return project.getCleaningMethod();
  }

  private void interpretCleaningResult(final DocumentCleaningResult documentCleaningResult,
      final Document document) {
    // Stemmed title and description may be null, as title and description may be null,
    // but stemmed content must not be null
    final boolean wasCleaningSuccesful = documentCleaningResult.getCleanedContent() != null;
    if (wasCleaningSuccesful) {
      document.setState(DocumentProcessingState.CLEANED);

      final CleanedContent cleanedContent =
          new CleanedContent(document.getId(), documentCleaningResult.getCleanedContent());
      outputProperResults(document, cleanedContent);
    } else {
      document.setState(DocumentProcessingState.CLEANING_ERROR);

      documentsProcessingInput.removeDocument(document);

      outputEmptyResults(document);
    }
  }

  private void outputProperResults(final Document document, final CleanedContent cleanedContent) {
    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putCleanedContent(cleanedContent);

    persistProperResults(document);
  }

  private void persistProperResults(final Document document) {
    final CleanedContent cleanedContent =
        documentsProcessingOutput.getCleanedContent(document.getId());

    persistDocumentProcessingOutputCallback.persistDocumentsProcessingOutput(document,
        cleanedContent, null, null);
  }

  private void outputEmptyResults(final Document document) {
    final CleanedContent cleanedContent = new CleanedContent(document.getId(), "");
    final StemmedContent stemmedContent = new StemmedContent(document.getId(), "");
    final List<NamedEntityOccurrence> namedEntityOccurrences = Collections.emptyList();

    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putCleanedContent(cleanedContent);
    documentsProcessingOutput.putStemmedContent(stemmedContent);
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), namedEntityOccurrences);

    persistEmptyResults(document);
  }

  private void persistEmptyResults(final Document document) {
    final CleanedContent cleanedContent =
        documentsProcessingOutput.getCleanedContent(document.getId());
    final StemmedContent stemmedContent =
        documentsProcessingOutput.getStemmedContent(document.getId());
    final List<NamedEntityOccurrence> namedEntityOccurrences =
        documentsProcessingOutput.getNamedEntityOccurrences(document.getId());

    persistDocumentProcessingOutputCallback.persistDocumentsProcessingOutput(document,
        cleanedContent, stemmedContent, namedEntityOccurrences);
  }

}
