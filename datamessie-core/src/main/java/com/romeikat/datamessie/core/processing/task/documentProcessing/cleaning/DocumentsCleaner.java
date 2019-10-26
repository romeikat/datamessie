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
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.CleanedContentDao;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.CleanCallback;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

public class DocumentsCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsCleaner.class);

  private final CleanedContentDao cleanedContentDao;
  private final Double processingParallelismFactor;

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  private final CleanCallback cleanCallback;

  public DocumentsCleaner(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput, final CleanCallback cleanCallback,
      final ApplicationContext ctx) {
    cleanedContentDao = ctx.getBean(CleanedContentDao.class);
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));

    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;

    this.cleanCallback = cleanCallback;
  }

  /**
   * Performs the cleaning for the documents in {@code documentsProcessingInput}.
   *
   * Depending on the result, {@code documentsProcessingInput} and {@code documentsProcessingInput}
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
   */
  public void cleanDocuments() {
    new ParallelProcessing<Document>(null, documentsProcessingInput.getDocuments(),
        processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Document document) {
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
    };
  }

  private void cleanDocument(final Document document) throws BoilerpipeProcessingException {
    if (document.getState() != DocumentProcessingState.REDIRECTED) {
      return;
    }

    final RawContent rawContent = documentsProcessingInput.getRawContent(document.getId());

    // Determine active rules
    final LocalDate downloadDate = document.getDownloaded().toLocalDate();
    final List<TagSelectingRule> tagSelectingRules =
        documentsProcessingInput.getActiveTagSelectingRules(document, downloadDate);

    // Clean
    final DocumentCleaningResult documentCleaningResult =
        cleanCallback.clean(document, rawContent, tagSelectingRules);

    // Interpret result
    interpretCleaningResult(documentCleaningResult, document);
  }

  private void interpretCleaningResult(final DocumentCleaningResult documentCleaningResult,
      final Document document) {
    // Stemmed title and description may be null, as title and description may be null,
    // but stemmed content must not be null
    final boolean wasCleaningSuccesful = documentCleaningResult.getCleanedContent() != null;
    if (wasCleaningSuccesful) {
      document.setState(DocumentProcessingState.CLEANED);

      documentsProcessingOutput.putDocument(document);
      documentsProcessingOutput.putCleanedContent(
          cleanedContentDao.create(document.getId(), documentCleaningResult.getCleanedContent()));
    } else {
      document.setState(DocumentProcessingState.CLEANING_ERROR);

      documentsProcessingInput.removeDocument(document);

      outputEmptyResults(document);
    }
  }

  private void outputEmptyResults(final Document document) {
    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putCleanedContent(cleanedContentDao.create(document.getId(), ""));
    documentsProcessingOutput.putStemmedContent(new StemmedContent(document.getId(), ""));
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), Collections.emptyList());
  }

}
