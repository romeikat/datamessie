package com.romeikat.datamessie.core.base.util;

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

import java.util.Collection;
import java.util.Set;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.model.core.Download;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;

public class DocumentWithDownloads {

  private final long documentId;
  private final DocumentProcessingState documentProcessingState;
  private final Set<Long> downloadIds;
  private boolean success;

  public DocumentWithDownloads(final long documentId,
      final DocumentProcessingState documentProcessingState) {
    this.documentId = documentId;
    this.documentProcessingState = documentProcessingState;
    downloadIds = Sets.newHashSet();
    success = false;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(getClass().getSimpleName());
    result.append(" [");

    result.append("documentId=");
    result.append(documentId);

    if (!downloadIds.isEmpty()) {
      result.append("; downloads=");
      result.append(downloadIds);
    }

    result.append("; success=");
    result.append(success);

    result.append("]");

    return result.toString();
  }

  public void addDownload(final Download download) {
    downloadIds.add(download.getId());
    if (download.getSuccess()) {
      success = true;
    }
  }

  public void addDownloads(final Collection<Download> downloads) {
    for (final Download download : downloads) {
      addDownload(download);
    }
  }

  public long getDocumentId() {
    return documentId;
  }

  public DocumentProcessingState getDocumentProcessingState() {
    return documentProcessingState;
  }

  public Set<Long> getDownloadIds() {
    return downloadIds;
  }

  public boolean getSuccess() {
    return success;
  }

}
