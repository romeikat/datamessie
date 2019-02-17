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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.domain.dto.FileDto;

@Service
public class FileUtil {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  @Value("${export.dir}")
  private String exportDir;

  public String readFile(final String file, final Charset charset) {
    try {
      final Path path = Paths.get(file);
      final byte[] bytes = Files.readAllBytes(path);
      final String contents = new String(bytes, charset);
      return contents;
    } catch (final Exception e) {
      LOG.error("Could not read file", e);
      return null;
    }
  }

  public synchronized String createTargetDir() {
    try {
      final String dirName =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
      Path dir = Paths.get(exportDir, dirName);
      dir = getNonExisting(dir);
      Files.createDirectories(dir);
      return dir.toString();
    } catch (final Exception e) {
      LOG.error("Could not create target directory", e);
      return null;
    }
  }

  public String createSubDir(final String parentDir, final String dir) {
    try {
      final Path subDir = Paths.get(parentDir, dir);
      Files.createDirectories(subDir);
      return subDir.toString();
    } catch (final Exception e) {
      LOG.error("Could not create subdirectory", e);
      return null;
    }
  }

  public synchronized File createEmptyTxtFile(final String filename) {
    return createEmptyTxtFile(exportDir, filename);
  }

  public synchronized File createEmptyTxtFile(final String dir, final String filename) {
    return createTxtFile(dir, filename, null);
  }

  public synchronized File createTxtFile(final String filename, final String content) {
    return createTxtFile(exportDir, filename, content);
  }

  public synchronized File createTxtFile(final String dir, String filename, final String content) {
    try {
      filename = normalizeFilename(filename);
      Path file = Paths.get(dir, filename + ".txt");
      file = getNonExisting(file);
      Files.createDirectories(file.getParent());
      Files.createFile(file);
      if (content != null && !content.isEmpty()) {
        final FileOutputStream fileOutputStream = new FileOutputStream(file.toFile(), false);
        final BufferedWriter bufferedWriter =
            new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        bufferedWriter.write(content);
        bufferedWriter.close();
        fileOutputStream.close();
      }
      return file.toFile();
    } catch (final Exception e) {
      LOG.error("Could not create text file", e);
      return null;
    }
  }

  public synchronized File createXlsxFile(final String filename, final SXSSFWorkbook xlsxWorkbook) {
    return createXlsxFile(exportDir, filename, xlsxWorkbook);
  }

  public synchronized File createXlsxFile(final String dir, String filename,
      final SXSSFWorkbook xlsxWorkbook) {
    try {
      filename = normalizeFilename(filename);
      Path file = Paths.get(dir, filename + ".xlsx");
      file = getNonExisting(file);
      Files.createDirectories(file.getParent());
      Files.createFile(file);
      final FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
      xlsxWorkbook.write(fileOutputStream);
      fileOutputStream.close();
      return file.toFile();
    } catch (final Exception e) {
      LOG.error("Could not create Excel file", e);
      return null;
    }
  }

  public synchronized Path createXlsxFile(final Path dir, final String filename,
      final SXSSFWorkbook xlsxWorkbook) {
    return Paths.get(createXlsxFile(dir.toString(), filename, xlsxWorkbook).getAbsolutePath());
  }

  public synchronized File createTxtFileAndZip(final String prefixFilename, final FileDto fileDto) {
    final List<FileDto> fileDtos = new ArrayList<FileDto>();
    fileDtos.add(fileDto);
    return createTxtFilesAndZip(prefixFilename, fileDtos);
  }

  public synchronized File createTxtFilesAndZip(final String filename,
      final Collection<? extends FileDto> fileDtos) {
    return createTxtFilesAndZip(filename, fileDtos, null);
  }

  public synchronized File createTxtFilesAndZip(String filename,
      final Collection<? extends FileDto> fileDtos, final SXSSFWorkbook summaryWorkbook) {
    try {
      filename = normalizeFilename(filename);
      // Create temporary directory
      final Path tempDir = Files.createTempDirectory(filename);
      // Create temporary files
      final List<Path> tempFiles = createTempFiles(tempDir, fileDtos);
      if (summaryWorkbook != null) {
        final Path tempSummary = createXlsxFile(tempDir, "Summary", summaryWorkbook);
        tempFiles.add(tempSummary);
      }
      // Create ZIP file
      final Path zipFile = createZipFile(exportDir, filename, tempFiles);
      // Delete temporary files
      for (final Path tempFile : tempFiles) {
        Files.delete(tempFile);
      }
      // Delete temporary directory
      Files.delete(tempDir);
      // Done
      return zipFile.toFile();
    } catch (final Exception e) {
      LOG.error("Could not create ZIP file", e);
      return null;
    }
  }

