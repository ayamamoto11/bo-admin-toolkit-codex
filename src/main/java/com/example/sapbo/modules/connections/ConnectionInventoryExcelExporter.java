package com.example.sapbo.modules.connections;

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

public class ConnectionInventoryExcelExporter {
    private static final String[] HEADERS = {
            "Connection ID", "Connection Name", "Connection CUID", "Connection Kind",
            "Parent Folder ID", "Folder Path", "Description", "Connection Type", "Database",
            "Server", "Data Source", "Network Layer", "User Name", "Metadata Summary"
    };

    public void export(List<ConnectionInventoryRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Connections");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (ConnectionInventoryRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getConnectionId());
                row.createCell(1).setCellValue(record.getConnectionName());
                row.createCell(2).setCellValue(record.getConnectionCuid());
                row.createCell(3).setCellValue(record.getConnectionKind());
                row.createCell(4).setCellValue(record.getParentFolderId());
                row.createCell(5).setCellValue(record.getFolderPath());
                row.createCell(6).setCellValue(record.getDescription());
                row.createCell(7).setCellValue(record.getConnectionType());
                row.createCell(8).setCellValue(record.getDatabaseName());
                row.createCell(9).setCellValue(record.getServerName());
                row.createCell(10).setCellValue(record.getDataSource());
                row.createCell(11).setCellValue(record.getNetworkLayer());
                row.createCell(12).setCellValue(record.getUserName());
                row.createCell(13).setCellValue(record.getMetadataSummary());
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
