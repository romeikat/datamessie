package com.romeikat.datamessie.core.base.ui.behavior;

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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.util.time.Duration;

public abstract class SwitchingFrequencyAjaxSelfUpdatingTimerBehavior
    extends AjaxSelfUpdatingTimerBehavior {

  private static final long serialVersionUID = 1L;

  private static final Duration refreshTimeFast = Duration.seconds(5);

  private static final Duration refreshTimeSlow = Duration.seconds(30);

  public SwitchingFrequencyAjaxSelfUpdatingTimerBehavior() {
    // Starting fast...
    super(refreshTimeFast);
  }

  @Override
  protected void onPostProcessTarget(final AjaxRequestTarget target) {
    final Duration updateInterval = determineUpdateInterval();
    // ...and after each update, continue slowly or fast
    setUpdateInterval(updateInterval);
  }

  private Duration determineUpdateInterval() {
    final Duration updateInterval = fastInterval() ? refreshTimeFast : refreshTimeSlow;
    return updateInterval;
  }

  abstract public boolean fastInterval();

}