  private List<Path> createTempFiles(final Path tempDir,
      final Collection<? extends FileDto> fileDtos) throws IOException {
    // For each DTO...
    final List<Path> tempFiles = new ArrayList<Path>();
    for (final FileDto fileDto : fileDtos) {
      // ...determine target file
      final String tempFileDir = tempDir.toString();
      final String tempFileName = fileDto.getFilename();
      final Path tempFile = Paths.get(tempFileDir, tempFileName);
      // ...create file and write content to file
      final String content = fileDto.getContent();
      if (StringUtils.isNotEmpty(content)) {
        final String stemmedContentWithLineSeparator = content + LINE_SEPARATOR;
        Files.write(tempFile, stemmedContentWithLineSeparator.getBytes(StandardCharsets.UTF_8));
        // ...remember file
        tempFiles.add(tempFile);
      }
    }
    // Done
    return tempFiles;
  }

  private Path createZipFile(final String dir, String filename, final List<Path> files)
      throws IOException {
    filename = normalizeFilename(filename);
    // Create ZIP file
    Path zipFile = Paths.get(dir, filename + ".zip");
    zipFile = getNonExisting(zipFile);
    Files.createDirectories(zipFile.getParent());
    Files.createFile(zipFile);
    final URI zipUri = URI.create("jar:file:" + zipFile.toUri().getPath());
    Files.delete(zipFile);
    final Map<String, String> zipProperties = new HashMap<String, String>();
    zipProperties.put("create", "true");
    zipProperties.put("encoding", "UTF-8");
    final FileSystem zipFS = FileSystems.newFileSystem(zipUri, zipProperties);
    // Copy TXT files to ZIP file
    for (final Path file : files) {
      final Path fileToZip = file;
      final Path pathInZipfile = zipFS.getPath(file.getFileName().toString());
      Files.copy(fileToZip, pathInZipfile);
    }
    // Done
    zipFS.close();
    return zipFile;
  }

  public synchronized void appendToFile(final File file, final String content) {
    final Path path = FileSystems.getDefault().getPath(file.getAbsolutePath());
    final Charset charset = Charset.defaultCharset();
    try (
        BufferedWriter writer = Files.newBufferedWriter(path, charset, StandardOpenOption.APPEND)) {
      writer.write(String.format("%s%n", content));
    } catch (final Exception e) {
      LOG.error("Could not apennd to file " + file.getAbsolutePath(), e);
    }
  }

  public synchronized void delete(final File file) {
    if (!file.exists()) {
      return;
    }
    // Delete directory
    if (file.isDirectory()) {
      // Delete all the directory contents
      final String[] containedFileNames = file.list();
      for (final String containedFileName : containedFileNames) {
        final File containedFile = new File(file, containedFileName);
        delete(containedFile);
      }
      // Directory is no empty, so delete it
      file.delete();
    }
    // Delete file
    else {
      file.delete();
    }
  }

  public synchronized void sendAsResponse(final File file) {
    if (file == null) {
      LOG.warn("No file provided");
      return;
    }

    final IResourceStream fileRsourceStream = new FileResourceStream(file);
    final IRequestHandler resourceStreamRequestHandler =
        new ResourceStreamRequestHandler(fileRsourceStream, file.getName());
    RequestCycle.get().scheduleRequestHandlerAfterCurrent(resourceStreamRequestHandler);
  }

  private static Path getNonExisting(Path path) {
    // Return path, if it does not exist
    if (!Files.exists(path)) {
      return path;
    }
    // Determine name and extension
    final String name = path.getFileName().toString();
    final int fileTypeIndex = name.lastIndexOf(".");
    String nameWithoutExtension;
    String extension;
    if (fileTypeIndex == -1) {
      nameWithoutExtension = name;
      extension = "";
    } else {
      nameWithoutExtension = name.substring(0, fileTypeIndex);
      extension = name.substring(fileTypeIndex);
    }
    // Determine number
    final int underscoreIndex = name.lastIndexOf("_");
    String nameWithoutExtensionWithoutNumber;
    Integer nextNumber = 1;
    if (underscoreIndex == -1) {
      nameWithoutExtensionWithoutNumber = nameWithoutExtension;
    } else {
      nameWithoutExtensionWithoutNumber = nameWithoutExtension.substring(0, underscoreIndex);
      final String numberString = name.substring(underscoreIndex + 1);
      try {
        nextNumber = Integer.parseInt(numberString) + 1;
      } catch (final NumberFormatException e) {
      }
    }
    // Determine next candidate
    String nextName;
    while (true) {
      nextName = nameWithoutExtensionWithoutNumber + "_" + nextNumber++ + extension;
      path = Paths.get(path.getParent().toString(), nextName);
      if (!Files.exists(path)) {
        return path;
      }
    }
  }

  private String normalizeFilename(final String filename) {
    final String normalizedFilename = filename.replaceAll("[ /\\\\]", "-");
    return normalizedFilename;
  }

}
