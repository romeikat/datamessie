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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.StatisticsRebuildingSparseTable;
import com.romeikat.datamessie.core.domain.dto.StatisticsDto;
import com.romeikat.datamessie.core.statistics.cache.DocumentsPerState;

@Service
public class RemoteStatisticsManager implements IStatisticsManager {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteStatisticsManager.class);

  private static final String REMOTE_ERROR_MSG = "Could not get statistics via HTTP";

  @Value("${statistics.manager.url}")
  private String httpStatisticsManagerUrl;

  @Autowired
  private IStatisticsManager httpStatisticsManager;

  @Override
  public int getOrder() {
    return 2;
  }

  private boolean isHttpStatisticsManagerConfigured() {
    return !StringUtils.isBlank(httpStatisticsManagerUrl);
  }

  @Override
  public void rebuildStatistics(final StatisticsRebuildingSparseTable statisticsToBeRebuilt) {
    if (!isHttpStatisticsManagerConfigured()) {
      return;
    }

    try {
      httpStatisticsManager.rebuildStatistics(statisticsToBeRebuilt);
    } catch (final Exception e) {
      LOG.warn(REMOTE_ERROR_MSG, e);
    }
  }

  @Override
  public void rebuildStatistics(final Long sourceId, final LocalDate published) {
    if (!isHttpStatisticsManagerConfigured()) {
      return;
    }

    try {
      httpStatisticsManager.rebuildStatistics(sourceId, published);
    } catch (final Exception e) {
      LOG.warn(REMOTE_ERROR_MSG, e);
    }
  }

  @Override
  public StatisticsDto getStatistics(final long projectId, final Integer numberOfDays) {
    if (!isHttpStatisticsManagerConfigured()) {
      return StatisticsDto.emptyStatistics();
    }

    try {
      return httpStatisticsManager.getStatistics(projectId, numberOfDays);
    } catch (final Exception e) {
      LOG.warn(REMOTE_ERROR_MSG, e);
      return StatisticsDto.emptyStatistics();
    }
  }

  @Override
  public <T> SparseSingleTable<Long, LocalDate, T> getStatistics(final Collection<Long> sourceIds,
      final LocalDate from, final LocalDate to,
      final Function<LocalDate, LocalDate> transformDateFunction,
      final Function<DocumentsPerState, T> transformValueFunction) {
    if (!isHttpStatisticsManagerConfigured()) {
      return new SparseSingleTable<Long, LocalDate, T>();
    }

    // Get via buffer
    try {
      final Collection<Long> sourceIdsSerializable = Sets.newHashSet(sourceIds);
      return httpStatisticsManager.getStatistics(sourceIdsSerializable, from, to,
          transformDateFunction, transformValueFunction);
    } catch (final Exception e) {
      LOG.error(REMOTE_ERROR_MSG, e);
      return new SparseSingleTable<Long, LocalDate, T>();
    }
  }

}
