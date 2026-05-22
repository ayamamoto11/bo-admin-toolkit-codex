package com.example.sapbo.modules.users;

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

public class UserGroupInventoryExcelExporter {
    private static final String[] HEADERS = {
            "Object Type",
            "Object ID",
            "Object Name",
            "Object CUID",
            "Parent Group ID",
            "Parent Group Name",
            "Group Path",
            "Disabled"
    };

    public void export(List<UserGroupInventoryRecord> records, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users and Groups");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (UserGroupInventoryRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getObjectType());
                row.createCell(1).setCellValue(record.getObjectId());
                row.createCell(2).setCellValue(record.getObjectName());
                row.createCell(3).setCellValue(record.getObjectCuid());
                row.createCell(4).setCellValue(record.getParentGroupId());
                row.createCell(5).setCellValue(record.getParentGroupName());
                row.createCell(6).setCellValue(record.getGroupPath());
                row.createCell(7).setCellValue(record.getDisabled());
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
