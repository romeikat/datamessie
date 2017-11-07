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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FutureUpdateBehavior<T> extends AbstractAjaxTimerBehavior {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(FutureUpdateBehavior.class);

  private transient Future<T> future;

  public FutureUpdateBehavior(final Duration updateInterval, final Future<T> future) {
    super(updateInterval);

    this.future = future;
  }

  protected abstract void onSuccess(AjaxRequestTarget target);

  protected abstract void onError(AjaxRequestTarget target, Exception e);

  @Override
  protected void onTimer(final AjaxRequestTarget target) {
    if (future.isDone()) {
      return;
    }

    try {
      final T data = future.get();
      getComponent().setDefaultModelObject(data);
      stop(target);
      onSuccess(target);
    } catch (final InterruptedException e) {
      stop(target);
      final String msg = "Error occurred while fetching data: " + e.getMessage();
      LOG.error(msg, e);
      onError(target, e);
    } catch (final ExecutionException e) {
      stop(target);
      final String msg = "Error occurred while fetching data: " + e.getMessage();
      LOG.error(msg, e);
      onError(target, e);
    }
  }

}
