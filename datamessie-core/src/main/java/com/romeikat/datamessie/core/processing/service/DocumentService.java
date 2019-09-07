package com.romeikat.datamessie.core.processing.service;

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
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

@Service("processingDocumentService")
public class DocumentService extends com.romeikat.datamessie.core.base.service.DocumentService {

  @Autowired
  @Qualifier("documentDao")
  private DocumentDao documentDao;

  public void updateDocument(final StatelessSession statelessSession, final Document document,
      final String url, final LocalDateTime downloaded, final DocumentProcessingState state,
      final Integer statusCode) {
    if (document == null) {
      return;
    }

    if (StringUtils.isNotBlank(url)) {
      document.setUrl(url);
    }
    if (downloaded != null) {
      document.setDownloaded(downloaded);
    }
    if (state != null) {
      document.setState(state);
    }
    if (statusCode != null) {
      document.setStatusCode(statusCode);
    }

    documentDao.update(statelessSession, document);
  }

  public void updateDocument(final StatelessSession statelessSession, final Document document,
      final DocumentProcessingState state) {
    if (document == null) {
      return;
    }

    if (state != null) {
      document.setState(state);
    }

    documentDao.update(statelessSession, document);
  }

}
