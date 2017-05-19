package com.romeikat.datamessie.core.processing.task.documentReindexing;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.task.BackgroundTask;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;

@Service(DocumentsReindexingTask.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentsReindexingTask extends BackgroundTask {

  public static final String BEAN_NAME = "documentsReindexingTask";

  public static final String NAME = "Documents reindexing";

  @Autowired
  private DocumentsReindexer documentsReindexer;

  private DocumentsReindexingTask() {}

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isVisibleAfterCompleted() {
    return true;
  }

  @Override
  public void execute(final TaskExecution taskExecution) throws Exception {
    // Start rebuilding
    documentsReindexer.performReindexing(taskExecution);
  }

}
