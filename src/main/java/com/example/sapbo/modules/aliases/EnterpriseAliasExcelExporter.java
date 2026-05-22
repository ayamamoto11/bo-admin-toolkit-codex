package com.example.sapbo.modules.aliases;

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

public class EnterpriseAliasExcelExporter {
    private static final String[] HEADERS = {
            "User ID", "Username", "Full Name", "User CUID", "AD Aliases",
            "Enterprise Alias", "Password", "Disabled", "Status", "Message"
    };

    public void export(List<EnterpriseAliasRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Enterprise Aliases");
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (EnterpriseAliasRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getUserId());
                row.createCell(1).setCellValue(record.getUsername());
                row.createCell(2).setCellValue(record.getFullName());
                row.createCell(3).setCellValue(record.getUserCuid());
                row.createCell(4).setCellValue(record.getAdAliases());
                row.createCell(5).setCellValue(record.getEnterpriseAlias());
                row.createCell(6).setCellValue(record.getPassword());
                row.createCell(7).setCellValue(record.getDisabled());
                row.createCell(8).setCellValue(record.getStatus());
                row.createCell(9).setCellValue(record.getMessage());
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
