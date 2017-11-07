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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.Response;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class AbstractIdBasedChoiceProvider<T> extends AbstractChoiceProvider<Long> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractIdBasedChoiceProvider.class);

  private static final long serialVersionUID = 1L;

  IModel<Map<Long, T>> choicesModel;

  public AbstractIdBasedChoiceProvider() {
    super();

    choicesModel = new LoadableDetachableModel<Map<Long, T>>() {
      private static final long serialVersionUID = 1L;

      @Override
      protected Map<Long, T> load() {
        final List<T> choices = provideChoices();
        final Map<Long, T> choicesMap = Maps.newHashMapWithExpectedSize(choices.size());
        for (final T choice : choices) {
          final long id = provideId(choice);
          choicesMap.put(id, choice);
        }
        return choicesMap;
      }
    };
  }

  @Override
  public String getDisplayValue(final Long choiceId) {
    final Map<Long, T> choices = getChoices();
    final T choice = choices.get(choiceId);
    if (choice == null) {
      return null;
    }
    return provideDisplayText(choice);
  }

  @Override
  public String getIdValue(final Long choice) {
    return String.valueOf(choice);
  }

  @Override
  public void query(final String term, final int page, final Response<Long> response) {
    // Determine choices
    final List<T> choices = Lists.newLinkedList();
    for (final T choice : getChoices().values()) {
      final String displayText = provideDisplayText(choice);
      if (displayText == null) {
        continue;
      }
      if (displayText.toLowerCase().contains(term.toLowerCase())) {
        choices.add(choice);
      }
    }

    // Sort choices
    Collections.sort(choices, getComparator());

    // Collect ids
    final Function<T, Long> choiceToIdFunction = new Function<T, Long>() {
      @Override
      public Long apply(final T choice) {
        return provideId(choice);
      }
    };
    final List<Long> choiceIds = Lists.transform(choices, choiceToIdFunction);

    // Done
    response.setResults(choiceIds);
  }

  @Override
  public Collection<Long> toChoices(final Collection<String> ids) {
    final Function<String, Long> toChoicesFunction = new Function<String, Long>() {

      @Override
      public Long apply(final String id) {
        try {
          return Long.parseLong(id);
        } catch (final NumberFormatException e) {
          LOG.warn("Cannot parse id {}", id);
          return null;
        }
      }

    };
    final Collection<Long> choices = Collections2.transform(ids, toChoicesFunction);
    // The returned collection must be serializable
    return Lists.newArrayList(choices);
  }

  protected Map<Long, T> getChoices() {
    return choicesModel.getObject();
  }

  protected abstract List<T> provideChoices();

  protected abstract long provideId(T choice);

  protected abstract String provideDisplayText(T choice);

  protected Comparator<T> getComparator() {
    // Sorting by display text
    final Comparator<T> displayTextComparator = new Comparator<T>() {
      @Override
      public int compare(final T choice1, final T choice2) {
        String displayText1 = provideDisplayText(choice1);
        String displayText2 = provideDisplayText(choice2);
        if (displayText1 == null) {
          displayText1 = "";
        }
        if (displayText2 == null) {
          displayText2 = "";
        }
        return displayText1.compareTo(displayText2);
      }
    };
    return displayTextComparator;
  }

  @Override
  public void detach() {
    super.detach();

    choicesModel.detach();
  }

}
