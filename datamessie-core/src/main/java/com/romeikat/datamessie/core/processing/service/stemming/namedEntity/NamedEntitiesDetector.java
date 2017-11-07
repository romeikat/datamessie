package com.romeikat.datamessie.core.processing.service.stemming.namedEntity;

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

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

@Service
public class NamedEntitiesDetector {

  @Autowired
  private ClassifierPipelineProvider classifierPipelineProvider;

  public List<NamedEntityDetectionDto> detectNamedEntities(final String text) {
    if (text == null) {
      return null;
    }

    final NamedEntityDetectionsUniqueList namedEntityDetections =
        new NamedEntityDetectionsUniqueList();
    // Create empty Annotation with the content
    final Annotation annotation = new Annotation(text);
    // Run annotators on the content
    classifierPipelineProvider.getPipeline().annotate(annotation);
    // Get all the sentences in the content
    final Collection<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
    // Traversing the sentences
    for (final CoreMap sentence : sentences) {
      // Traversing the words in the sentence
      for (final CoreLabel token : sentence.get(TokensAnnotation.class)) {
        // Skip word if not a named entity
        final String namedEntityClass = token.get(NamedEntityTagAnnotation.class);
        final NamedEntityType namedEntityType = getType(namedEntityClass);
        if (namedEntityType == null) {
          namedEntityDetections.flushBuffer();
          continue;
        }
        // Create named entity detection
        final String word = token.get(TextAnnotation.class).toLowerCase();
        final NamedEntityDetectionDto namedEntityDetection = new NamedEntityDetectionDto();
        namedEntityDetection.setName(word).setParentName(word).setType(namedEntityType)
            .setQuantity(1);
        // Add named entity detection
        namedEntityDetections.add(namedEntityDetection);
      }
    }
    // Done
    final List<NamedEntityDetectionDto> namedEntityDetectionsAsList =
        namedEntityDetections.asList();
    return namedEntityDetectionsAsList;
  }

  private NamedEntityType getType(final String namedEntityClass) {
    if (namedEntityClass == null) {
      return null;
    }
    switch (namedEntityClass) {
      case "I-PER":
      case "B-PER":
        return NamedEntityType.PERSON;
      case "I-LOC":
      case "B-LOC":
        return NamedEntityType.LOCATION;
      case "I-ORG":
      case "B-ORG":
        return NamedEntityType.ORGANISATION;
      case "I-MISC":
      case "B-MISC":
        return NamedEntityType.MISC;
      default:
        return null;
    }
  }

  public List<String> getNamedEntityNames(
      final List<NamedEntityDetectionDto> namedEntityDetections) {
    final Function<NamedEntityDetectionDto, String> namedEntityDetectionToNamedEntityNameFunction =
        new Function<NamedEntityDetectionDto, String>() {
          @Override
          public String apply(final NamedEntityDetectionDto namedEntityDetection) {
            return namedEntityDetection.getName();
          }
        };
    final List<String> namedEntityNames =
        Lists.transform(namedEntityDetections, namedEntityDetectionToNamedEntityNameFunction);
    return namedEntityNames;
  }

}
