package com.romeikat.datamessie.core.base.util.comparator;

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

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;

public class CollatorComparator implements Comparator<String> {

  public static final CollatorComparator INSTANCE = new CollatorComparator();

  private final Collator collator = Collator.getInstance();

  private CollatorComparator() {}

  @Override
  public int compare(final String string1, final String string2) {
    final CollationKey nameKey1 = collator.getCollationKey(string1);
    final CollationKey nameKey2 = collator.getCollationKey(string2);
    return nameKey1.compareTo(nameKey2);
  }

}
