package com.romeikat.datamessie.core.sync.init;

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

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.task.management.TaskManager;
import com.romeikat.datamessie.core.sync.task.SyncTask;

@Service
public class SyncInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(SyncInitializer.class);

  @Value("${sync.module.enabled}")
  private boolean moduleEnabled;

  @Value("${sync.enabled}")
  private boolean syncEnabled;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  private ApplicationContext ctx;

  @PostConstruct
  private void initialize() {
    if (moduleEnabled) {
      if (syncEnabled) {
        startProcessing();
        LOG.info("Initializing database synchronization");
      } else {
        LOG.info("Database synchronization is disabled");
      }
    }
  }

  private void startProcessing() {
    final SyncTask task = (SyncTask) ctx.getBean(SyncTask.BEAN_NAME);
    taskManager.addTask(task);
  }

}
