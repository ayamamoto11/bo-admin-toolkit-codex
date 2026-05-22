package com.example.sapbo.modules.reports;

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

public class ReportArchiveExcelExporter {
    private static final String[] HEADERS = {
            "Row", "CUID", "Report Name", "Current Folder Path", "Archive Folder",
            "Target Folder Path", "Final Report Name", "Status", "Message"
    };

    public void export(List<ReportArchiveRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report Archive");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (ReportArchiveRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getRowNumber());
                row.createCell(1).setCellValue(record.getCuid());
                row.createCell(2).setCellValue(record.getReportName());
                row.createCell(3).setCellValue(record.getCurrentFolderPath());
                row.createCell(4).setCellValue(record.getArchiveFolderName());
                row.createCell(5).setCellValue(record.getTargetFolderPath());
                row.createCell(6).setCellValue(record.getFinalReportName());
                row.createCell(7).setCellValue(record.getStatus());
                row.createCell(8).setCellValue(record.getMessage());
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
