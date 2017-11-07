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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public class ExcelSheet {

  private static final int LAST_ROW_INDEX = SpreadsheetVersion.EXCEL2007.getLastRowIndex();

  private static final int LAST_COLUMN_INDEX = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();

  private static final String SPACE_ERROR = "Last row/column reached";

  private final SXSSFSheet sheet;

  private final CellStyle dateFormatCellStyle;

  private final CellStyle doubleFormatCellStyle;

  private int currentRowIndex;

  private int columnCursorIndex;

  private Row currentRow;

  private Cell currentCell;

  public ExcelSheet(final String sheetname) {
    this(new SXSSFWorkbook(), sheetname);
  }

  public ExcelSheet(final SXSSFWorkbook workbook, String sheetname) {
    // Sheet
    sheetname = normalizeSheetname(sheetname);
    sheet = workbook.createSheet(sheetname);
    // Date format
    final short dateFormat = workbook.createDataFormat().getFormat("dd.MM.yyyy");
    dateFormatCellStyle = workbook.createCellStyle();
    dateFormatCellStyle.setDataFormat(dateFormat);
    // Double number format
    final short doubleFormat = workbook.createDataFormat().getFormat("0.00");
    doubleFormatCellStyle = workbook.createCellStyle();
    doubleFormatCellStyle.setDataFormat(doubleFormat);
    // Indices
    currentRowIndex = 0;
    columnCursorIndex = 0;
    // Create first row
    currentRow = sheet.createRow(currentRowIndex);
  }

  public ExcelSheet nextRow() {
    currentRowIndex++;
    columnCursorIndex = 0;
    // Create next row, if more rows possible
    if (currentRowIndex <= LAST_ROW_INDEX) {
      currentRow = sheet.createRow(currentRowIndex);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final String value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null) {
      currentCell.setCellValue(value);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final Boolean value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null) {
      currentCell.setCellValue(value);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final Integer value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null) {
      currentCell.setCellValue(value);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final Long value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null) {
      currentCell.setCellValue(value);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final Double value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null && !value.isNaN()) {
      currentCell.setCellValue(value);
      currentCell.setCellStyle(doubleFormatCellStyle);
    }
    // Done
    return this;
  }

  public ExcelSheet add(final LocalDate value) {
    return add(DateUtil.fromLocalDate(value));
  }

  public ExcelSheet add(final LocalDateTime value) {
    return add(DateUtil.fromLocalDateTime(value));
  }

  public ExcelSheet add(final Date value) {
    // Check space
    if (!withinAllowedSpace()) {
      writeInfoAboutEndOfSpace();
      return this;
    }
    // Create cell
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write into cell
    if (value != null) {
      currentCell.setCellValue(value);
      currentCell.setCellStyle(dateFormatCellStyle);
    }
    // Done
    return this;
  }

  private boolean withinAllowedSpace() {
    return currentRowIndex < LAST_ROW_INDEX && columnCursorIndex < LAST_COLUMN_INDEX;
  }

  private void writeInfoAboutEndOfSpace() {
    // Decrease column cursor to last column, if necessary
    if (columnCursorIndex > LAST_COLUMN_INDEX) {
      columnCursorIndex = LAST_COLUMN_INDEX;
    }
    // Assuming that current row is the last one
    currentCell = currentRow.createCell(columnCursorIndex++);
    // Write info
    currentCell.setCellValue(SPACE_ERROR);
  }

  public ExcelSheet setColumnNo(final int columnIndex) {
    columnCursorIndex = columnIndex;
    // Done
    return this;
  }

  private String normalizeSheetname(final String sheetname) {
    final String normalizedSheetname = WorkbookUtil.createSafeSheetName(sheetname, '-');
    return normalizedSheetname;
  }

}
