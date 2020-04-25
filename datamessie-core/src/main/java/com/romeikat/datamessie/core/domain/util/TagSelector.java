package com.romeikat.datamessie.core.domain.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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

import java.util.Set;
import com.google.common.collect.Sets;

public class TagSelector {

  private final String tagName;
  private final String idName;
  private final Set<String> classNames;
  private final boolean exactClassNamesMatch;

  public TagSelector(final String tagName, final String idName, final Set<String> classNames,
      final boolean exactClassNamesMatch) {
    super();
    this.tagName = tagName;
    this.idName = idName;
    this.classNames = classNames;
    this.exactClassNamesMatch = exactClassNamesMatch;
  }

  public static TagSelector fromTextualRepresentation(final String tagSelector) {
    String tagName = null;
    String idName = null;
    Set<String> classNames = null;
    boolean exactClassNamesMatch = false;

    final String[] parts = tagSelector.split("#");

    // Tag name
    tagName = parts[0];
    if (tagName.isEmpty()) {
      tagName = null;
    }

    // ID name
    if (parts.length >= 2) {
      idName = parts[1];
      if (idName.isEmpty()) {
        idName = null;
      }
    }

    // Class names
    if (parts.length >= 3) {
      exactClassNamesMatch = parts[2].startsWith("\"") && parts[2].endsWith("\"");
      final String classDefinition =
          exactClassNamesMatch ? parts[2].substring(1, parts[2].length() - 1) : parts[2];
      classNames = Sets.newHashSet(classDefinition.split("\\s+"));
    }

    return new TagSelector(tagName, idName, classNames, exactClassNamesMatch);
  }

  public boolean isValid() {
    // Tag name is required
    if (tagName == null) {
      return false;
    }

    // Either ID name or class names are required
    if (idName == null && classNames == null) {
      return false;
    }

    return true;
  }

  public boolean checkForIdNameMatch(final String candidateIdName) {
    return idName == null || candidateIdName.equals(idName);
  }

  public boolean checkForClassNamesMatch(final Set<String> candidateClassNames) {
    if (classNames == null) {
      return true;
    }

    if (exactClassNamesMatch) {
      return candidateClassNames.equals(classNames);
    } else {
      return candidateClassNames.containsAll(classNames);
    }
  }

  public String getTagName() {
    return tagName;
  }

  public String getIdName() {
    return idName;
  }

  public Set<String> getClassNames() {
    return classNames;
  }

}
