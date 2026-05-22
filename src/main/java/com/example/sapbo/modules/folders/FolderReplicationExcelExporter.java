package com.example.sapbo.modules.folders;

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

public class FolderReplicationExcelExporter {
    private static final String[] HEADERS = {
            "Source Folder ID", "Source Folder CUID", "Source Folder Name", "Source Folder Path",
            "Destination Root", "Destination Folder Path", "Status", "Message"
    };

    public void export(List<FolderReplicationRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Folder Replication");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (FolderReplicationRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getSourceFolderId());
                row.createCell(1).setCellValue(record.getSourceFolderCuid());
                row.createCell(2).setCellValue(record.getSourceFolderName());
                row.createCell(3).setCellValue(record.getSourceFolderPath());
                row.createCell(4).setCellValue(record.getDestinationRootName());
                row.createCell(5).setCellValue(record.getDestinationFolderPath());
                row.createCell(6).setCellValue(record.getStatus());
                row.createCell(7).setCellValue(record.getMessage());
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
