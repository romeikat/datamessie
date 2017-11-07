package com.romeikat.datamessie.core.statistics.cache;

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

import java.io.Serializable;
import com.google.common.base.Function;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class GetNumberOfDocumentsRateFunction
    implements Function<DocumentsPerState, Double>, Serializable {

  private static final long serialVersionUID = 1L;

  private final DocumentProcessingState[] states1;
  private final DocumentProcessingState[] states2;

  public GetNumberOfDocumentsRateFunction(final DocumentProcessingState[] states1,
      final DocumentProcessingState[] states2) {
    this.states1 = states1;
    this.states2 = states2;
  }

  @Override
  public Double apply(final DocumentsPerState documentsPerState) {
    final long numberOfDocumentsDenominator = getNumberOfDocuments(documentsPerState, states2);
    if (numberOfDocumentsDenominator == 0) {
      return null;
    }

    final long numberOfDocumentsNumerator = getNumberOfDocuments(documentsPerState, states1);

    final double numberOfDocumentsRate =
        (double) numberOfDocumentsNumerator / (double) numberOfDocumentsDenominator;
    return numberOfDocumentsRate;
  }

  private static long getNumberOfDocuments(final DocumentsPerState documentsPerState,
      final DocumentProcessingState[] states) {
    // Filter by states
    final long documentsOfStates = documentsPerState.get(states);
    return documentsOfStates;
  }

}
