package com.romeikat.datamessie.core;

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

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import com.ninja_squad.dbsetup.operation.Insert;
import com.ninja_squad.dbsetup.operation.Operation;
import com.romeikat.datamessie.core.domain.entity.impl.BarEntity;
import com.romeikat.datamessie.core.domain.entity.impl.BarEntityWithId;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Crawling;
import com.romeikat.datamessie.core.domain.entity.impl.DeletingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntity;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithGeneratedId;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithId;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithId2;
import com.romeikat.datamessie.core.domain.entity.impl.FooEntityWithoutIdAndVersion;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntity;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.Project;
import com.romeikat.datamessie.core.domain.entity.impl.Project2Source;
import com.romeikat.datamessie.core.domain.entity.impl.Project2User;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.RedirectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.Source;
import com.romeikat.datamessie.core.domain.entity.impl.Source2SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.SourceType;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import com.romeikat.datamessie.core.domain.entity.impl.TagSelectingRule;
import com.romeikat.datamessie.core.domain.entity.impl.User;

public class CommonOperations {

  public static final Operation DELETE_ALL_FOR_DATAMESSIE = deleteAllFrom(FooEntity.TABLE_NAME,
      FooEntityWithId.TABLE_NAME, FooEntityWithGeneratedId.TABLE_NAME, FooEntityWithId2.TABLE_NAME,
      FooEntityWithGeneratedIdAndVersion.TABLE_NAME, FooEntityWithoutIdAndVersion.TABLE_NAME,
      BarEntity.TABLE_NAME, BarEntityWithId.TABLE_NAME, NamedEntityOccurrence.TABLE_NAME,
      NamedEntityCategory.TABLE_NAME, NamedEntity.TABLE_NAME, RawContent.TABLE_NAME,
      CleanedContent.TABLE_NAME, StemmedContent.TABLE_NAME, Download.TABLE_NAME,
      Document.TABLE_NAME, Crawling.TABLE_NAME, RedirectingRule.TABLE_NAME, DeletingRule.TABLE_NAME,
      TagSelectingRule.TABLE_NAME, Source2SourceType.TABLE_NAME, SourceType.TABLE_NAME,
      Project2Source.TABLE_NAME, Source.TABLE_NAME, Project2User.TABLE_NAME, Project.TABLE_NAME,
      User.TABLE_NAME);

  public static Insert insertIntoBarEntity(final BarEntity barEntity) {
    return insertInto(BarEntity.TABLE_NAME).columns("name", "active", "foo_id")
        .values(barEntity.getName(), barEntity.getActive(), barEntity.getFooId()).build();
  }

  public static Insert insertIntoBarEntityWithId(final BarEntityWithId barEntityWithId) {
    return insertInto(BarEntityWithId.TABLE_NAME)
        .columns("id", "name", "active", "foo_id").values(barEntityWithId.getId(),
            barEntityWithId.getName(), barEntityWithId.getActive(), barEntityWithId.getFooId())
        .build();
  }

  public static Insert insertIntoUser(final User user) {
    return insertInto(User.TABLE_NAME)
        .columns("id", "version", "username", "passwordSalt", "passwordHash").values(user.getId(),
            user.getVersion(), user.getUsername(), user.getPasswordSalt(), user.getPasswordHash())
        .build();
  }

  public static Insert insertIntoSourceType(final SourceType sourceType) {
    return insertInto(SourceType.TABLE_NAME).columns("id", "name")
        .values(sourceType.getId(), sourceType.getName()).build();
  }

  public static Insert insertIntoSource(final Source source) {
    return insertInto(Source.TABLE_NAME)
        .columns("id", "version", "name", "language", "url", "userAgent", "cookie",
            "crawlingEnabled", "visible", "statisticsChecking", "notes")
        .values(source.getId(), source.getVersion(), source.getName(), source.getLanguage(),
            source.getUrl(), source.getUserAgent(), source.getCookie(), source.getCrawlingEnabled(),
            source.getVisible(), source.getStatisticsChecking(), source.getNotes())
        .build();
  }

  public static Insert insertIntoSource2SourceType(final Source2SourceType source2SourceType) {
    return insertInto(Source2SourceType.TABLE_NAME).columns("source_id", "sourceType_id")
        .values(source2SourceType.getSourceId(), source2SourceType.getSourceTypeId()).build();
  }

  public static Insert insertIntoProject(final Project project) {
    return insertInto(Project.TABLE_NAME)
        .columns("id", "version", "name", "crawlingEnabled", "crawlingInterval",
            "preprocessingEnabled")
        .values(project.getId(), project.getVersion(), project.getName(),
            project.getCrawlingEnabled(), project.getCrawlingInterval(),
            project.getPreprocessingEnabled())
        .build();
  }

  public static Insert insertIntoProject2User(final Project2User project2User) {
    return insertInto(Project2User.TABLE_NAME).columns("project_id", "user_id")
        .values(project2User.getProjectId(), project2User.getUserId()).build();
  }

