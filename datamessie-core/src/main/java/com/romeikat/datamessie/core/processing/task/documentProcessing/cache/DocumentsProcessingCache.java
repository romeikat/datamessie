package com.romeikat.datamessie.core.processing.task.documentProcessing.cache;

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

import java.util.Collection;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.impl.RawContentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;

public class DocumentsProcessingCache {

  private final RawContentDao rawContentDao;
  private final SourceDao sourceDao;
  private final SessionFactory sessionFactory;

  private final DocumentsWithRawContentsCache documentsWithRawContentsCache;
  private final DocumentsWithSourcesCache documentsWithSourcesCache;
  private final SourcesWithTagSelectingRulesCache sourcesWithTagSelectingRulesCache;

  public DocumentsProcessingCache(final Collection<Document> documents,
      final ApplicationContext ctx) {
    sessionFactory = ctx.getBean("sessionFactory", SessionFactory.class);
    rawContentDao = ctx.getBean(RawContentDao.class);
    sourceDao = ctx.getBean(SourceDao.class);

    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    documentsWithRawContentsCache = new DocumentsWithRawContentsCache(
        sessionProvider.getStatelessSession(), rawContentDao, documents);
    documentsWithSourcesCache =
        new DocumentsWithSourcesCache(sessionProvider.getStatelessSession(), sourceDao, documents);
    sourcesWithTagSelectingRulesCache = new SourcesWithTagSelectingRulesCache(ctx);
    sessionProvider.closeStatelessSession();
  }

  public RawContent getRawContent(final long documentId) {
    return documentsWithRawContentsCache.get(documentId);
  }

  public Source getSource(final long documentId) {
    return documentsWithSourcesCache.get(documentId);
  }

  public List<TagSelectingRule> getTagSelectingRules(final SharedSessionContract ssc,
      final long sourceId) {
    return sourcesWithTagSelectingRulesCache.getValue(ssc, sourceId);
  }

}
