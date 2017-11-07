package com.romeikat.datamessie.core.statistics.task;

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

import java.time.LocalDate;
import org.apache.commons.lang3.tuple.MutablePair;

class SourceAndPublished extends MutablePair<Long, LocalDate> {

  private static final long serialVersionUID = 1L;

  public SourceAndPublished(final Long sourceId, final LocalDate published) {
    super(sourceId, published);
  }

  public Long getSourceId() {
    return getLeft();
  }

  public LocalDate getPublished() {
    return getRight();
  }

}
