package com.romeikat.datamessie.core.base.util;

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

import com.romeikat.datamessie.model.util.StringHashProvider;

public class UpdateTracker<T extends StringHashProvider> {

  private final T object;

  private String stringHashBeforeUpdate;
  private String stringHashAfterUpdate;

  public UpdateTracker(final T object) {
    this.object = object;
  }

  public UpdateTracker<T> beginUpdate() {
    stringHashBeforeUpdate = object.asStringHash();

    return this;
  }

  public UpdateTracker<T> endUpdate() {
    stringHashAfterUpdate = object.asStringHash();

    return this;
  }

  public boolean wasObjectUpdated() {
    if (stringHashBeforeUpdate == null || stringHashAfterUpdate == null) {
      return false;
    }

    return !stringHashBeforeUpdate.equals(stringHashAfterUpdate);
  }

}