  public static Insert insertIntoProject2Source(final Project2Source project2Source) {
    return insertInto(Project2Source.TABLE_NAME).columns("project_id", "source_id")
        .values(project2Source.getProjectId(), project2Source.getSourceId()).build();
  }

  public static Insert insertIntoCrawling(final Crawling crawling) {
    return insertInto(Crawling.TABLE_NAME)
        .columns("id", "version", "started", "completed", "project_id")
        .values(crawling.getId(), crawling.getVersion(), crawling.getStarted(),
            crawling.getCompleted(), crawling.getProjectId())
        .build();
  }

  public static Insert insertIntoDocument(final Document document) {
    return insertInto(Document.TABLE_NAME)
        .columns("id", "version", "title", "stemmedTitle", "url", "description",
            "stemmedDescription", "published", "downloaded", "state", "statusCode", "crawling_id",
            "source_id")
        .values(document.getId(), document.getVersion(), document.getTitle(),
            document.getStemmedTitle(), document.getUrl(), document.getDescription(),
            document.getStemmedDescription(), document.getPublished(), document.getDownloaded(),
            document.getState(), document.getStatusCode(), document.getCrawlingId(),
            document.getSourceId())
        .build();
  }

  public static Insert insertIntoDownload(final Download download) {
    return insertInto(Download.TABLE_NAME)
        .columns("id", "version", "url", "source_id", "document_id", "success")
        .values(download.getId(), download.getVersion(), download.getUrl(), download.getSourceId(),
            download.getDocumentId(), download.getSuccess())
        .build();
  }

  public static Insert insertIntoRawContent(final RawContent rawContent) {
    return insertInto(RawContent.TABLE_NAME).columns("document_id", "version", "rawContent")
        .values(rawContent.getDocumentId(), rawContent.getVersion(), rawContent.getContent())
        .build();
  }

  public static Insert insertIntoCleanedContent(final CleanedContent cleanedContent) {
    return insertInto(CleanedContent.TABLE_NAME).columns("document_id", "version", "content")
        .values(cleanedContent.getDocumentId(), cleanedContent.getVersion(),
            cleanedContent.getContent())
        .build();
  }

  public static Insert insertIntoStemmedContent(final StemmedContent stemmedContent) {
    return insertInto(StemmedContent.TABLE_NAME).columns("document_id", "version", "content")
        .values(stemmedContent.getDocumentId(), stemmedContent.getVersion(),
            stemmedContent.getContent())
        .build();
  }

  public static Insert insertIntoNamedEntity(final NamedEntity namedEntity) {
    return insertInto(NamedEntity.TABLE_NAME).columns("id", "version", "name")
        .values(namedEntity.getId(), namedEntity.getVersion(), namedEntity.getName()).build();
  }

  public static Insert insertIntoNamedEntityOccurrence(
      final NamedEntityOccurrence namedEntityOccurrence) {
    return insertInto(NamedEntityOccurrence.TABLE_NAME)
        .columns("id", "version", "namedEntity_id", "parentNamedEntity_id", "type", "quantity",
            "document_id")
        .values(namedEntityOccurrence.getId(), namedEntityOccurrence.getVersion(),
            namedEntityOccurrence.getNamedEntityId(),
            namedEntityOccurrence.getParentNamedEntityId(), namedEntityOccurrence.getType(),
            namedEntityOccurrence.getQuantity(), namedEntityOccurrence.getDocumentId())
        .build();
  }

  public static Insert insertIntoTagRedirectingRule(final RedirectingRule redirectingRule) {
    return insertInto(RedirectingRule.TABLE_NAME)
        .columns("id", "version", "regex", "regexGroup", "activeFrom", "activeTo", "mode",
            "position", "source_id")
        .values(redirectingRule.getId(), redirectingRule.getVersion(), redirectingRule.getRegex(),
            redirectingRule.getRegexGroup(), redirectingRule.getActiveFrom(),
            redirectingRule.getActiveTo(), redirectingRule.getMode(), redirectingRule.getPosition(),
            redirectingRule.getSourceId())
        .build();
  }

  public static Insert insertIntoDeletingRule(final DeletingRule deletingRule) {
    return insertInto(DeletingRule.TABLE_NAME)
        .columns("id", "version", "selector", "activeFrom", "activeTo", "mode", "position",
            "source_id")
        .values(deletingRule.getId(), deletingRule.getVersion(), deletingRule.getSelector(),
            deletingRule.getActiveFrom(), deletingRule.getActiveTo(), deletingRule.getMode(),
            deletingRule.getPosition(), deletingRule.getSourceId())
        .build();
  }

  public static Insert insertIntoTagSelectingRule(final TagSelectingRule tagSelectingRule) {
    return insertInto(TagSelectingRule.TABLE_NAME)
        .columns("id", "version", "selector", "activeFrom", "activeTo", "mode", "position",
            "source_id")
        .values(tagSelectingRule.getId(), tagSelectingRule.getVersion(),
            tagSelectingRule.getSelector(), tagSelectingRule.getActiveFrom(),
            tagSelectingRule.getActiveTo(), tagSelectingRule.getMode(),
            tagSelectingRule.getPosition(), tagSelectingRule.getSourceId())
        .build();
  }

}
