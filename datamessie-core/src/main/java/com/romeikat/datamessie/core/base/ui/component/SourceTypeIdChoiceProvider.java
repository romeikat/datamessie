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

import java.util.List;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;

import com.romeikat.datamessie.core.base.dao.impl.SourceTypeDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.dto.SourceTypeDto;

public class SourceTypeIdChoiceProvider extends AbstractIdBasedChoiceProvider<SourceTypeDto> {

  private static final long serialVersionUID = 1L;

  @SpringBean
  private SourceTypeDao sourceTypeDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public SourceTypeIdChoiceProvider() {
    super();
    Injector.get().inject(this);
  }

  @Override
  protected List<SourceTypeDto> provideChoices() {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
    final List<SourceTypeDto> choices = sourceTypeDao.getAsDtos(sessionProvider.getStatelessSession());
    sessionProvider.closeStatelessSession();
    return choices;
  }

  @Override
  protected long provideId(final SourceTypeDto choice) {
    return choice.getId();
  }

  @Override
  protected String provideDisplayText(final SourceTypeDto choice) {
    return choice.getName();
  }

}
