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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.app.plugin.DateMessiePlugins;
import com.romeikat.datamessie.core.base.app.plugin.ISynchronizersProvider;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.CleanedContentSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.CrawlingSynchronizer;
import com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion.DeletingRuleSynchronizer;
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

  @Value("${sync.filter.sources}")
  private List<Long> sourceIdsForSync;

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

    // Original data

    final SourceSynchronizer sourceSynchronizer = new SourceSynchronizer(sourceIdsForSync, ctx);
    synchronizers.add(sourceSynchronizer);

    final Source2SourceTypeSynchronizer source2SourceTypeSynchronizer =
        new Source2SourceTypeSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(source2SourceTypeSynchronizer);

    final SourceTypeSynchronizer sourceTypeSynchronizer =
        new SourceTypeSynchronizer(source2SourceTypeSynchronizer, ctx);
    synchronizers.add(sourceTypeSynchronizer);

    final RedirectingRuleSynchronizer redirectingRuleSynchronizer =
        new RedirectingRuleSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(redirectingRuleSynchronizer);

    final DeletingRuleSynchronizer deletingRuleSynchronizer =
        new DeletingRuleSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(deletingRuleSynchronizer);

    final TagSelectingRuleSynchronizer tagSelectingRuleSynchronizer =
        new TagSelectingRuleSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(tagSelectingRuleSynchronizer);

    final Project2SourceSynchronizer project2SourceSynchronizer =
        new Project2SourceSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(project2SourceSynchronizer);

    final ProjectSynchronizer projectSynchronizer =
        new ProjectSynchronizer(project2SourceSynchronizer, ctx);
    synchronizers.add(projectSynchronizer);

    final Project2UserSynchronizer project2UserSynchronizer =
        new Project2UserSynchronizer(projectSynchronizer, ctx);
    synchronizers.add(project2UserSynchronizer);

    final UserSynchronizer userSynchronizer = new UserSynchronizer(project2UserSynchronizer, ctx);
    synchronizers.add(userSynchronizer);

    final DocumentSynchronizer documentSynchronizer =
        new DocumentSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(documentSynchronizer);

    final DownloadSynchronizer downloadSynchronizer =
        new DownloadSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(downloadSynchronizer);

    final RawContentSynchronizer rawContentSynchronizer =
        new RawContentSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(rawContentSynchronizer);

    final CrawlingSynchronizer crawlingSynchronizer =
        new CrawlingSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(crawlingSynchronizer);

    // Processed data

    final CleanedContentSynchronizer cleanedContentSynchronizer =
        new CleanedContentSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(cleanedContentSynchronizer);

    final StemmedContentSynchronizer stemmedContentSynchronizer =
        new StemmedContentSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(stemmedContentSynchronizer);

    final NamedEntityOccurrenceSynchronizer namedEntityOccurrenceSynchronizer =
        new NamedEntityOccurrenceSynchronizer(documentSynchronizer, ctx);
    synchronizers.add(namedEntityOccurrenceSynchronizer);

    final NamedEntityCategorySynchronizer namedEntityCategorySynchronizer =
        new NamedEntityCategorySynchronizer(namedEntityOccurrenceSynchronizer, ctx);
    synchronizers.add(namedEntityCategorySynchronizer);

    final NamedEntitySynchronizer namedEntitySynchronizer = new NamedEntitySynchronizer(
        namedEntityOccurrenceSynchronizer, namedEntityCategorySynchronizer, ctx);
    synchronizers.add(namedEntitySynchronizer);

    final StatisticsSynchronizer statisticsSynchronizer =
        new StatisticsSynchronizer(sourceSynchronizer, ctx);
    synchronizers.add(statisticsSynchronizer);

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
