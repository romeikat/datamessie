package com.romeikat.datamessie.core.base.ui.component;

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
import java.util.Map;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;
import org.wicketstuff.select2.Response;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.dao.impl.SourceTypeDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;

public class SourceTypeChoiceProvider extends AbstractChoiceProvider<SourceTypeDto> {

  private static final long serialVersionUID = 1L;

  @SpringBean
  private SourceTypeDao sourceTypeDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourceTypeChoiceProvider() {
    super();
    Injector.get().inject(this);
  }

  @Override
  public String getDisplayValue(final SourceTypeDto choice) {
    return choice.getName();
  }

  @Override
  public String getIdValue(final SourceTypeDto choice) {
    return String.valueOf(choice.getId());
  }

  @Override
  public void query(final String term, final int page, final Response<SourceTypeDto> response) {
    for (final SourceTypeDto sourceType : getSourceTypes().values()) {
      final String existingTerm = sourceType.getName().toLowerCase();
      final String searchTerm = term.toLowerCase();
      if (existingTerm.contains(searchTerm)) {
        response.add(sourceType);
      }
    }
  }

  @Override
  public Collection<SourceTypeDto> toChoices(final Collection<String> ids) {
    final Map<Long, SourceTypeDto> sourceTypes = getSourceTypes();
    final Function<String, SourceTypeDto> toChoicesFunction = new Function<String, SourceTypeDto>() {

      @Override
      public SourceTypeDto apply(final String id) {
        return sourceTypes.get(Long.parseLong(id));
      }

    };
    final Collection<SourceTypeDto> choices = Collections2.transform(ids, toChoicesFunction);
    return choices;
  }

  private Map<Long, SourceTypeDto> getSourceTypes() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final List<SourceTypeDto> sourceTypes = sourceTypeDao.getAsDtos(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();

    final Map<Long, SourceTypeDto> sourceTypesMap = Maps.newHashMapWithExpectedSize(sourceTypes.size());
    for (final SourceTypeDto sourceType : sourceTypes) {
      sourceTypesMap.put(sourceType.getId(), sourceType);
    }
    return sourceTypesMap;
  }

}
