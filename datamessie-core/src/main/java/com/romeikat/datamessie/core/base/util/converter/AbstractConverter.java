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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.convert.IConverter;

public abstract class AbstractConverter<T> implements IConverter<T> {

  private static final long serialVersionUID = 1L;

  @Override
  public T convertToObject(final String value, final Locale locale) {
    return convertToObject(value);
  }

  public T convertToObject(final String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }

    return toObject(value);
  }

  protected abstract T toObject(String value);

  @Override
  public String convertToString(final T value, final Locale locale) {
    return convertToString(value);
  }

  public String convertToString(final T value) {
    if (value == null) {
      return null;
    }

    return toString(value);
  }

  protected abstract String toString(T value);

}
