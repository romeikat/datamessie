package com.romeikat.datamessie.core.base.task.management;

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

import java.util.Date;
import com.romeikat.datamessie.core.base.util.converter.DateConverter;

public class TaskExecutionWork {

  private String message;

  private Long start;

  private Long end;

  public TaskExecutionWork() {
    this(null);
  }

  public TaskExecutionWork(final String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public Long getStart() {
    return start;
  }

  public void setStart(final Long start) {
    this.start = start;
  }

  public Long getEnd() {
    return end;
  }

  public void setEnd(final Long end) {
    this.end = end;
  }

  public Long getDuration() {
    if (start == null || end == null) {
      return null;
    }
    final long duration = end - start;
    return duration;
  }

  public Long getLatestActivity() {
    if (end != null) {
      return end;
    }
    if (start != null) {
      return start;
    }
    return null;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();
    // Start
    final Long start = getStart();
    if (start != null) {
      result.append(DateConverter.INSTANCE_UI.convertToString(new Date(start)));
      result.append(": ");
    }
    // Message
    final String message = getMessage();
    if (message != null) {
      result.append(message);
    }
    // Duration
    final Long duration = getDuration();
    if (duration != null) {
      final long seconds = Math.round(duration / 1000d);
      result.append(" (");
      result.append(seconds);
      result.append(" s)");
    }
    // Done
    return result.toString();
  }

}
