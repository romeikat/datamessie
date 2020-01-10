package com.romeikat.datamessie.core.processing.util;

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

import java.time.LocalDate;

public class ProcessingDates {

  private LocalDate numbersToDate;

  private LocalDate processingFromDate;
  private LocalDate processingToDate;

  public ProcessingDates() {}

  public ProcessingDates(final LocalDate numbersToDate, final LocalDate processingFromDate,
      final LocalDate processingToDate) {
    this.numbersToDate = numbersToDate;

    this.processingFromDate = processingFromDate;
    this.processingToDate = processingToDate;
  }

  public LocalDate getNumbersToDate() {
    return numbersToDate;
  }

  public void setNumbersToDate(final LocalDate numbersToDate) {
    this.numbersToDate = numbersToDate;
  }

  public LocalDate getProcessingFromDate() {
    return processingFromDate;
  }

  public void setProcessingFromDate(final LocalDate processingFromDate) {
    this.processingFromDate = processingFromDate;
  }

  public LocalDate getProcessingToDate() {
    return processingToDate;
  }

  public void setProcessingToDate(final LocalDate processingToDate) {
    this.processingToDate = processingToDate;
  }

}
