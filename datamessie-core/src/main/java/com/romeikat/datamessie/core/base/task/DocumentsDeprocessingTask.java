package com.romeikat.datamessie.core.base.task;

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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.romeikat.datamessie.core.base.service.DocumentService;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Service(DocumentsDeprocessingTask.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentsDeprocessingTask implements Task {

  public static final String BEAN_NAME = "documentsDeprocessingTask";

  public static final String NAME = "Documents deprocessing";

  private final long sourceId;

  private final DocumentProcessingState targetState;

  @Autowired
  @Qualifier("documentService")
  private DocumentService documentService;

  @Autowired
  private SessionFactory sessionFactory;

  private DocumentsDeprocessingTask(final long sourceId, final DocumentProcessingState targetState) {
    this.sourceId = sourceId;
    this.targetState = targetState;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isVisibleAfterCompleted() {
    return true;
  }

  @Override
  public Integer getPriority() {
    return 4;
  }

  @Override
  public void execute(final TaskExecution taskExecution) throws TaskCancelledException {
    // Deprocess documents of source
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    documentService.deprocessDocumentsOfSource(sessionProvider.getStatelessSession(), taskExecution, sourceId,
        targetState);
    sessionProvider.closeStatelessSession();
  }

  public long getSourceId() {
    return sourceId;
  }

  public DocumentProcessingState getTargetState() {
    return targetState;
  }

}
