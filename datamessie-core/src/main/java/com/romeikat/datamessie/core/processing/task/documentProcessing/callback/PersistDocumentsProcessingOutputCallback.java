package com.romeikat.datamessie.core.processing.task.documentProcessing.callback;

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

import java.util.Collection;
import java.util.Map;
import com.romeikat.datamessie.core.domain.entity.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.Document;
import com.romeikat.datamessie.core.domain.entity.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;

@FunctionalInterface
public interface PersistDocumentsProcessingOutputCallback {

  void persistDocumentsProcessingOutput(Collection<Document> documentsToBeUpdated,
      Collection<Download> downloadsToBeCreatedOrUpdated,
      Collection<RawContent> rawContentsToBeUpdated,
      Collection<CleanedContent> cleanedContentsToBeCreatedOrUpdated,
      Collection<StemmedContent> stemmedContentsToBeCreatedOrUpdated,
      Map<Long, ? extends Collection<NamedEntityOccurrence>> namedEntityOccurrencesToBeReplaced,
      Collection<NamedEntityCategory> namedEntityCategoriesToBeSaved);

}
