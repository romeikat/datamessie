package com.romeikat.datamessie.core.processing.task.documentProcessing.stemming;

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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.domain.enums.Language;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.StemCallback;

public class DocumentsStemmer {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsStemmer.class);

  private final Double processingParallelismFactor;

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  private final StemCallback stemCallback;

  public DocumentsStemmer(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput, final StemCallback stemCallback,
      final ApplicationContext ctx) {
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));

    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;

    this.stemCallback = stemCallback;
  }

  /**
   * Performs the stemming for the documents in {@code documentsProcessingInput}.
   *
   * Depending on the result, {@code documentsProcessingInput} and {@code documentsProcessingInput}
   * are modified as follows.
   * <ul>
   * <li>Documents whose state is not {@code DocumentProcessingState.REDIRECTED} are ignored.</li>
   * <li>If stemming was successful, the state of the document is set to
   * {@code DocumentProcessingState.STEMMED}. The properties of the document ({@code stemmedTitle}
   * and {@code stemmedDescription}) are updated. Hence, the document is added to the
   * {@code documentsProcessingOutput}. Also, a stemmed content is created and added to the
   * {@code documentsProcessingOutput}. Finally, named entity detections are added to the
   * {@code documentsProcessingInput} for further processing.</li>
   * <li>If cleaning failed, the document is removed from the {@code documentsProcessingInput}. Its
   * state is set to {@code DocumentProcessingState.TECHNICAL_ERROR}. Hence, the document is added
   * to the {@code documentsProcessingOutput}. Also, empty stemmed content and named entity
   * detections are added to the {@code documentsProcessingOutput}.</li>
   * </ul>
   *
   * @param stemmingEnabled
   */
  public void stemDocuments(boolean stemmingEnabled) {
    new ParallelProcessing<Document>(null, documentsProcessingInput.getDocuments(),
        processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Document document) {
        try {
          if (stemmingEnabled) {
            stemDocument(document);
          } else {
            outputEmptyResults(document);
          }
        } catch (final Exception e) {
          final String msg = String.format("Could not stem document %s", document.getId());
          LOG.error(msg, e);

          document.setState(DocumentProcessingState.TECHNICAL_ERROR);

          documentsProcessingInput.removeDocument(document);

          outputEmptyResults(document);
        }
      }
    };
  }

  private void stemDocument(final Document document) {
    if (document.getState() != DocumentProcessingState.CLEANED) {
      return;
    }

    final CleanedContent cleanedContent =
        documentsProcessingOutput.getCleanedContent(document.getId());

    // Determine language
    final Source source = documentsProcessingInput.getSource(document.getId());
    final Language language = source.getLanguage();

    // Stem
    final DocumentStemmingResult documentStemmingResult =
        stemCallback.stem(document, cleanedContent.getContent(), language);

    // Interpret result
    interpretStemmingResult(documentStemmingResult, document);
  }

  private void interpretStemmingResult(final DocumentStemmingResult documentStemmingResult,
      final Document document) {
    document.setState(DocumentProcessingState.STEMMED);

    document.setStemmedTitle(documentStemmingResult.getStemmedTitle());
    document.setStemmedDescription(documentStemmingResult.getStemmedDescription());

    documentsProcessingOutput.putDocument(document);
    final String stemmedContent =
        ObjectUtils.defaultIfNull(documentStemmingResult.getStemmedContent(), "");
    documentsProcessingOutput
        .putStemmedContent(new StemmedContent(document.getId(), stemmedContent));

    final List<NamedEntityDetectionDto> namedEntityDetections =
        documentStemmingResult.getNamedEntityDetections();
    documentsProcessingInput.putNamedEntityDetections(document.getId(), namedEntityDetections);

    // No cleanup necessary as there is no state STEMMING_ERROR
  }

  private void outputEmptyResults(final Document document) {
    document.setStemmedTitle("");
    document.setStemmedDescription("");
    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putStemmedContent(new StemmedContent(document.getId(), ""));
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), Collections.emptyList());
  }

}
