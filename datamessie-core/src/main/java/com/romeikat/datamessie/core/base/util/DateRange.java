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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;
import jersey.repackaged.com.google.common.base.Objects;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Represents an open or closed range betweem two boundary dates. An open end is represented by a
 * {@code null} boundary date, a closed end by a specific date.
 *
 * @author Dr. Raphael Romeikat
 *
 */
public class DateRange implements Comparable<DateRange> {

  private final LocalDate from;
  private final LocalDate to;

  private DateRange(final LocalDate from, final LocalDate to) {
    this.from = from;
    this.to = to;
  }

  public static DateRange create(final LocalDate from, final LocalDate to) {
    return new DateRange(from, to);
  }

  /**
   * Calculates the date range/s that only appear/s in exactly one out of two provided date ranges.
   * The result may:
   * <ul>
   * <li>be empty (if the provided date ranges are equal)</li>
   * <li>contain one date range (in case the provided date ranges overlap sharing a common from xor
   * to date)</li>
   * <li>contain two date ranges (in case the two provided date ranges overlap in another way or
   * don't overlap at all)</li>
   * </ul>
   *
   * @param dateRange1
   * @param dateRange2
   * @return
   */
  public static List<DateRange> combineRangesWithXor(final DateRange dateRange1,
      final DateRange dateRange2) {
    // Order by from date (first) and to date (second)
    final ArrayList<DateRange> dateRanges = Lists.newArrayList(dateRange1, dateRange2);
    Collections.sort(dateRanges, new AscendingComparator<DateRange>());
    final DateRange earlier = dateRanges.get(0);
    final DateRange later = dateRanges.get(1);

    final boolean noOverlap = earlier.getTo() != null && later.getFrom() != null
        && earlier.getTo().isBefore(later.getFrom());

    final List<DateRange> result = Lists.newArrayListWithExpectedSize(2);

    // No overlap => use both ranges
    if (noOverlap) {
      result.add(dateRange1);
      result.add(dateRange2);
    }

    // Overlap => determine non-overlapping ranges
    else {
      // Equal => no mutually exclusive dates at all
      final boolean equal = earlier.equals(later);
      if (equal) {
      }

      // Same from => mutually exclusive range at end
      else if (Objects.equal(earlier.getFrom(), later.getFrom())) {
        final LocalDate from = getEarlierTo(earlier.getTo(), later.getTo()).plusDays(1);
        final LocalDate to = getLaterTo(earlier.getTo(), later.getTo());
        final DateRange resultEnd = new DateRange(from, to);
        result.add(resultEnd);
      }

      // Same to => mutually exclusive range at begin
      else if (Objects.equal(earlier.getTo(), later.getTo())) {
        final LocalDate from = getEarlierFrom(earlier.getFrom(), later.getFrom());
        final LocalDate to = getLaterFrom(earlier.getFrom(), later.getFrom()).minusDays(1);
        final DateRange reeultBegin = new DateRange(from, to);
        result.add(reeultBegin);
      }

      // Same to => mutually exclusive range at begin and end
      else {
        final DateRange resultBegin =
            new DateRange(earlier.getFrom(), later.getFrom().minusDays(1));
        result.add(resultBegin);

        final DateRange resultEnd = new DateRange(earlier.getTo().plusDays(1), later.getTo());
        result.add(resultEnd);
      }
    }

    return result;
  }

  private static LocalDate getEarlierTo(final LocalDate to1, final LocalDate to2) {
    if (to1 == null && to2 != null) {
      return to2;
    } else if (to1 != null && to2 == null) {
      return to1;
    } else if (to1 != null && to2 != null) {
      return to1.isBefore(to2) ? to1 : to2;
    } else {
      return null;
    }
  }

  private static LocalDate getLaterTo(final LocalDate to1, final LocalDate to2) {
    if (to1 == null && to2 != null) {
      return to1;
    } else if (to1 != null && to2 == null) {
      return to2;
    } else if (to1 != null && to2 != null) {
      return to1.isAfter(to2) ? to1 : to2;
    } else {
      return null;
    }
  }

  private static LocalDate getEarlierFrom(final LocalDate from1, final LocalDate from2) {
    if (from1 == null && from2 != null) {
      return from1;
    } else if (from1 != null && from2 == null) {
      return from2;
    } else if (from1 != null && from2 != null) {
      return from1.isBefore(from2) ? from1 : from2;
    } else {
      return null;
    }
  }

  private static LocalDate getLaterFrom(final LocalDate from1, final LocalDate from2) {
    if (from1 == null && from2 != null) {
      return from2;
    } else if (from1 != null && from2 == null) {
      return from1;
    } else if (from1 != null && from2 != null) {
      return from1.isAfter(from2) ? from1 : from2;
    } else {
      return null;
    }
  }

  public Set<LocalDate> applyTo(final Collection<LocalDate> dates) {
    final Predicate<LocalDate> isContainedInDateRangePredicate = new Predicate<LocalDate>() {
      @Override
      public boolean test(final LocalDate date) {
        final boolean fromOk = from == null || !date.isBefore(from);
        if (!fromOk) {
          return false;
        }

        final boolean toOk = to == null || !date.isAfter(to);
        if (!toOk) {
          return false;
        }

        return true;
      }
    };
    return dates.stream().filter(isContainedInDateRangePredicate).collect(Collectors.toSet());
  }

  public static Set<LocalDate> applyDateRangesTo(final Collection<DateRange> dateRanges,
      final Collection<LocalDate> dates) {
    final Set<LocalDate> result = Sets.newHashSet();

    for (final DateRange dateRange : dateRanges) {
      final Collection<LocalDate> subResult = dateRange.applyTo(dates);
      result.addAll(subResult);
    }

    return result;
  }

  public LocalDate getFrom() {
    return from;
  }

  public LocalDate getTo() {
    return to;
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    final DateRange otherDateRange = (DateRange) other;
    final boolean equals = new EqualsBuilder().append(from, otherDateRange.from)
        .append(to, otherDateRange.to).isEquals();
    return equals;
  }

  @Override
  public int hashCode() {
    final int hashCode = new HashCodeBuilder().append(from).append(to).toHashCode();
    return hashCode;
  }

  @Override
  public int compareTo(final DateRange other) {
    // Prio 1: from
    if (from == null && other.from != null) {
      return -1;
    } else if (from != null && other.from == null) {
      return 1;
    }

    // Prio 2: to
    if (to == null && other.to != null) {
      return -1;
    } else if (to != null && other.to == null) {
      return 1;
    }

    // Equal
    return 0;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(getClass().getSimpleName());
    result.append(" [");
    result.append(from);
    result.append(" - ");
    result.append(to);
    result.append("]");

    return result.toString();
  }

}
