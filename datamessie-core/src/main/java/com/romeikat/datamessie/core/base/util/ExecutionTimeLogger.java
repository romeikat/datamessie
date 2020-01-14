package com.romeikat.datamessie.core.base.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2020 Dr. Raphael Romeikat
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

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTimeLogger {

  private final Logger log;
  private final boolean enabled;

  private final StopWatch sw;

  public ExecutionTimeLogger(final Class<?> clazz) {
    log = LoggerFactory.getLogger(clazz);
    enabled = log.isDebugEnabled();

    sw = new StopWatch();
  }

  public void start() {
    if (!enabled) {
      return;
    }

    sw.start();
  }

  public void log(final String section) {
    if (!enabled) {
      return;
    }

    log.debug("{}: {}s", section, sw.getTime() / 1000);
  }

  public void stop() {
    if (!enabled) {
      return;
    }

    sw.stop();
  }

}
