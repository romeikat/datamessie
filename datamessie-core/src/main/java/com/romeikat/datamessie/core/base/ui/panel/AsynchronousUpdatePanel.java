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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romeikat.datamessie.core.base.ui.behavior.FutureUpdateBehavior;

public abstract class AsynchronousUpdatePanel<T> extends Panel {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(AsynchronousUpdatePanel.class);

  private static final ResourceReference LOADING_RESOURCE_BAR =
      new PackageResourceReference(AsynchronousUpdatePanel.class, "ajax-loader-bar.gif");
  // private static final ResourceReference LOADING_RESOURCE_CIRCLE = new
  // PackageResourceReference(AsynchronousUpdatePanel.class, "ajax-loader-circle.gif");

  protected static final String STATUS_COMPONENT_ID = "status";
  protected static final int DEFAULT_TIMER_DURATION_SECS = 1;

  protected static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

  private transient Future<T> future;

  public AsynchronousUpdatePanel(final String id) {
    this(id, Duration.seconds(DEFAULT_TIMER_DURATION_SECS));
  }

  /**
   * Creates the panel.
   *
   * @param id
   * @param callableParameterModel
   * @param updateInterval
   */
  public AsynchronousUpdatePanel(final String id, final Duration updateInterval) {
    super(id, Model.of());

    final Callable<T> callable = createCallable();
    future = EXECUTOR.submit(callable);

    // Behaviour
    final FutureUpdateBehavior<T> behaviour = createBehavior(updateInterval);
    add(behaviour);

    // Loading...
    final Label loadingLabel = createLoadingLabel(STATUS_COMPONENT_ID);
    add(loadingLabel);
  }

  /**
   * Creates a callable that encapsulates the actual loading of the data.
   *
   * @param callableParameterModel Model providing access to parameters needed by the callable
   * @return A callable instance that encapsulates the logic needed to obtain the panel data
   */
  protected abstract Callable<T> createCallable();

  private FutureUpdateBehavior<T> createBehavior(final Duration updateInterval) {
    final FutureUpdateBehavior<T> behaviour = new FutureUpdateBehavior<T>(updateInterval, future) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onSuccess(final AjaxRequestTarget target) {
        final EmptyPanel emptyPanel = new EmptyPanel(STATUS_COMPONENT_ID);
        AsynchronousUpdatePanel.this.replace(emptyPanel);

        target.add(AsynchronousUpdatePanel.this);
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Exception e) {
        final String msg = "Error occurred while fetching data";
        LOG.error("Error during asynchronous loading", e);

        final Label label = new Label(STATUS_COMPONENT_ID, msg + ": " + e.getMessage());
        AsynchronousUpdatePanel.this.replace(label);

        target.add(AsynchronousUpdatePanel.this);
      }
    };
    return behaviour;
  }

  // Alternative with an overlay:
  // http://javathoughts.capesugarbird.com/2008/03/ajax-button-with-overlay-div-and-wait.html
  private Label createLoadingLabel(final String id) {
    final CharSequence url = RequestCycle.get().urlFor(LOADING_RESOURCE_BAR, null);
    final String loading = "<img style='vertical-align:middle;' alt=\"Loading...\" src=\"" + url + "\"/>";
    final Label label = new Label(id, loading);
    label.setEscapeModelStrings(false);
    return label;
  }

  @SuppressWarnings("unchecked")
  public T getModelObject() {
    return (T) getDefaultModelObject();
  }

  @SuppressWarnings("unchecked")
  public IModel<T> getModel() {
    return (IModel<T>) getDefaultModel();
  }

}
