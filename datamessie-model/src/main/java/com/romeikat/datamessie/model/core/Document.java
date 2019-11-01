package com.romeikat.datamessie.model.core;

/*-
 * ============================LICENSE_START============================
 * data.messie (model)
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.romeikat.datamessie.model.EntityWithIdAndVersion;
import com.romeikat.datamessie.model.enums.DocumentProcessingState;


public interface Document extends EntityWithIdAndVersion {

  String getTitle();

  Document setTitle(String title);

  String getStemmedTitle();

  Document setStemmedTitle(String stemmedTitle);

  String getUrl();

  Document setUrl(String url);

  String getDescription();

  Document setDescription(String description);

  String getStemmedDescription();

  Document setStemmedDescription(String stemmedDescription);

  LocalDateTime getPublished();

  Document setPublished(LocalDateTime published);

  LocalDate getPublishedDate();

  LocalDateTime getDownloaded();

  Document setDownloaded(LocalDateTime downloaded);

  DocumentProcessingState getState();

  Document setState(DocumentProcessingState state);

  Integer getStatusCode();

  Document setStatusCode(Integer statusCode);

  long getCrawlingId();

  Document setCrawlingId(long crawlingId);

  long getSourceId();

  Document setSourceId(long sourceId);

}
