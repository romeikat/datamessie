package com.romeikat.datamessie.core.processing.init;

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
import com.romeikat.datamessie.core.processing.task.documentProcessing.DocumentsProcessingTask;

@Service
public class DocumentsProcessingInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsProcessingInitializer.class);

  @Value("${processing.module.enabled}")
  private boolean moduleEnabled;

  @Value("${documents.processing.enabled}")
  private boolean documentsProcessingEnabled;

  @Value("${documents.processing.batch.pause}")
  private long pause;

  @Autowired
  private TaskManager taskManager;

  @Autowired
  private ApplicationContext ctx;

  @PostConstruct
  private void initialize() {
    if (moduleEnabled) {
      if (documentsProcessingEnabled) {
        startProcessing();
        LOG.info("Initializing documents processing");
      } else {
        LOG.info("Documents processing is disabled");
      }
    }
  }

  private void startProcessing() {
    final DocumentsProcessingTask task =
        (DocumentsProcessingTask) ctx.getBean(DocumentsProcessingTask.BEAN_NAME);
    taskManager.addTask(task, pause);
  }

}
