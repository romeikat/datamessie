package com.romeikat.datamessie.core.statistics.task;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2018 Dr. Raphael Romeikat
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

public class StatisticsStatus {

  private final String sourceName;

  private long successfulToday;
  private long successfulMinus1;
  private long successfulMinus7;

  public StatisticsStatus(final String sourceName) {
    this.sourceName = sourceName;
  }

  public String getSourceName() {
    return sourceName;
  }

  public long getSuccessfulToday() {
    return successfulToday;
  }

  public void setSuccessfulToday(final long successfulToday) {
    this.successfulToday = successfulToday;
  }

  public long getSuccessfulMinus1() {
    return successfulMinus1;
  }

  public void setSuccessfulMinus1(final long successfulMinus1) {
    this.successfulMinus1 = successfulMinus1;
  }

  public long getSuccessfulMinus7() {
    return successfulMinus7;
  }

  public void setSuccessfulMinus7(final long successfulMinus7) {
    this.successfulMinus7 = successfulMinus7;
  }

}
