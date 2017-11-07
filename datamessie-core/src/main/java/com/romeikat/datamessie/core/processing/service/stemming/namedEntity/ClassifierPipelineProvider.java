package com.romeikat.datamessie.core.processing.service.stemming.namedEntity;

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

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.util.Waiter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

@Service
public class ClassifierPipelineProvider {

  private static final Logger LOG = LoggerFactory.getLogger(ClassifierPipelineProvider.class);

  @Value("${pos.model}")
  private String posModel;

  @Value("${ner.model}")
  private String nerModel;

  private StanfordCoreNLP pipeline = null;

  private Boolean pipelineInitializing = false;

  @Async
  public void initializePipeline() {
    // Perform initialization only once
    synchronized (pipelineInitializing) {
      if (pipelineInitializing) {
        LOG.debug("Skipping classifier pipeline initialization as already in progress");
        return;
      }

      LOG.info("Starting classifier pipeline initialization");
      pipelineInitializing = true;
      pipeline = null;
    }

    // Initialize
    StanfordCoreNLP.clearAnnotatorPool();
    final Properties props = new Properties();
    // Lexing
    props.put("untokenizable", "noneKeep");
    // Lemmatization (works only for English)
    // props.put("annotators", "tokenize, ssplit, pos, lemma");
    // Named entity recognition
    props.put("annotators", "tokenize, ssplit, ner");
    props.put("pos.model", posModel);
    props.put("ner.useSUTime", "false");
    props.put("ner.model", nerModel);
    props.put("ner.applyNumericClassifiers", "false");
    try {
      pipeline = new StanfordCoreNLP(props);
    } catch (final Exception e) {
      LOG.error("Could not initialize pipeline", e);
      return;
    }

    // Done
    LOG.info("Completed classifier pipeline initialization");
    pipelineInitializing = false;
  }

  public void waitUntilPiplineInitialized(final TaskExecution taskExecution) {
    final Waiter waiter = new Waiter() {
      @Override
      public boolean isConditionFulfilled() {
        return pipeline != null && !pipelineInitializing;
      }
    };
    waiter.setFeedbackMessage("Waiting until pipeline is initialized...");
    waiter.setTaskExecution(taskExecution);
    waiter.waitUntilConditionIsFulfilled();
  }

  public StanfordCoreNLP getPipeline() {
    waitUntilPiplineInitialized(null);
    return pipeline;
  }

}
