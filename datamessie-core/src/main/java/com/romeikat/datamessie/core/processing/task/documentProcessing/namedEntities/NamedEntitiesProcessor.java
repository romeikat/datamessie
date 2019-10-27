package com.romeikat.datamessie.core.processing.task.documentProcessing.namedEntities;

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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.parallelProcessing.ParallelProcessing;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingInput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingOutput;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetNamedEntityNamesWithoutCategoryCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.GetOrCreateNamedEntitiesCallback;
import com.romeikat.datamessie.core.processing.task.documentProcessing.callback.ProvideNamedEntityCategoryTitlesCallback;

public class NamedEntitiesProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(NamedEntitiesProcessor.class);

  private final Double processingParallelismFactor;
  private final SessionFactory sessionFactory;

  private final DocumentsProcessingInput documentsProcessingInput;
  private final DocumentsProcessingOutput documentsProcessingOutput;

  private final NamedEntityOccurrencesCreator namedEntityOccurrencesCreator;
  private final NamedEntityCategoriesCreator namedEntityCategoriesCreator;

  private final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback;

  public NamedEntitiesProcessor(final DocumentsProcessingInput documentsProcessingInput,
      final DocumentsProcessingOutput documentsProcessingOutput,
      final GetOrCreateNamedEntitiesCallback getOrCreateNamedEntitiesCallback,
      final GetNamedEntityNamesWithoutCategoryCallback getNamedEntityNamesWithoutCategoryCallback,
      final ProvideNamedEntityCategoryTitlesCallback provideNamedEntityCategoryTitlesCallback,
      final ApplicationContext ctx) {
    processingParallelismFactor = Double
        .parseDouble(SpringUtil.getPropertyValue(ctx, "documents.processing.parallelism.factor"));
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);

    this.documentsProcessingInput = documentsProcessingInput;
    this.documentsProcessingOutput = documentsProcessingOutput;

    namedEntityOccurrencesCreator = new NamedEntityOccurrencesCreator(ctx);
    namedEntityCategoriesCreator = new NamedEntityCategoriesCreator(
        getOrCreateNamedEntitiesCallback, getNamedEntityNamesWithoutCategoryCallback,
        provideNamedEntityCategoryTitlesCallback, ctx);

    this.getOrCreateNamedEntitiesCallback = getOrCreateNamedEntitiesCallback;
  }

  /**
   * Processes the named entity detections for the documents in {@code documentsProcessingInput}.
   *
   * Depending on the result, {@code documentsProcessingInput} and {@code documentsProcessingInput}
   * are modified as follows.
   * <ul>
   * <li>Missing named entites are created and persisted them immediately, so they can be assigned
   * to named entity occurrences.</li>
   * <li>For any named entity detection, a named entity occurrence is created and added to the
   * {@code documentsProcessingOutput}.</li>
   * <li>If processing failed, the document is removed from the {@code documentsProcessingInput}.
   * Its state is set to {@code DocumentProcessingState.TECHNICAL_ERROR}. Hence, the document is
   * added to the {@code documentsProcessingOutput}. Also, empty ynamed entity occurrences are added
   * to the {@code documentsProcessingOutput}.</li>
   * <li>Finally, missing named entity categories are created and added to the
   * {@code documentsProcessingOutput}.</li>
   * </ul>
   */
  public void processNamedEntities() {
    // Collect named entity names
    final Collection<String> namedEntityNames = determineNamedEntityNames();

    // Get or create named entities
    final Map<String, Long> namedEntityNames2NamedEntityId =
        getOrCreateNamedEntities(namedEntityNames);

    // Create NamedEntityOccurrences
    createNamedEntityOccurrences(namedEntityNames2NamedEntityId);

    // Create NamedEntityCategories
    createNamedEntityCategories(namedEntityNames2NamedEntityId);
  }

  private Set<String> determineNamedEntityNames() {
    final Set<NamedEntityDetectionDto> allNamedEntityDetections =
        documentsProcessingInput.getAllNamedEntityDetections();
    final Set<String> namedEntityNames = Sets.newHashSet();
    for (final NamedEntityDetectionDto namedEntityDetection : allNamedEntityDetections) {
      namedEntityNames.add(namedEntityDetection.getName());
      namedEntityNames.add(namedEntityDetection.getParentName());
    }
    return namedEntityNames;
  }

  private Map<String, Long> getOrCreateNamedEntities(final Collection<String> namedEntityNames) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final Map<String, Long> namedEntityNames2NamedEntityId = getOrCreateNamedEntitiesCallback
        .getOrCreate(sessionProvider.getStatelessSession(), namedEntityNames);
    sessionProvider.closeStatelessSession();
    return namedEntityNames2NamedEntityId;
  }

  private void createNamedEntityOccurrences(
      final Map<String, Long> namedEntityNames2NamedEntityId) {
    new ParallelProcessing<Document>(null, documentsProcessingInput.getDocuments(),
        processingParallelismFactor) {
      @Override
      public void doProcessing(final HibernateSessionProvider sessionProvider,
          final Document document) {
        try {
          final Set<NamedEntityDetectionDto> namedEntityDetections =
              documentsProcessingInput.getNamedEntityDetections(document.getId());
          final List<NamedEntityOccurrence> namedEntityOccurrences =
              namedEntityOccurrencesCreator.createNamedEntityOccurrences(document.getId(),
                  namedEntityDetections, namedEntityNames2NamedEntityId);
          documentsProcessingOutput.putNamedEntityOccurrences(document.getId(),
              namedEntityOccurrences);
        } catch (final Exception e) {
          final String msg =
              String.format("Could not process named entities of document %s", document.getId());
          LOG.error(msg, e);

          document.setState(DocumentProcessingState.TECHNICAL_ERROR);

          documentsProcessingInput.removeDocument(document);

          outputEmptyResults(document);
        }
      }
    };
  }

  private void createNamedEntityCategories(final Map<String, Long> namedEntityNames2NamedEntityId) {
    final Collection<NamedEntityCategory> namedEntityCategories =
        namedEntityCategoriesCreator.createNamedEntityCategories(namedEntityNames2NamedEntityId);
    documentsProcessingOutput.putNamedEntityCategories(namedEntityCategories);
  }

  private void outputEmptyResults(final Document document) {
    documentsProcessingOutput.putDocument(document);
    documentsProcessingOutput.putNamedEntityOccurrences(document.getId(), Collections.emptyList());
  }

}
