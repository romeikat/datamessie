package com.romeikat.datamessie.core.processing.service.cleaning.boilerpipe;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ArticleSentencesExtractor;
import de.l3s.boilerpipe.extractors.CanolaExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.extractors.KeepEverythingExtractor;
import de.l3s.boilerpipe.extractors.KeepEverythingWithMinKWordsExtractor;
import de.l3s.boilerpipe.extractors.LargestContentExtractor;
import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;

@Service
public class BoilerplateRemover {

  @Value("${boilerpipe.extractor}")
  public String extractor;

  @Autowired
  private Environment env;

  public String removeBoilerplate(final String text) throws BoilerpipeProcessingException {
    if (text == null) {
      return null;
    }

    String cleanedText;
    synchronized (this) {
      final ExtractorBase extractorBase = getExtractorBase(extractor, null);
      cleanedText = extractorBase.getText(text);
    }
    return cleanedText;
  }

  private ExtractorBase getExtractorBase(final String extractor, String minK) {
    switch (extractor) {
      case "ArticleExtractor":
        return ArticleExtractor.INSTANCE;
      case "ArticleSentencesExtractor":
        return ArticleSentencesExtractor.INSTANCE;
      case "CanolaExtractor":
        return CanolaExtractor.INSTANCE;
      case "DefaultExtractor":
        return DefaultExtractor.INSTANCE;
      case "KeepEverythingExtractor":
        return KeepEverythingExtractor.INSTANCE;
      case "KeepEverythingWithMinKWordsExtractor":
        try {
          if (minK == null) {
            minK = env.getProperty("boilerpipe.extractor.minK");
          }
          final int minKAsInt = minK == null || minK.isEmpty() ? 0 : Integer.parseInt(minK);
          return new KeepEverythingWithMinKWordsExtractor(minKAsInt);
        } catch (final NumberFormatException e) {
          throw new RuntimeException("Unsupported minK: " + minK);
        }
      case "LargestContentExtractor":
        return LargestContentExtractor.INSTANCE;
      case "NumWordsRulesExtractor":
        return NumWordsRulesExtractor.INSTANCE;
      default:
        throw new RuntimeException("Unsupported extractor: " + extractor);
    }
  }

}
