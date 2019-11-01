package com.romeikat.datamessie.model.core;

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
import com.romeikat.datamessie.model.EntityWithIdAndVersion;
import com.romeikat.datamessie.model.util.StringHashProvider;

public interface TagSelectingRule extends EntityWithIdAndVersion, StringHashProvider {

  String getTagSelector();

  TagSelectingRule setTagSelector(String tagSelector);

  LocalDate getActiveFrom();

  TagSelectingRule setActiveFrom(LocalDate activeFrom);

  LocalDate getActiveTo();

  TagSelectingRule setActiveTo(LocalDate activeTo);

  boolean isActive(LocalDate localDate);

  long getSourceId();

  TagSelectingRule setSourceId(long sourceId);

}
