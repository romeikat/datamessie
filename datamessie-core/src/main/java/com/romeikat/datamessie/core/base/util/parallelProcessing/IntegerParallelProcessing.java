package com.romeikat.datamessie.core.base.util.parallelProcessing;

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

import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;

public abstract class IntegerParallelProcessing extends ParallelProcessing<Integer> {

  public IntegerParallelProcessing(final SessionFactory sessionFactory, final int start,
      final int end) {
    super(sessionFactory, getList(start, end));
  }

  public IntegerParallelProcessing(final SessionFactory sessionFactory, final int start,
      final int end, final Double parallelismFactor) {
    super(sessionFactory, getList(start, end), parallelismFactor);
  }

  private static List<Integer> getList(final int start, final int end) {
    final List<Integer> list = new ArrayList<Integer>();
    for (int i = start; i <= end; i++) {
      list.add(i);
    }
    return list;
  }

}
