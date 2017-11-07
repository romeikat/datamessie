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

import java.io.File;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

public abstract class AjaxDownloadBehavior extends AbstractAjaxBehavior {

  private static final long serialVersionUID = 1L;

  public void initiate(final AjaxRequestTarget target) {
    String url = getCallbackUrl().toString();

    // Anti-cache
    url = url + (url.contains("?") ? "&" : "?");
    url = url + "antiCache=" + System.currentTimeMillis();

    // Timeout is needed to let Wicket release the channel
    target.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
  }

  @Override
  public void onRequest() {
    final File file = getFile();
    final IResourceStream fileRsourceStream = new FileResourceStream(file);
    final IRequestHandler resourceStreamRequestHandler =
        new ResourceStreamRequestHandler(fileRsourceStream, file.getName());
    getComponent().getRequestCycle()
        .scheduleRequestHandlerAfterCurrent(resourceStreamRequestHandler);
  }

  protected abstract File getFile();

}
