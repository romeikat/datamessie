package com.romeikat.datamessie.core.base.app.shared;

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
import java.util.Collection;

import com.google.common.base.Function;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.dto.StatisticsDto;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;

public interface IStatisticsManager extends ISharedBean {

  void rebuildStatistics(StatisticsRebuildingSparseTable statisticsToBeRebuilt);

  void rebuildStatistics(Long sourceId, final LocalDate published);

  StatisticsDto getStatistics(long projectId, Integer numberOfDays);

  <T> SparseSingleTable<Long, LocalDate, T> getStatistics(Collection<Long> sourceIds, LocalDate from, LocalDate to,
      Function<LocalDate, LocalDate> transformDateFunction, Function<DocumentsPerState, T> transformValueFunction);

}
