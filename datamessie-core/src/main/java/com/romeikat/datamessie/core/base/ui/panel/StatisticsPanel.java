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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.time.Duration;

public class StatisticsPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private static final int SELF_UPDATING_INTERVAL = 60;

  private static final Duration AJAX_TIMEOUT = Duration.seconds(5);

  public StatisticsPanel(final String id) {
    super(id);

    setOutputMarkupId(true);
    add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(SELF_UPDATING_INTERVAL)) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.setRequestTimeout(AJAX_TIMEOUT);
      }
    });

    // Today's history
    final AjaxLazyLoadPanel todaysStatisticsPanel = new AjaxLazyLoadPanel("todaysStatistics") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        final StatisticsPeriodPanel statisticsPeriodPanel = new StatisticsPeriodPanel(id, 1);
        return statisticsPeriodPanel;
      }

      @Override
      protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.setRequestTimeout(AJAX_TIMEOUT);
      }
    };
    add(todaysStatisticsPanel);
  }

}
