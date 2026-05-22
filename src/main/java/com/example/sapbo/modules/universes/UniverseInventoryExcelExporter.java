package com.example.sapbo.modules.universes;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class UniverseInventoryExcelExporter {
    private static final String[] HEADERS = {
            "Universe ID",
            "Universe Name",
            "Universe CUID",
            "Universe Type",
            "Parent Folder ID",
            "Folder Path",
            "Connection ID",
            "Connection Name",
            "Connection CUID",
            "Connection Kind",
            "Connection Type",
            "Database"
    };

    public void export(List<UniverseInventoryRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Universes");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (UniverseInventoryRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getUniverseId());
                row.createCell(1).setCellValue(record.getUniverseName());
                row.createCell(2).setCellValue(record.getUniverseCuid());
                row.createCell(3).setCellValue(record.getUniverseType());
                row.createCell(4).setCellValue(record.getParentFolderId());
                row.createCell(5).setCellValue(record.getFolderPath());
                row.createCell(6).setCellValue(record.getConnectionId());
                row.createCell(7).setCellValue(record.getConnectionName());
                row.createCell(8).setCellValue(record.getConnectionCuid());
                row.createCell(9).setCellValue(record.getConnectionKind());
                row.createCell(10).setCellValue(record.getConnectionType());
                row.createCell(11).setCellValue(record.getDatabaseName());
            }

            for (int column = 0; column < HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
            }

            workbook.write(outputStream);
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
