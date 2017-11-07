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

import java.util.List;
import org.hibernate.SharedSessionContract;
import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.cache.AbstractLazyCache;
import com.romeikat.datamessie.core.base.dao.impl.TagSelectingRuleDao;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;

public class SourcesWithTagSelectingRulesCache
    extends AbstractLazyCache<Long, List<TagSelectingRule>, SharedSessionContract> {

  private final TagSelectingRuleDao tagSelectingRuleDao;

  public SourcesWithTagSelectingRulesCache(final ApplicationContext ctx) {
    tagSelectingRuleDao = ctx.getBean(TagSelectingRuleDao.class);
  }

  @Override
  protected List<TagSelectingRule> loadValue(final SharedSessionContract ssc, final Long sourceId) {
    // Source ID -> tag selecting rules
    final List<TagSelectingRule> tagSelectingRules = tagSelectingRuleDao.getOfSource(ssc, sourceId);
    return tagSelectingRules;
  }


}
