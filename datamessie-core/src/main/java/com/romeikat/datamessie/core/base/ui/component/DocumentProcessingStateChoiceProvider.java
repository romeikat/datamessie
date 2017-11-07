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
import java.util.Map;
import org.wicketstuff.select2.Response;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.romeikat.datamessie.core.domain.enums.DocumentProcessingState;

public class DocumentProcessingStateChoiceProvider
    extends AbstractChoiceProvider<DocumentProcessingState> {

  private static final long serialVersionUID = 1L;

  @Override
  public String getDisplayValue(final DocumentProcessingState choice) {
    return choice.getName();
  }

  @Override
  public String getIdValue(final DocumentProcessingState choice) {
    return String.valueOf(choice.ordinal());
  }

  @Override
  public void query(final String term, final int page,
      final Response<DocumentProcessingState> response) {
    for (final DocumentProcessingState state : getStates().values()) {
      final String existingTerm = state.getName().toLowerCase();
      final String searchTerm = term.toLowerCase();
      if (existingTerm.contains(searchTerm)) {
        response.add(state);
      }
    }
  }

  @Override
  public Collection<DocumentProcessingState> toChoices(final Collection<String> ids) {
    final Map<Integer, DocumentProcessingState> states = getStates();
    final Function<String, DocumentProcessingState> toChoicesFunction =
        new Function<String, DocumentProcessingState>() {

          @Override
          public DocumentProcessingState apply(final String id) {
            return states.get(Integer.parseInt(id));
          }

        };
    final Collection<DocumentProcessingState> choices =
        Collections2.transform(ids, toChoicesFunction);
    return choices;
  }

  private Map<Integer, DocumentProcessingState> getStates() {
    final DocumentProcessingState[] states = DocumentProcessingState.values();

    final Map<Integer, DocumentProcessingState> statesMap =
        Maps.newHashMapWithExpectedSize(states.length);
    for (final DocumentProcessingState state : states) {
      statesMap.put(state.ordinal(), state);
    }
    return statesMap;
  }

}
