package com.example.sapbo.core;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;

public class DebugResultExcelExporter {
    public void export(DebugResult debugResult, String debugTitle, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            writeSummary(workbook, debugResult, debugTitle, headerStyle);
            writeQueryResults(workbook, debugResult, headerStyle);
            writeProperties(workbook, debugResult, headerStyle);
            workbook.write(outputStream);
        }
    }

    private void writeSummary(
            Workbook workbook,
            DebugResult debugResult,
            String debugTitle,
            CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Summary");
        writeKeyValue(sheet, 0, "Debug Type", debugTitle, headerStyle);
        writeKeyValue(sheet, 1, "Found", String.valueOf(debugResult.isFound()), headerStyle);
        writeKeyValue(sheet, 2, "Object ID", String.valueOf(debugResult.getObjectId()), headerStyle);
        writeKeyValue(sheet, 3, "Object Name", debugResult.getObjectName(), headerStyle);
        writeKeyValue(sheet, 4, "CUID", debugResult.getObjectCuid(), headerStyle);
        writeKeyValue(sheet, 5, "Kind", debugResult.getObjectKind(), headerStyle);
        autoSize(sheet, 2);
    }

    private void writeQueryResults(Workbook workbook, DebugResult debugResult, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Query Results");
        int rowIndex = 0;

        for (DebugResult.QueryResult queryResult : debugResult.getQueryResults()) {
            Row labelRow = sheet.createRow(rowIndex++);
            labelRow.createCell(0).setCellValue(queryResult.getLabel());

            Row queryRow = sheet.createRow(rowIndex++);
            queryRow.createCell(0).setCellValue("Query");
            queryRow.createCell(1).setCellValue(queryResult.getQuery());

            if (!queryResult.getError().isEmpty()) {
                Row errorRow = sheet.createRow(rowIndex++);
                errorRow.createCell(0).setCellValue("Error");
                errorRow.createCell(1).setCellValue(queryResult.getError());
            }

            Row headerRow = sheet.createRow(rowIndex++);
            writeHeaders(headerRow, headerStyle, "ID", "Name", "Kind", "CUID");

            for (DebugResult.Row resultRow : queryResult.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(resultRow.getId());
                row.createCell(1).setCellValue(resultRow.getName());
                row.createCell(2).setCellValue(resultRow.getKind());
                row.createCell(3).setCellValue(resultRow.getCuid());
            }

            rowIndex++;
        }

        autoSize(sheet, 4);
    }

    private void writeProperties(Workbook workbook, DebugResult debugResult, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Properties");
        Row headerRow = sheet.createRow(0);
        writeHeaders(headerRow, headerStyle, "Property", "Value");

        int rowIndex = 1;
        for (DebugResult.Row property : debugResult.getProperties()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(property.getId());
            row.createCell(1).setCellValue(property.getName());
        }

        autoSize(sheet, 2);
    }

    private void writeKeyValue(Sheet sheet, int rowIndex, String key, String value, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private void writeHeaders(Row row, CellStyle headerStyle, String... headers) {
        for (int column = 0; column < headers.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers[column]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        return headerStyle;
    }
}
