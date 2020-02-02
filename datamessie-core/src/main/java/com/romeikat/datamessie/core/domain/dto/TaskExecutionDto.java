package com.romeikat.datamessie.core.domain.dto;

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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;

public class TaskExecutionDto implements Serializable {

  private static final long serialVersionUID = 1L;

  private long id;

  private String name;

  private LocalDateTime created;

  private TaskExecutionStatus status;

  private List<TaskExecutionWorkDto> works;

  private Serializable result;

  public long getId() {
    return id;
  }

  public void setId(final long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public void setCreated(final LocalDateTime created) {
    this.created = created;
  }

  public TaskExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(final TaskExecutionStatus status) {
    this.status = status;
  }

  public List<TaskExecutionWorkDto> getWorks() {
    return works;
  }

  public void setWorks(final List<TaskExecutionWorkDto> works) {
    this.works = works;
  }

  public Serializable getResult() {
    return result;
  }

  public void setResult(final Serializable result) {
    this.result = result;
  }

}
