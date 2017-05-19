package com.romeikat.datamessie.core.base.util;

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

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.romeikat.datamessie.core.AbstractTest;
import com.romeikat.datamessie.core.base.util.DateUtil;

public class DateUtilTest extends AbstractTest {

  @Test
  public void getsLocalDatesBetween_ascending() throws Exception {
    final LocalDate localDate1 = LocalDate.of(2016, 2, 28);
    final LocalDate localDate2 = LocalDate.of(2016, 3, 1);

    final List<LocalDate> actual = DateUtil.getLocalDatesBetween(localDate1, localDate2);
    final List<LocalDate> expected =
        Lists.newArrayList(LocalDate.of(2016, 2, 28), LocalDate.of(2016, 2, 29), LocalDate.of(2016, 3, 1));

    assertEquals(expected, actual);
  }

  @Test
  public void getsLocalDatesBetween_descending() throws Exception {
    final LocalDate localDate1 = LocalDate.of(2016, 3, 1);
    final LocalDate localDate2 = LocalDate.of(2016, 2, 28);

    final List<LocalDate> actual = DateUtil.getLocalDatesBetween(localDate1, localDate2);
    final List<LocalDate> expected =
        Lists.newArrayList(LocalDate.of(2016, 3, 1), LocalDate.of(2016, 2, 29), LocalDate.of(2016, 2, 28));

    assertEquals(expected, actual);
  }

  @Test
  public void getsLocalDatesBetween_equals() throws Exception {
    final LocalDate localDate1 = LocalDate.of(2016, 2, 28);
    final LocalDate localDate2 = LocalDate.of(2016, 2, 28);

    final List<LocalDate> actual = DateUtil.getLocalDatesBetween(localDate1, localDate2);
    final List<LocalDate> expected = Lists.newArrayList(LocalDate.of(2016, 2, 28));

    assertEquals(expected, actual);
  }

  @Test
  public void getsLocalDatesBetween_oneYear() throws Exception {
    final LocalDate localDate1 = LocalDate.of(2015, 1, 1);
    final LocalDate localDate2 = LocalDate.of(2015, 12, 31);

    final int actual = DateUtil.getLocalDatesBetween(localDate1, localDate2).size();
    final int expected = 365;

    assertEquals(expected, actual);
  }

}
