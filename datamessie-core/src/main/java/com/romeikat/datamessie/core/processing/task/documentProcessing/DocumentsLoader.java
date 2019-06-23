package com.romeikat.datamessie.core.processing.task.documentProcessing;

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

import java.time.LocalDate;
import java.util.List;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.StringUtil;
import com.romeikat.datamessie.core.base.util.converter.LocalDateConverter;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.processing.dao.DocumentDao;

@Service
public class DocumentsLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentsLoader.class);

  @Value("${documents.processing.batch.size}")
  private int batchSize;

  private TaskExecutionWork work;

  @Autowired
  @Qualifier("processingDocumentDao")
  private DocumentDao documentDao;

  @Autowired
  private StringUtil stringUtil;

  private DocumentsLoader() {}

  public List<Document> loadDocumentsToProcess(final StatelessSession statelessSession,
      final TaskExecution taskExecution, final LocalDate downloadedDate)
      throws TaskCancelledException {
    try {
      // Load documents
      final List<Document> documentsToProcess =
          documentDao.getToProcess(statelessSession, downloadedDate, batchSize);

      // Logging
      final String singularPlural =
          stringUtil.getSingularOrPluralTerm("document", documentsToProcess.size());
      work = taskExecution.reportWorkStart(
          String.format("Loaded %s %s to process with download date %s", documentsToProcess.size(),
              singularPlural, LocalDateConverter.INSTANCE_UI.convertToString(downloadedDate)));
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
      return documentsToProcess;
    } catch (final Exception e) {
      taskExecution.reportWorkEnd(work);
      taskExecution.reportWork("Could not load documents to process");
      LOG.error("Could not load documents to process", e);
      return null;
    }
  }

}
