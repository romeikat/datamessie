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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.AbstractTest;
import jersey.repackaged.com.google.common.collect.Lists;

public class DateRangeTest extends AbstractTest {

  @Test
  public void combinesRangesWithXor_equal() {
    final DateRange dateRange =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange, dateRange);

    assertTrue(result.isEmpty());
  }

  @Test
  public void combinesRangesWithXor_overlapSameFromClosed() {
    final DateRange dateRange1 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
    final DateRange dateRange2 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 30));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(1, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
        result.get(0));
  }

  @Test
  public void combinesRangesWithXor_overlapSameFromOpen() {
    final DateRange dateRange1 = DateRange.create(null, LocalDate.of(2020, 5, 31));
    final DateRange dateRange2 = DateRange.create(null, LocalDate.of(2020, 6, 30));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(1, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30)),
        result.get(0));
  }

  @Test
  public void combinesRangesWithXor_overlapSameToClosed() {
    final DateRange dateRange1 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 30));
    final DateRange dateRange2 =
        DateRange.create(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 30));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(1, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
        result.get(0));
  }

  @Test
  public void combinesRangesWithXor_overlapSameToOpen() {
    final DateRange dateRange1 = DateRange.create(LocalDate.of(2020, 5, 1), null);
    final DateRange dateRange2 = DateRange.create(LocalDate.of(2020, 6, 1), null);

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(1, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
        result.get(0));
  }

  @Test
  public void combinesRangesWithXor_overlapClosed() {
    final DateRange dateRange1 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 6, 30));
    final DateRange dateRange2 =
        DateRange.create(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(2, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
        result.get(0));
    assertEquals(DateRange.create(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
        result.get(1));
  }

  @Test
  public void combinesRangesWithXor_overlapOpen() {
    final DateRange dateRange1 = DateRange.create(null, LocalDate.of(2020, 6, 30));
    final DateRange dateRange2 = DateRange.create(LocalDate.of(2020, 6, 1), null);

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(2, result.size());
    assertEquals(DateRange.create(null, LocalDate.of(2020, 5, 31)), result.get(0));
    assertEquals(DateRange.create(LocalDate.of(2020, 7, 1), null), result.get(1));
  }

  @Test
  public void combinesRangesWithXor_noOverlapClosed() {
    final DateRange dateRange1 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
    final DateRange dateRange2 =
        DateRange.create(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31));

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(2, result.size());
    assertEquals(DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31)),
        result.get(0));
    assertEquals(DateRange.create(LocalDate.of(2020, 7, 1), LocalDate.of(2020, 7, 31)),
        result.get(1));
  }

  @Test
  public void combinesRangesWithXor_noOverlapOpen() {
    final DateRange dateRange1 = DateRange.create(null, LocalDate.of(2020, 5, 31));
    final DateRange dateRange2 = DateRange.create(LocalDate.of(2020, 7, 1), null);

    final List<DateRange> result = DateRange.combineRangesWithXor(dateRange1, dateRange2);

    // Provided date range applies
    assertEquals(2, result.size());
    assertEquals(DateRange.create(null, LocalDate.of(2020, 5, 31)), result.get(0));
    assertEquals(DateRange.create(LocalDate.of(2020, 7, 1), null), result.get(1));
  }

  @Test
  public void appliesTo() {
    final DateRange dateRange =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31));
    final Collection<LocalDate> dates = Sets.newHashSet(LocalDate.of(2020, 4, 1),
        LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 31), LocalDate.of(2020, 6, 1));

    final Collection<LocalDate> result = dateRange.applyTo(dates);

    // Provided date range applies
    assertEquals(2, result.size());
    assertTrue(result.contains(LocalDate.of(2020, 5, 1)));
    assertTrue(result.contains(LocalDate.of(2020, 5, 31)));
  }

  @Test
  public void appliesDateRangesTo() {
    final DateRange dateRange1 =
        DateRange.create(LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 10));
    final DateRange dateRange2 =
        DateRange.create(LocalDate.of(2020, 5, 21), LocalDate.of(2020, 5, 30));
    final Collection<DateRange> dateRanges = Lists.newArrayList(dateRange1, dateRange2);
    final Collection<LocalDate> dates = Sets.newHashSet(LocalDate.of(2020, 4, 1),
        LocalDate.of(2020, 5, 1), LocalDate.of(2020, 5, 11), LocalDate.of(2020, 5, 21),
        LocalDate.of(2020, 5, 31), LocalDate.of(2020, 6, 1));

    final Collection<LocalDate> result = DateRange.applyDateRangesTo(dateRanges, dates);

    // Provided date range applies
    assertEquals(2, result.size());
    assertTrue(result.contains(LocalDate.of(2020, 5, 1)));
    assertTrue(result.contains(LocalDate.of(2020, 5, 21)));
  }

}
