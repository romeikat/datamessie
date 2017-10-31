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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;

public class DocumentsFilter extends TextArea<String> {

  private static final long serialVersionUID = 1L;

  private final IModel<String> selectedDocumentsModel;

  public DocumentsFilter(final String id, final IModel<DocumentsFilterSettings> dfsModel) {
    super(id);

    // Selected documents model
    selectedDocumentsModel = new IModel<String>() {

      private static final long serialVersionUID = 1L;

      @Override
      public String getObject() {
        final DocumentsFilterSettings dfs = dfsModel.getObject();
        if (dfs == null) {
          return null;
        }
        final Collection<Long> documentsIds = dfs.getDocumentIds();
        final String documentsString = generateDocumentsString(documentsIds);
        return documentsString;
      }

      @Override
      public void setObject(final String object) {
        final Collection<Long> documentIds = extractDocumentIds(object);
        final DocumentsFilterSettings dfs = dfsModel.getObject();
        dfs.setDocumentIds(documentIds);
      }

      @Override
      public void detach() {}

    };
    setModel(selectedDocumentsModel);
  }

  private static String generateDocumentsString(final Collection<Long> documentIds) {
    if (documentIds == null || documentIds.isEmpty()) {
      return null;
    }
    final String documentsString = StringUtils.join(documentIds, " ");
    return documentsString;
  }

  private static List<Long> extractDocumentIds(final String text) {
    final List<Long> documentIds = new LinkedList<Long>();
    if (text != null) {
      final Pattern p = Pattern.compile("\\d+");
      final Matcher m = p.matcher(text);
      while (m.find()) {
        final String document = m.group();
        final long documentId = Long.parseLong(document);
        documentIds.add(documentId);
      }
    }
    return documentIds;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    selectedDocumentsModel.detach();
  }

}
