package com.romeikat.datamessie.core.base.dao.impl;

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
import javax.annotation.Resource;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionDto;
import com.romeikat.datamessie.core.domain.dto.TaskExecutionWorkDto;

@Repository
public class TaskExecutionDao {

  @Resource
  private TaskManager taskManager;

  public List<TaskExecutionDto> getVisibleTaskExecutionsOrderedByLatestActivityDesc() {
    final List<TaskExecutionDto> taskExecutionDtos = new ArrayList<TaskExecutionDto>();
    // Task executions
    final List<TaskExecution> taskExecutionsOrderedByLatestActivityDesc =
        taskManager.getVisibleTaskExecutionsOrderedByLatestActivityDesc();
    for (final TaskExecution taskExecution : taskExecutionsOrderedByLatestActivityDesc) {
      final TaskExecutionDto taskExecutionDto = new TaskExecutionDto();
      // Status
      taskExecutionDto.setId(taskExecution.getId());
      taskExecutionDto.setName(taskExecution.getName());
      taskExecutionDto.setStatus(taskExecution.getStatus());
      // Task execution works
      final List<TaskExecutionWorkDto> workDtos = new ArrayList<TaskExecutionWorkDto>();
      final List<TaskExecutionWork> taskExecutionWorks = taskExecution.getWorks();
      for (final TaskExecutionWork taskExecutionWork : taskExecutionWorks) {
        final TaskExecutionWorkDto taskExecutionWorkDto = new TaskExecutionWorkDto();
        taskExecutionWorkDto.setMessage(taskExecutionWork.getMessage());
        taskExecutionWorkDto.setStart(taskExecutionWork.getStart());
        taskExecutionWorkDto.setDuration(taskExecutionWork.getDuration());
        workDtos.add(taskExecutionWorkDto);
      }
      taskExecutionDto.setWorks(workDtos);
      // Add
      taskExecutionDtos.add(taskExecutionDto);
    }
    // Done
    return taskExecutionDtos;
  }

}
