package com.romeikat.datamessie.core.base.util.converter;

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

import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentProcessingStateConverter extends AbstractConverter<DocumentProcessingState> {

  public static final DocumentProcessingStateConverter INSTANCE = new DocumentProcessingStateConverter();

  private static final long serialVersionUID = 1L;

  private DocumentProcessingStateConverter() {}

  @Override
  protected DocumentProcessingState toObject(final String value) {
    if (value == null) {
      return null;
    }

    return DocumentProcessingState.valueOf(DocumentProcessingState.class, value);
  }

  @Override
  protected String toString(final DocumentProcessingState value) {
    if (value == null) {
      return null;
    }

    return value.getName();
  }

}
