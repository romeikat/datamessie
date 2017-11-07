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

import java.util.ArrayList;
import java.util.List;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.enums.NamedEntityType;
import com.romeikat.datamessie.core.processing.dto.NamedEntityDetectionDto;

public class NamedEntityDetectionsUniqueList {

  private final List<NamedEntityDetectionDto> namedEntityDetections =
      new ArrayList<NamedEntityDetectionDto>();

  private final List<NamedEntityDetectionDto> buffer = new ArrayList<NamedEntityDetectionDto>();

  public synchronized void add(final NamedEntityDetectionDto namedEntityDetection) {
    addToBuffer(namedEntityDetection);
  }

  private void addToBuffer(final NamedEntityDetectionDto namedEntityDetection) {
    // Buffer empty => buffer
    if (buffer.isEmpty()) {
      buffer.add(namedEntityDetection);
    }
    // Named entity detections buffered with matching type => buffer
    else if (getBufferedType().equals(namedEntityDetection.getType())) {
      buffer.add(namedEntityDetection);
    }
    // Named entity detections buffered with different type => flush and buffer
    else {
      flushBuffer();
      buffer.add(namedEntityDetection);
    }
  }

  private NamedEntityType getBufferedType() {
    return buffer.get(0).getType();
  }

  public synchronized void flushBuffer() {
    if (buffer.isEmpty()) {
      return;
    }
    // Merge buffered named entity detections
    final NamedEntityDetectionDto mergedNamedEntityDetection = buffer.get(0);
    for (int i = 1; i < buffer.size(); i++) {
      final NamedEntityDetectionDto furtherNamedEntityDetection = buffer.get(i);
      final String oldName = mergedNamedEntityDetection.getName();
      final String oldParentName = mergedNamedEntityDetection.getParentName();
      final String newName = oldName + " " + furtherNamedEntityDetection.getName();
      final String newParentName =
          oldParentName + " " + furtherNamedEntityDetection.getParentName();
      mergedNamedEntityDetection.setName(newName).setParentName(newParentName);
    }
    // Clear buffer
    buffer.clear();
    // Add merged named entity to list
    integrateIntoList(mergedNamedEntityDetection);
  }

  private void integrateIntoList(final NamedEntityDetectionDto newNamedEntityDetection) {
    // Compare with the existing named entity detections
    for (final NamedEntityDetectionDto existingNamedEntityDetection : namedEntityDetections) {
      // Existing named entity detection has a different type => skip
      final boolean typeMatch =
          existingNamedEntityDetection.getType().equals(newNamedEntityDetection.getType());
      if (!typeMatch) {
        continue;
      }
      // Existing and new named entity detections are the same => increment quantity
      final String existingName = existingNamedEntityDetection.getName();
      final String newName = newNamedEntityDetection.getName();
      final boolean same = existingName.equals(newName);
      if (same) {
        final Integer oldQuantity = existingNamedEntityDetection.getQuantity();
        final int newQuantity = oldQuantity == null ? 2 : oldQuantity + 1;
        existingNamedEntityDetection.setQuantity(newQuantity);
        return;
      }
      // Existing named entity detection is a part of new one => mark new one as parent of
      // existing one
      final List<String> existingWordList =
          NamedEntity.getWordList(existingNamedEntityDetection.getParentName());
      final List<String> newWordList =
          NamedEntity.getWordList(newNamedEntityDetection.getParentName());
      final boolean existingIsPartOfNew = newWordList.containsAll(existingWordList);
      if (existingIsPartOfNew) {
        existingNamedEntityDetection.setParentName(newNamedEntityDetection.getParentName());
        continue;
      }
      // New named entity is a part of existing one => mark (first) existing one as parent
      // of new one
      final boolean newIsPartOfExisting = existingWordList.containsAll(newWordList);
      if (newIsPartOfExisting) {
        if (!newNamedEntityDetection.hasDifferentParent()) {
          newNamedEntityDetection.setParentName(existingNamedEntityDetection.getParentName());
        }
        continue;
      }
    }
    // No same named entity occurred, so add a new one
    namedEntityDetections.add(newNamedEntityDetection);
  }

  public synchronized List<NamedEntityDetectionDto> asList() {
    flushBuffer();
    return new ArrayList<NamedEntityDetectionDto>(namedEntityDetections);
  }

}
