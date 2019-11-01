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
import com.romeikat.datamessie.core.domain.dto.RedirectingRuleDto;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRuleImpl;
import com.romeikat.datamessie.model.core.RedirectingRule;

@Repository
public class RedirectingRuleDao extends AbstractEntityWithIdAndVersionDao<RedirectingRule> {

  public RedirectingRuleDao() {
    super(RedirectingRuleImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "activeFrom";
  }

  public RedirectingRule create(final long id, final long sourceId) {
    return new RedirectingRuleImpl(id, sourceId);
  }

  public List<RedirectingRule> getOfSource(final SharedSessionContract ssc, final long sourceId) {
    // Query: RedirectingRule
    final EntityWithIdQuery<RedirectingRule> redirectingRuleQuery =
        new EntityWithIdQuery<>(RedirectingRuleImpl.class);
    redirectingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    redirectingRuleQuery.addOrder(Order.asc("activeFrom"));
    redirectingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<RedirectingRule> redirectingRules = redirectingRuleQuery.listObjects(ssc);
    return redirectingRules;
  }

  public ListMultimap<Long, RedirectingRule> getPerSourceId(final SharedSessionContract ssc,
      final Collection<Long> sourceIds) {
    // Query: RedirectingRule
    final EntityWithIdQuery<RedirectingRule> redirectingRuleQuery =
        new EntityWithIdQuery<>(RedirectingRuleImpl.class);
    redirectingRuleQuery.addRestriction(Restrictions.in("sourceId", sourceIds));
    redirectingRuleQuery.addOrder(Order.asc("activeFrom"));
    redirectingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<RedirectingRule> redirectingRules = redirectingRuleQuery.listObjects(ssc);
    final ListMultimap<Long, RedirectingRule> result = ArrayListMultimap.create();
    for (final RedirectingRule redirectingRule : redirectingRules) {
      result.put(redirectingRule.getSourceId(), redirectingRule);
    }
    return result;
  }

  public List<RedirectingRuleDto> getAsDtos(final SharedSessionContract ssc, final long sourceId) {
    // Query: RedirectingRule
    final EntityWithIdQuery<RedirectingRule> redirectingRuleQuery =
        new EntityWithIdQuery<>(RedirectingRuleImpl.class);
    redirectingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    redirectingRuleQuery.addOrder(Order.asc("activeFrom"));
    redirectingRuleQuery
        .setResultTransformer(new AliasToBeanResultTransformer(RedirectingRuleDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("regex"), "regex");
    projectionList.add(Projections.property("regexGroup"), "regexGroup");
    projectionList.add(Projections.property("activeFrom"), "activeFrom");
    projectionList.add(Projections.property("activeTo"), "activeTo");
    @SuppressWarnings("unchecked")
    final List<RedirectingRuleDto> dtos =
        (List<RedirectingRuleDto>) redirectingRuleQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

}
