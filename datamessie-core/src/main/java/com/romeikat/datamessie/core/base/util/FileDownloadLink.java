package com.romeikat.datamessie.core.base.util;

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

import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;

public class FileDownloadLink extends DownloadLink {

  private static final long serialVersionUID = 1L;

  @SpringBean
  private FileUtil fileUtil;

  public FileDownloadLink(final String id, final IModel<File> fileModel) {
    super(id, fileModel);
    setCacheDuration(Duration.NONE);
    setDeleteAfterDownload(true);
  }

  @Override
  public void onClick() {
    final File file = getModelObject();
    fileUtil.sendAsResponse(file);
  }

}
