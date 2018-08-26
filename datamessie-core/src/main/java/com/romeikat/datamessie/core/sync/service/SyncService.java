package com.romeikat.datamessie.core.sync.service;

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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.ISynchronizersProvider;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.CleanedContentSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.CrawlingSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.DocumentSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.DownloadSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.NamedEntityCategorySynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.NamedEntityOccurrenceSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.NamedEntitySynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.ProjectSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.RawContentSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.RedirectingRuleSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.SourceSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.SourceTypeSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.StatisticsSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.StemmedContentSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.TagSelectingRuleSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.UserSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Project2SourceSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Project2UserSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Source2SourceTypeSynchronizer;
import com.romeikat.datamessie.core.sync.service.template.ISynchronizer;
import jersey.repackaged.com.google.common.collect.Lists;

@Service
public class SyncService {

  public static final Integer MAX_RESULTS = null;

  @Autowired
  private ApplicationContext ctx;

  private SyncService() {}

  public void doSynchronization(final TaskExecution taskExecution) throws TaskCancelledException {
    // Synchronizers from core
    final List<ISynchronizer> synchronizersFromCore = getSynchronizersFromCore();
    for (final ISynchronizer synchronizer : synchronizersFromCore) {
      synchronizer.synchronize(taskExecution);
    }

    // Synchronizers from plugins
    final List<ISynchronizer> synchronizersFromPlugins = getSynchronizersFromPlugins();
    for (final ISynchronizer synchronizer : synchronizersFromPlugins) {
      synchronizer.synchronize(taskExecution);
    }
  }

  private List<ISynchronizer> getSynchronizersFromCore() {
    final List<ISynchronizer> synchronizers = Lists.newLinkedList();

    synchronizers.add(new UserSynchronizer(ctx));
    synchronizers.add(new SourceTypeSynchronizer(ctx));
    synchronizers.add(new SourceSynchronizer(ctx));
    synchronizers.add(new Source2SourceTypeSynchronizer(ctx));
    synchronizers.add(new TagSelectingRuleSynchronizer(ctx));
    synchronizers.add(new RedirectingRuleSynchronizer(ctx));

    synchronizers.add(new ProjectSynchronizer(ctx));
    synchronizers.add(new Project2UserSynchronizer(ctx));
    synchronizers.add(new Project2SourceSynchronizer(ctx));

    synchronizers.add(new CrawlingSynchronizer(ctx));
    synchronizers.add(new DocumentSynchronizer(ctx));
    synchronizers.add(new DownloadSynchronizer(ctx));
    synchronizers.add(new RawContentSynchronizer(ctx));

    synchronizers.add(new CleanedContentSynchronizer(ctx));
    synchronizers.add(new StemmedContentSynchronizer(ctx));
    synchronizers.add(new NamedEntitySynchronizer(ctx));
    synchronizers.add(new NamedEntityCategorySynchronizer(ctx));
    synchronizers.add(new NamedEntityOccurrenceSynchronizer(ctx));
    synchronizers.add(new StatisticsSynchronizer(ctx));

    return synchronizers;
  }

  private List<ISynchronizer> getSynchronizersFromPlugins() {
    final List<ISynchronizer> synchronizers = Lists.newLinkedList();

    final Collection<ISynchronizersProvider> plugins =
        DateMessiePlugins.getInstance(ctx).getOrLoadPlugins(ISynchronizersProvider.class);
    for (final ISynchronizersProvider plugin : plugins) {
      final List<ISynchronizer> pluginSynchronizers = plugin.provideSynchronizers();
      synchronizers.addAll(pluginSynchronizers);
    }

    return synchronizers;
  }

}
