package com.romeikat.datamessie.core.processing.task.documentProcessing.redirecting;

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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.romeikat.datamessie.core.domain.entity.Download;

public class DownloadEntry {

  private final Download download;

  public DownloadEntry(final Download download) {
    this.download = download;
  }

  public Download getDowload() {
    return download;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final DownloadEntry otherDownloadEntry = (DownloadEntry) other;
    final boolean equals =
        new EqualsBuilder().append(download.getUrl(), otherDownloadEntry.download.getUrl())
            .append(download.getSourceId(), otherDownloadEntry.download.getSourceId()).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode =
        new HashCodeBuilder().append(download.getUrl()).append(download.getSourceId()).toHashCode();
    return hashCode;
  }

}
