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

import java.util.List;
import com.romeikat.datamessie.core.processing.task.documentProcessing.cleaning.DocumentCleaningResult;
import com.romeikat.datamessie.model.core.Document;
import com.romeikat.datamessie.model.core.RawContent;
import com.romeikat.datamessie.model.core.TagSelectingRule;
import de.l3s.boilerpipe.BoilerpipeProcessingException;

@FunctionalInterface
public interface CleanCallback {

  DocumentCleaningResult clean(Document document, RawContent rawContent,
      List<TagSelectingRule> tagSelectingRules) throws BoilerpipeProcessingException;

}
