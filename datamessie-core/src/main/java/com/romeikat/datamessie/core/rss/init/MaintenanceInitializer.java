package com.romeikat.datamessie.core.rss.init;

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

import java.time.LocalTime;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.task.Task;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.base.task.scheduling.DaytimeTaskSchedulingThread;
import com.romeikat.datamessie.core.rss.task.maintenance.MaintenanceTask;

@Service
public class MaintenanceInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(MaintenanceInitializer.class);

  @Value("${rss.module.enabled}")
  private boolean moduleEnabled;

  @Value("${maintenance.enabled}")
  private boolean maintenanceEnabled;

  @Value("${maintenance.daytime}")
  private String maintenanceDaytime;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  private ApplicationContext ctx;

  @PostConstruct
  private void initialize() {
    if (moduleEnabled) {
      if (maintenanceEnabled) {
        LOG.info("Initializing maintenance");
        scheduleMaintenance();
      } else {
        LOG.info("Maintenance is disabled");
      }
    }
  }

  private void scheduleMaintenance() {
    new DaytimeTaskSchedulingThread() {

      @Override
      protected Task getTask() {
        final MaintenanceTask task = (MaintenanceTask) ctx.getBean(MaintenanceTask.BEAN_NAME);
        return task;
      }

      @Override
      protected String getTaskName() {
        return MaintenanceTask.NAME;
      }

      @Override
      protected LocalTime getTaskExecutionDaytime() {
        return LocalTime.parse(maintenanceDaytime);
      }

      @Override
      protected TaskManager getTaskManager() {
        return taskManager;
      }

    }.start();
  }

}
