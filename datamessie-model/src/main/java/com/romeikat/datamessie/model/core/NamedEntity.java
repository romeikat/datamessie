package com.romeikat.datamessie.model.core;

/*-
 * ============================LICENSE_START============================
 * data.messie (model)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
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

import java.util.ArrayList;
import java.util.List;
import com.romeikat.datamessie.model.EntityWithIdAndVersion;

public interface NamedEntity extends EntityWithIdAndVersion {

  String getName();

  NamedEntity setName(String name);

  static String getAsSingleWord(final String name) {
    return name.replaceAll("[ -]", "_");
  }

  static String getAsMultipleWords(final String singleName) {
    return singleName.replaceAll("[_-]", " ");
  }

  static List<String> getWordList(final String name) {
    final List<String> words = new ArrayList<String>();
    final String singleWord = getAsSingleWord(name);
    for (final String word : singleWord.split("_")) {
      words.add(word.toLowerCase());
    }
    return words;
  }

  static int getNumberOfWords(final String name) {
    final List<String> wordList = getWordList(name);
    final int numberOfWords = wordList.size();
    return numberOfWords;
  }

}
