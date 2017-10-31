package com.romeikat.datamessie.core.base.ui.panel;

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

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.SessionFactory;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.dao.impl.DocumentDao;
import com.romeikat.datamessie.core.base.dao.impl.SourceDao;
import com.romeikat.datamessie.core.base.service.AuthenticationService.DataMessieRoles;
import com.romeikat.datamessie.core.base.ui.page.StatisticsPage;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;
import com.romeikat.datamessie.core.base.util.comparator.AscendingComparator;
import com.romeikat.datamessie.core.base.util.comparator.DescendingComparator;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.base.util.sparsetable.ISingleTable;
import com.romeikat.datamessie.core.base.util.sparsetable.MapValueKeyComparator;
import com.romeikat.datamessie.core.base.util.sparsetable.SparseSingleTable;
import com.romeikat.datamessie.core.domain.dto.SourceOverviewDto;
import com.romeikat.datamessie.core.view.ui.page.SourcePage;

@AuthorizeInstantiation(DataMessieRoles.STATISTICS_PAGE)
public abstract class AbstractStatisticsPanel<Z extends Serializable & Comparable<? super Z>>
    extends AbstractTablePanel<Long, LocalDate, Z> {

  private static final long serialVersionUID = 1L;

  private static final int ROWS_PER_PAGE = 50;

  private static final String DATE_PATTERN = "d.M.";

  private IModel<Map<Long, SourceOverviewDto>> sourcesModel;

  private StatisticsPage statisticsPage;

  @SpringBean(name = "sourceDao")
  private SourceDao sourceDao;

  @SpringBean(name = "documentDao")
  private DocumentDao documentDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public AbstractStatisticsPanel(final String id, final StatisticsPage statisticsPage) {
    super(id);
    this.statisticsPage = statisticsPage;

    sourcesModel = new LoadableDetachableModel<Map<Long, SourceOverviewDto>>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected Map<Long, SourceOverviewDto> load() {
        final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);
        final DocumentsFilterSettings dfs = DataMessieSession.get().getDocumentsFilterSettings();
        final Long projectId = dfs.getProjectId();
        final Long sourceId = dfs.getSourceId();
        final Collection<Long> sourceTypeIds = dfs.getSourceTypeIds();
        final List<SourceOverviewDto> sources =
            sourceDao.getAsOverviewDtos(sessionProvider.getStatelessSession(), projectId, sourceId, sourceTypeIds);
        sessionProvider.closeStatelessSession();

        final Map<Long, SourceOverviewDto> sourcesMap = Maps.newHashMapWithExpectedSize(sources.size());
        for (final SourceOverviewDto source : sources) {
          sourcesMap.put(source.getId(), source);
        }
        return sourcesMap;
      }
    };
  }

  @Override
  protected ISingleTable<Long, LocalDate, Z> getTable() {
    return getStatistics();
  }

  @Override
  protected int getRowsPerPage() {
    return ROWS_PER_PAGE;
  }

  @Override
  protected String getSingularObjectName() {
    return "source";
  }

  @Override
  protected String getPluralObjectName() {
    return "sources";
  }

  @Override
  protected IModel<String> getFirstColumnHeaderModel() {
    return Model.of("Source");
  }

  @Override
  protected IModel<String> getColumnHeaderModel(final LocalDate publishedDate) {
    return Model.of(publishedDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
  }

  @Override
  protected Component getRowHeaderComponent(final String componentId, final Long sourceId) {
    final SourceOverviewDto source = sourcesModel.getObject().get(sourceId);
    if (source == null) {
      return null;
    }

    final PageParameters sourcePageParameters = statisticsPage.createProjectPageParameters();
    sourcePageParameters.set("id", source.getId());
    final IModel<String> labelModel = new PropertyModel<String>(source, "name");
    final BookmarkablePageLinkPanel<SourcePage> sourceLinkPanel =
        new BookmarkablePageLinkPanel<SourcePage>(componentId, SourcePage.class, sourcePageParameters, labelModel);
    return sourceLinkPanel;
  }

  @Override
  protected Component getValueComponent(final String componentId, final Long sourceId, final LocalDate date,
      final IModel<String> valueModel) {
    final LabelPanel statisticsPanel = new LabelPanel(componentId, valueModel);
    return statisticsPanel;
  }

  @Override
  protected Comparator<Long> getRowHeaderComparator() {
    final Map<Long, String> sourcesIdsNames = getSourcesIdsNames();
    // Sorts by source name, then source ID
    final Comparator<Long> keyComparator = new AscendingComparator<Long>();
    final Comparator<String> valueComparator = new AscendingComparator<String>();
    return new MapValueKeyComparator<Long, String>(sourcesIdsNames, keyComparator, valueComparator);
  }

  private Map<Long, String> getSourcesIdsNames() {
    final Function<SourceOverviewDto, String> sourceToNameFunction = new Function<SourceOverviewDto, String>() {
      @Override
      public String apply(final SourceOverviewDto source) {
        return source.getName();
      }
    };
    final Map<Long, String> sourcesIdsNames = Maps.transformValues(sourcesModel.getObject(), sourceToNameFunction);
    return sourcesIdsNames;
  }

  @Override
  protected Comparator<LocalDate> getColumnHeaderComparator() {
    return new DescendingComparator<LocalDate>();
  }

  private ISingleTable<Long, LocalDate, Z> getStatistics() {
    final Set<Long> sourceIds = sourcesModel.getObject().keySet();
    final ISingleTable<Long, LocalDate, Z> statistics = getStatistics(sourceIds);
    return statistics;
  }

  protected abstract SparseSingleTable<Long, LocalDate, Z> getStatistics(Collection<Long> sourceIds);

  @Override
  protected void onDetach() {
    super.onDetach();

    sourcesModel.detach();
  }

}
