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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Attribute;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Service
public class ParseUtil {

  public List<String> parseTerms(final String text, final Analyzer analyzer,
      final boolean keepQuotes) {
    final List<String> terms = new LinkedList<String>();
    // Keep quotes
    if (keepQuotes) {
      final List<QuotedAwareToken> tokens = getQuotedAwareTokens(text);
      for (final QuotedAwareToken token : tokens) {
        // Quoted string
        if (token.isQuoted()) {
          terms.add(token.toString());
        }
        // Unquoted string
        else {
          final List<String> containedTokens = parseTerms(token.toString(), analyzer);
          terms.addAll(containedTokens);
        }
      }
    }
    // Don't keep quotes
    else {
      final List<String> containedTokens = parseTerms(text, analyzer);
      terms.addAll(containedTokens);
    }
    // Done
    return terms;
  }

  private List<QuotedAwareToken> getQuotedAwareTokens(final String text) {
    final List<QuotedAwareToken> tokens = new LinkedList<QuotedAwareToken>();
    QuotedAwareToken currentToken = new QuotedAwareToken();
    // Process each single character
    final Pattern pattern =
        Pattern.compile(".", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    final Matcher matcher = pattern.matcher(text);
    boolean withinQuote = false;
    while (matcher.find()) {
      final String character = matcher.group();
      if (character.equals("\"")) {
        // End quoted token
        if (withinQuote) {
          currentToken.append(character);
          tokens.add(currentToken);
          currentToken = new QuotedAwareToken();
          withinQuote = false;
        }
        // Start quoted token
        else {
          if (!currentToken.isEmpty()) {
            tokens.add(currentToken);
          }
          currentToken = new QuotedAwareToken();
          currentToken.append(character);
          withinQuote = true;
        }
      } else {
        // Continue token
        currentToken.append(character);
      }
    }
    // Last token
    if (!currentToken.isEmpty()) {
      tokens.add(currentToken);
    }
    // Done
    return tokens;
  }

  public List<String> parseTerms(final String text, final Analyzer analyzer) {
    final List<String> terms = new LinkedList<String>();
    try {
      final TokenStream tokenStream = analyzer.tokenStream(null, text);
      tokenStream.reset();
      final Attribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
      while (tokenStream.incrementToken()) {
        final String term = attribute.toString();
        terms.add(term);
      }
      tokenStream.end();
      tokenStream.close();
    } catch (final IOException e) {
      // Cannot be thrown due to usage of a StringReader
    }
    return terms;
  }

  public List<String> parseTerms(final String text) {
    return parseTerms(text, new TokenizerAnalyzer());
  }

  static class QuotedAwareToken {

    private final StringBuffer token;

    public QuotedAwareToken() {
      token = new StringBuffer();
    }

    public void append(final String s) {
      token.append(s);
    }

    public boolean isEmpty() {
      return token.length() == 0;
    }

    @Override
    public String toString() {
      return token.toString();
    }

    public boolean isQuoted() {
      if (token.length() < 2) {
        return false;
      }
      if (!token.substring(0, 1).equals("\"")) {
        return false;
      }
      if (!token.substring(token.length() - 1).equals("\"")) {
        return false;
      }
      return true;
    }

  }

  public boolean containsWordsInTheRightSequence(final String text,
      final Collection<String> word1Variants, final Collection<String> word2Variants,
      final Integer maxWordsInBetween, final Analyzer analyzer) {
    if (word1Variants == null || word2Variants == null) {
      return true;
    }

    final Set<String> parsedWord1Variants = Sets.newHashSet();
    for (final String word1Variant : word1Variants) {
      final List<String> additionalParsedWord1Variants = parseTerms(word1Variant, analyzer, false);
      parsedWord1Variants.addAll(additionalParsedWord1Variants);
    }
    if (parsedWord1Variants.isEmpty()) {
      return false;
    }

    final Set<String> parsedWord2Variants = Sets.newHashSet();
    for (final String word2Variant : word2Variants) {
      final List<String> additionalParsedWord2Variants = parseTerms(word2Variant, analyzer, false);
      parsedWord2Variants.addAll(additionalParsedWord2Variants);
    }
    if (parsedWord2Variants.isEmpty()) {
      return false;
    }

    final List<String> parsedWords = parseTerms(text, analyzer, false);

    final List<Integer> indexesOfWord1 = getIndexesOfWordVariants(parsedWord1Variants, parsedWords);
    final List<Integer> indexesOfWord2 = getIndexesOfWordVariants(parsedWord2Variants, parsedWords);

    final boolean matchFound = containsIndexesInTheRightSequenceAndDistance(indexesOfWord1,
        indexesOfWord2, maxWordsInBetween);
    return matchFound;
  }

  private boolean containsIndexesInTheRightSequenceAndDistance(final List<Integer> indexesOfWord1,
      final List<Integer> indexesOfWord2, final Integer maxWordsInBetween) {
    for (final int indexOfWord1 : indexesOfWord1) {
      for (final int indexOfWord2 : indexesOfWord2) {
        if (areIndexesInTheRightSequenceAndDistance(indexOfWord1, indexOfWord2,
            maxWordsInBetween)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean areIndexesInTheRightSequenceAndDistance(final int indexOfWord1,
      final int indexOfWord2, final Integer maxWordsInBetween) {

    final int indexesDiff = indexOfWord2 - indexOfWord1;
    final boolean areIndexesInTheRightSequence = indexesDiff > 0;
    if (!areIndexesInTheRightSequence) {
      return false;
    }

    final int actualWordsInBetween = indexesDiff - 1;
    final boolean areWithinDistance =
        maxWordsInBetween == null || actualWordsInBetween <= maxWordsInBetween;
    if (!areWithinDistance) {
      return false;
    }

    return true;
  }

  private List<Integer> getIndexesOfWordVariants(final Collection<String> wordVariants,
      final List<String> words) {
    final List<Integer> indexes = Lists.newLinkedList();

    int i = 0;
    final Iterator<String> wordsIterator = words.iterator();
    while (wordsIterator.hasNext()) {
      final String word = wordsIterator.next();

      for (final String wordVariant : wordVariants) {
        if (wordVariant.equalsIgnoreCase(word)) {
          indexes.add(i);
          break;
        }

      }

      i++;
    }

    return indexes;
  }

}
