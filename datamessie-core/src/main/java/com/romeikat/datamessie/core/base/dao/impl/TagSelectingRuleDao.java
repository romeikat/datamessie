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
import com.romeikat.datamessie.core.domain.dto.TagSelectingRuleDto;
import com.romeikat.datamessie.core.domain.entity.TagSelectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRuleImpl;

@Repository
public class TagSelectingRuleDao extends AbstractEntityWithIdAndVersionDao<TagSelectingRule> {

  public TagSelectingRuleDao() {
    super(TagSelectingRuleImpl.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "activeFrom";
  }

  public TagSelectingRule create(final long id, final long sourceId) {
    return new TagSelectingRuleImpl(id, sourceId);
  }

  public ListMultimap<Long, TagSelectingRule> getPerSourceId(final SharedSessionContract ssc,
      final Collection<Long> sourceIds) {
    // Query: RedirectingRule
    final EntityWithIdQuery<TagSelectingRule> tagSelectingRuleQuery =
        new EntityWithIdQuery<>(TagSelectingRuleImpl.class);
    tagSelectingRuleQuery.addRestriction(Restrictions.in("sourceId", sourceIds));
    tagSelectingRuleQuery.addOrder(Order.asc("activeFrom"));
    tagSelectingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<TagSelectingRule> tagSelectingRules = tagSelectingRuleQuery.listObjects(ssc);
    final ListMultimap<Long, TagSelectingRule> result = ArrayListMultimap.create();
    for (final TagSelectingRule tagSelectingRule : tagSelectingRules) {
      result.put(tagSelectingRule.getSourceId(), tagSelectingRule);
    }
    return result;
  }

  public List<TagSelectingRule> getOfSource(final SharedSessionContract ssc, final long sourceId) {
    // Query: TagSelectingRule
    final EntityWithIdQuery<TagSelectingRule> tagSelectingRuleQuery =
        new EntityWithIdQuery<>(TagSelectingRuleImpl.class);
    tagSelectingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    tagSelectingRuleQuery.addOrder(Order.asc("activeFrom"));
    tagSelectingRuleQuery.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

    // Done
    final List<TagSelectingRule> tagSelectingRules = tagSelectingRuleQuery.listObjects(ssc);
    return tagSelectingRules;
  }

  public List<TagSelectingRuleDto> getAsDtos(final SharedSessionContract ssc, final long sourceId) {
    // Query: TagSelectingRule
    final EntityWithIdQuery<TagSelectingRule> tagSelectingRuleQuery =
        new EntityWithIdQuery<>(TagSelectingRuleImpl.class);
    tagSelectingRuleQuery.addRestriction(Restrictions.eq("sourceId", sourceId));
    tagSelectingRuleQuery.addOrder(Order.asc("activeFrom"));
    tagSelectingRuleQuery
        .setResultTransformer(new AliasToBeanResultTransformer(TagSelectingRuleDto.class));

    // Done
    final ProjectionList projectionList = Projections.projectionList();
    projectionList.add(Projections.property("id"), "id");
    projectionList.add(Projections.property("tagSelector"), "tagSelector");
    projectionList.add(Projections.property("activeFrom"), "activeFrom");
    projectionList.add(Projections.property("activeTo"), "activeTo");
    @SuppressWarnings("unchecked")
    final List<TagSelectingRuleDto> dtos =
        (List<TagSelectingRuleDto>) tagSelectingRuleQuery.listForProjection(ssc, projectionList);
    return dtos;
  }

}
