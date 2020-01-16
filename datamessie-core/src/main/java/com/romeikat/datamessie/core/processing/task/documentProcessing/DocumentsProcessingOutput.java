package com.romeikat.datamessie.core.processing.task.documentProcessing;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.romeikat.datamessie.core.domain.entity.impl.CleanedContent;
import com.romeikat.datamessie.core.domain.entity.impl.Document;
import com.romeikat.datamessie.core.domain.entity.impl.Download;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityCategory;
import com.romeikat.datamessie.core.domain.entity.impl.NamedEntityOccurrence;
import com.romeikat.datamessie.core.domain.entity.impl.RawContent;
import com.romeikat.datamessie.core.domain.entity.impl.StemmedContent;
import jersey.repackaged.com.google.common.collect.Maps;

public class DocumentsProcessingOutput {

  /**
   * {@link Document Documents} to be persisted. Only contains {@link Document Documents} that
   * already exist.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final ConcurrentMap<Long, Document> documents;

  /**
   * {@link Download Downloads} to be persisted. May contain {@link Download downloads} that already
   * exist or ones that are new.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final SetMultimap<Long, Download> downloads;

  /**
   * {@link RawContent RawContents} to be persisted. Only contains {@link RawContent RawContents}
   * that already exist.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final ConcurrentMap<Long, RawContent> rawContents;

  /**
   * {@link CleanedContent CleanedContents} to be persisted. May contain {@link CleanedContent
   * CleanedContents} that already exist or ones that are new.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final ConcurrentMap<Long, CleanedContent> cleanedContents;

  /**
   * {@link StemmedContent StemmedContents} to be persisted. May contain {@link StemmedContent
   * StemmedContents} that already exist or ones that are new.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final ConcurrentMap<Long, StemmedContent> stemmedContents;

  /**
   * {@link NamedEntityOccurrence NamedEntityOccurrences} to be persisted. Only contains
   * {@link NamedEntityOccurrence NamedEntityOccurrences} that are new. Other existing
   * {@link NamedEntityOccurrence NamedEntityOccurrences} for the respective documents are to be
   * deleted.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final ConcurrentMap<Long, List<NamedEntityOccurrence>> namedEntityOccurrences;

  /**
   * {@link NamedEntityCategory NamedEntityCategorys} to be persisted. Only contains
   * {@link NamedEntityCategory NamedEntityCategorys} that are new.<br>
   * The persistence provider will decide how to persist in the most efficient way.
   */
  private final Set<NamedEntityCategory> namedEntityCategories;


  public DocumentsProcessingOutput() {
    documents = Maps.newConcurrentMap();
    downloads = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    rawContents = Maps.newConcurrentMap();
    cleanedContents = Maps.newConcurrentMap();
    stemmedContents = Maps.newConcurrentMap();
    namedEntityOccurrences = Maps.newConcurrentMap();
    namedEntityCategories = ConcurrentHashMap.newKeySet();
  }

  public void putDocument(final Document document) {
    documents.put(document.getId(), document);
  }

  public void putDocuments(final Collection<Document> documents) {
    for (final Document document : documents) {
      putDocument(document);
    }
  }

  public Document getDocument(final long documentId) {
    return documents.get(documentId);
  }

  public Map<Long, Document> getDocuments() {
    return documents;
  }

  public void putDownload(final Download download) {
    downloads.put(download.getDocumentId(), download);
  }

  public Set<Download> getDownloads(final Collection<Document> documents) {
    final Set<Download> result = Sets.newHashSet();
    for (final Document docunent : documents) {
      final Collection<Download> downloadsOfDocument = downloads.get(docunent.getId());
      result.addAll(downloadsOfDocument);
    }
    return result;
  }

  public Multimap<Long, Download> getDownloads() {
    return downloads;
  }

  public void putRawContent(final RawContent rawContent) {
    rawContents.put(rawContent.getId(), rawContent);
  }

  public Set<RawContent> getRawContents(final Collection<Document> documents) {
    final Set<RawContent> result = Sets.newHashSet();
    for (final Document docunent : documents) {
      final RawContent rawContentOfDocument = rawContents.get(docunent.getId());
      if (rawContentOfDocument != null) {
        result.add(rawContentOfDocument);
      }
    }
    return result;

  }

  public Map<Long, RawContent> getRawContents() {
    return rawContents;
  }

  public void putCleanedContent(final CleanedContent cleanedContent) {
    cleanedContents.put(cleanedContent.getId(), cleanedContent);
  }

  public CleanedContent getCleanedContent(final long documentId) {
    return cleanedContents.get(documentId);
  }

  public Map<Long, CleanedContent> getCleanedContents() {
    return cleanedContents;
  }

  public void putStemmedContent(final StemmedContent stemmedContent) {
    stemmedContents.put(stemmedContent.getId(), stemmedContent);
  }

  public StemmedContent getStemmedContent(final long documentId) {
    return stemmedContents.get(documentId);
  }

  public Map<Long, StemmedContent> getStemmedContents() {
    return stemmedContents;
  }

  public void putNamedEntityOccurrences(final long documentId,
      final List<NamedEntityOccurrence> namedEntityOccurrences) {
    this.namedEntityOccurrences.put(documentId, namedEntityOccurrences);
  }

  public List<NamedEntityOccurrence> getNamedEntityOccurrences(final long documentId) {
    return namedEntityOccurrences.get(documentId);
  }

  public Map<Long, List<NamedEntityOccurrence>> getNamedEntityOccurrences() {
    return namedEntityOccurrences;
  }

  public void putNamedEntityCategories(
      final Collection<NamedEntityCategory> namedEntityCategories) {
    this.namedEntityCategories.addAll(namedEntityCategories);
  }

  public Set<NamedEntityCategory> getNamedEntityCategories() {
    return namedEntityCategories;
  }

}
