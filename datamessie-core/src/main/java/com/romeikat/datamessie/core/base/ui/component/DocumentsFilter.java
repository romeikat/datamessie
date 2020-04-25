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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import com.romeikat.datamessie.core.base.util.DocumentsFilterSettings;

/**
 * Represents document IDs to be filtered as string:
 * <ul>
 * <li>No filtering of documents is displayed as <code>null</code> string</li>
 * <li>An empty collection of documents IDs is displayed as <code>-1</code></li>
 * <li>A collection of document IDs is displayed as comma-separated list</li>
 * </ul>
 * Negative IDs are ignored.
 *
 * @author Dr. Raphael Romeikat
 */
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
        final String result = generateDocumentsString(documentsIds);
        return result;
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
    if (documentIds == null) {
      return null;
    } else if (documentIds.isEmpty()) {
      return "-1";
    }

    final Collection<Long> validDocumentIds =
        documentIds.stream().filter(dId -> dId >= 0).collect(Collectors.toList());

    final String result = StringUtils.join(validDocumentIds, " ");
    return result;
  }

  private static List<Long> extractDocumentIds(final String text) {
    if (StringUtils.isBlank(text)) {
      return null;
    }

    final List<Long> result = new LinkedList<Long>();
    final Pattern p = Pattern.compile("\\d+");
    final Matcher m = p.matcher(text);
    while (m.find()) {
      final String document = m.group();
      final long documentId = Long.parseLong(document);
      if (documentId >= 0) {
        result.add(documentId);
      }
    }

    return result;
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    selectedDocumentsModel.detach();
  }

}
