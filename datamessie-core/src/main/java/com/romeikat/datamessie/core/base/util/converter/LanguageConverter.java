package com.romeikat.datamessie.core.base.util.converter;

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

import com.romeikat.datamessie.model.enums.DocumentProcessingState;
import com.romeikat.datamessie.model.enums.Language;

public class LanguageConverter extends AbstractConverter<Language> {

  public static final LanguageConverter INSTANCE = new LanguageConverter();

  private static final long serialVersionUID = 1L;

  private LanguageConverter() {}

  @Override
  protected Language toObject(final String value) {
    if (value == null) {
      return null;
    }

    return DocumentProcessingState.valueOf(Language.class, value);
  }

  @Override
  protected String toString(final Language value) {
    if (value == null) {
      return null;
    }

    return value.getName();
  }

}
