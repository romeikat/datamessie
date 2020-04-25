package com.romeikat.datamessie.core.base.dao.impl;

import java.util.Collection;
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
import org.hibernate.Criteria;
import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.dto.DeletingRuleDto;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;

@Repository
public class DeletingRuleDao extends AbstractEntityWithIdAndVersionDao<DeletingRule> {

  public DeletingRuleDao() {
    super(DeletingRule.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "activeFrom";
  }

  public ListMultimap<Long, DeletingRule> getPerSourceId(final SharedSessionContract ssc,
      final Collection<Long> sourceIds) {
    // Query: RedirectingRule
    final EntityWithIdQuery<DeletingRule> deletingRuleQuery =
        new EntityWithIdQuery<>(DeletingRule.class);
    deletingRuleQuery.addRestriction(Restrictions.in("sourceId", sourceIds));
    deletingRuleQuery.addOrder(Order.asc("position"));
    deletingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<DeletingRule> deletingRules = deletingRuleQuery.listObjects(ssc);
    final ListMultimap<Long, DeletingRule> result = ArrayListMultimap.create();
    for (final DeletingRule deletingRule : deletingRules) {
      result.put(deletingRule.getSourceId(), deletingRule);
    }
    return result;
  }

  public List<DeletingRule> getOfSource(final SharedSessionContract ssc, final long sourceId) {
    // Query: DeletingRule
    final EntityWithIdQuery<DeletingRule> deletingRuleQuery =
        new EntityWithIdQuery<>(DeletingRule.class);
    deletingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    deletingRuleQuery.addOrder(Order.asc("position"));
    deletingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<DeletingRule> deletingRules = deletingRuleQuery.listObjects(ssc);
    return deletingRules;
  }

  public List<DeletingRuleDto> getAsDtos(final SharedSessionContract ssc, final long sourceId) {
    // Query: DeletingRule
    final EntityWithIdQuery<DeletingRule> deletingRuleQuery =
        new EntityWithIdQuery<>(DeletingRule.class);
    deletingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    deletingRuleQuery.addOrder(Order.asc("position"));
    deletingRuleQuery.setResultTransformer(new AliasToBeanResultTransformer(DeletingRuleDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("selector"), "selector");
    projectionList.add(Projections.property("activeFrom"), "activeFrom");
    projectionList.add(Projections.property("activeTo"), "activeTo");
    projectionList.add(Projections.property("mode"), "mode");
    @SuppressWarnings("unchecked")
    final List<DeletingRuleDto> dtos =
        (List<DeletingRuleDto>) deletingRuleQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

}
