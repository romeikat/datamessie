package com.romeikat.datamessie.core.base.util.comparator;

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

import java.util.Comparator;
import com.romeikat.datamessie.core.base.util.DocumentWithDownloads;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public class MasterDocumentWithDownloadsComparator implements Comparator<DocumentWithDownloads> {

  public final static MasterDocumentWithDownloadsComparator INSTANCE =
      new MasterDocumentWithDownloadsComparator();

  private MasterDocumentWithDownloadsComparator() {}

  @Override
  public int compare(final DocumentWithDownloads d1, final DocumentWithDownloads d2) {
    // Prio 1: Order by success
    final boolean success1 = isSuccess(d1);
    final boolean success2 = isSuccess(d2);
    if (success1 && !success2) {
      return -1;
    } else if (!success1 && success2) {
      return 1;
    }

    // Prio 2: Order by ID
    return Long.valueOf(d1.getDocumentId()).compareTo(Long.valueOf(d2.getDocumentId()));
  }

  private boolean isSuccess(final DocumentWithDownloads d) {
    if (d.getDocumentProcessingState() == DocumentProcessingState.REDIRECTING_ERROR) {
      return false;
    }

    if (!d.getSuccess()) {
      return false;
    }

    return true;
  }

}
