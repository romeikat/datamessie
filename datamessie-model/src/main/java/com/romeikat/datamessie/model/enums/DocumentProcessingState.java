package com.romeikat.datamessie.model.enums;

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
import java.util.Set;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.model.core.Document;

public enum DocumentProcessingState {

  DOWNLOADED("Downloaded"),

  /**
   * The {@link Document} could not be downloaded.
   */
  DOWNLOAD_ERROR("Downloading error"),

  /**
   * The raw content of the redirected URL was downloaded successfully.
   */
  REDIRECTED("Redirected"),

  /**
   * The raw content of the redirected URL could not be downloaded.
   */
  REDIRECTING_ERROR("Redirecting error"),

  /**
   * The {@link Document} was cleaned successfully.
   */
  CLEANED("Cleaned"),

  /**
   * No matching tag in the raw content of the {@link Document} was found.
   */
  CLEANING_ERROR("Cleaning error"),

  /**
   * The {@link Document} was stemmed successfully. This is the final desired state within the
   * lifecycle.
   */
  STEMMED("Stemmed"),

  /**
   * To be deleted.
   */
  TO_BE_DELETED("To be deleted"),

  /**
   * An unexpected error occurred.
   */
  TECHNICAL_ERROR("Technical error");

  private String name;

  private DocumentProcessingState(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static DocumentProcessingState[] getWith(final DocumentProcessingState... states) {
    return states;
  }

  public static DocumentProcessingState[] getWithout(
      final DocumentProcessingState... excludedStates) {
    final List<DocumentProcessingState> desiredStates = Lists.newLinkedList();
    final Set<DocumentProcessingState> excludedStatesAsSet = Sets.newHashSet(excludedStates);
    for (final DocumentProcessingState state : DocumentProcessingState.values()) {
      if (!excludedStatesAsSet.contains(state)) {
        desiredStates.add(state);
      }
    }
    return desiredStates.toArray(new DocumentProcessingState[] {});
  }

  public static List<DocumentProcessingState> getSuccessStates() {
    return Lists.newArrayList(DOWNLOADED, REDIRECTED, CLEANED, STEMMED);
  }

  public static List<DocumentProcessingState> getSuccessStatesFor(
      final DocumentProcessingState state) {
    final List<DocumentProcessingState> states = Lists.newArrayListWithExpectedSize(4);

    // Add one after another in the right order until the desired state occurs
    final List<DocumentProcessingState> successStates = getSuccessStates();
    Collections.reverse(successStates);
    for (final DocumentProcessingState successState : successStates) {
      states.add(successState);
      if (state == successState) {
        return states;
      }
    }

    return states;
  }

  public static List<DocumentProcessingState> getErrorStates() {
    return Lists.newArrayList(DOWNLOAD_ERROR, REDIRECTING_ERROR, CLEANING_ERROR, TECHNICAL_ERROR,
        TO_BE_DELETED);
  }

  public static Set<DocumentProcessingState> getAllStates() {
    return Sets.newHashSet(DocumentProcessingState.values());
  }

}
