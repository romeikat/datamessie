package com.romeikat.datamessie.core.sync.util;

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

public enum SyncMode {

  SYNC,

  MIGRATE;

  /**
   * Determines whether data should be deleted at all.
   */
  public boolean shouldDeleteData() {
    // SYNC mode: data is always deleted
    // MIGRATE mode: data is never deleted
    return this == SYNC;
  }

  /**
   * Determines whether data should be created or updated at all.
   */
  public boolean shouldCreateAndUpdateData() {
    return true;
  }

  /**
   * Determines whether data should be created depending on the RHS.
   */
  public boolean shouldCreateData(final boolean isRhsEmpty) {
    // SYNC mode: data is always created
    // MIGRATE mode: data is only created in the 1st run, i.e. when RHS is empty
    return this == SYNC || isRhsEmpty;
  }

  /**
   * Determines whether data should be updated depending on the RHS.
   */
  public boolean shouldUpdateData(final boolean isRhsEmpty) {
    // SYNC mode: data is always updated
    // MIGRATE mode: data is only created in the 2nd run, i.e. when RHS is non-empty
    return this == SYNC || !isRhsEmpty;
  }

  public boolean shouldApplyFilters() {
    // Filters only make sense for MIGRATE mode
    return this == MIGRATE;
  }

}
