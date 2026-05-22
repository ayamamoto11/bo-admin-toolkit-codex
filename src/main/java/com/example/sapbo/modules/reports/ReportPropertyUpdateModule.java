package com.example.sapbo.modules.reports;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.example.sapbo.core.ConnectionManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportPropertyUpdateModule {
    public static final String IDENTIFIER_ID = "id";
    public static final String IDENTIFIER_CUID = "cuid";

    private static final String STATUS_READY = "Ready";
    private static final String STATUS_ERROR = "Error";
    private static final String STATUS_SKIPPED = "Skipped";

    private final ConnectionManager connectionManager;

    public ReportPropertyUpdateModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<ReportPropertyUpdateRecord> validate(InputStream inputStream, String identifierType)
            throws IOException, SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        List<ReportPropertyUpdateRecord> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return records;
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = readHeaderColumns(sheet.getRow(sheet.getFirstRowNum()), formatter);
            String identifierColumn = IDENTIFIER_CUID.equals(identifierType) ? "cuid" : "id";
            Integer identifierIndex = columns.get(identifierColumn);

            if (identifierIndex == null) {
                records.add(new ReportPropertyUpdateRecord(
                        1,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        STATUS_ERROR,
                        "Missing required " + identifierColumn.toUpperCase(Locale.ENGLISH) + " column."));
                return records;
            }

            Integer nameIndex = columns.get("name");
            Integer descriptionIndex = columns.get("description");
            Integer keywordIndex = columns.get("keyword");

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String identifier = readCell(row, identifierIndex, formatter);
                String name = readOptionalCell(row, nameIndex, formatter);
                String description = readOptionalCell(row, descriptionIndex, formatter);
                String keyword = readOptionalCell(row, keywordIndex, formatter);

                if (identifier.isEmpty() && name.isEmpty() && description.isEmpty() && keyword.isEmpty()) {
                    continue;
                }

                records.add(validateRow(
                        infoStore,
                        rowIndex + 1,
                        identifierType,
                        identifier,
                        name,
                        description,
                        keyword));
            }
        }

        return records;
    }

    public List<ReportPropertyUpdateRecord> applyUpdates(
            List<ReportPropertyUpdateRecord> validatedRecords,
            String identifierType) throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects changedReports = infoStore.newInfoObjectCollection();
        Map<String, ReportPropertyUpdateRecord> changedByIdentifier = new LinkedHashMap<>();
        List<ReportPropertyUpdateRecord> results = new ArrayList<>();

        for (ReportPropertyUpdateRecord record : validatedRecords) {
            if (!record.isReady()) {
                results.add(record);
                continue;
            }

            IInfoObject report = queryReport(infoStore, identifierType, record.getIdentifier());
            if (report == null) {
                results.add(copyWithStatus(record, STATUS_ERROR, "Report was not found at update time."));
                continue;
            }

            if (!record.getNewName().isEmpty()) {
                report.setTitle(record.getNewName());
            }
            if (!record.getNewDescription().isEmpty()) {
                report.setDescription(record.getNewDescription());
            }
            if (!record.getNewKeyword().isEmpty()) {
                report.setKeyword(record.getNewKeyword());
            }

            changedReports.add(report);
            changedByIdentifier.put(record.getIdentifier(), record);
        }

        if (!changedReports.isEmpty()) {
            infoStore.commit(changedReports);
        }

        for (ReportPropertyUpdateRecord record : validatedRecords) {
            if (changedByIdentifier.containsKey(record.getIdentifier())) {
                results.add(copyWithStatus(record, "Updated", "Report properties were updated."));
            }
        }

        return results;
    }

    private ReportPropertyUpdateRecord validateRow(
            IInfoStore infoStore,
            int rowNumber,
            String identifierType,
            String identifier,
            String name,
            String description,
            String keyword) throws SDKException {
        if (identifier.isEmpty()) {
            return new ReportPropertyUpdateRecord(
                    rowNumber, identifier, "", "", "", name, description, keyword,
                    STATUS_ERROR, "Missing identifier.");
        }

        if (IDENTIFIER_ID.equals(identifierType) && !isPositiveInteger(identifier)) {
            return new ReportPropertyUpdateRecord(
                    rowNumber, identifier, "", "", "", name, description, keyword,
                    STATUS_ERROR, "ID must be numeric.");
        }

        if (name.isEmpty() && description.isEmpty() && keyword.isEmpty()) {
            return new ReportPropertyUpdateRecord(
                    rowNumber, identifier, "", "", "", name, description, keyword,
                    STATUS_SKIPPED, "No update values were provided.");
        }

        IInfoObject report = queryReport(infoStore, identifierType, identifier);
        if (report == null) {
            return new ReportPropertyUpdateRecord(
                    rowNumber, identifier, "", "", "", name, description, keyword,
                    STATUS_ERROR, "Report was not found or is not a WebI report.");
        }

        return new ReportPropertyUpdateRecord(
                rowNumber,
                identifier,
                String.valueOf(report.getID()),
                report.getCUID(),
                report.getTitle(),
                name,
                description,
                keyword,
                STATUS_READY,
                "Valid WebI report.");
    }

    private IInfoObject queryReport(IInfoStore infoStore, String identifierType, String identifier)
            throws SDKException {
        String predicate;
        if (IDENTIFIER_CUID.equals(identifierType)) {
            predicate = "SI_CUID = '" + escapeCmsQueryValue(identifier) + "'";
        } else {
            predicate = "SI_ID = " + identifier;
        }

        IInfoObjects objects = infoStore.query("SELECT TOP 1 * FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Webi' AND " + predicate);
        if (objects.size() == 0) {
            return null;
        }

        return (IInfoObject) objects.get(0);
    }

    private Map<String, Integer> readHeaderColumns(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        if (headerRow == null) {
            return columns;
        }

        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (!header.isEmpty()) {
                columns.put(header, cell.getColumnIndex());
            }
        }

        return columns;
    }

    private String normalizeHeader(String header) {
        String cleanHeader = header == null ? "" : header.trim().toLowerCase(Locale.ENGLISH);
        cleanHeader = cleanHeader.replace(" ", "").replace("_", "");
        if ("siid".equals(cleanHeader) || "objectid".equals(cleanHeader) || "reportid".equals(cleanHeader)) {
            return "id";
        }
        if ("sicuid".equals(cleanHeader) || "reportcuid".equals(cleanHeader) || "objectcuid".equals(cleanHeader)) {
            return "cuid";
        }
        if ("reportname".equals(cleanHeader) || "newname".equals(cleanHeader)) {
            return "name";
        }
        if ("sidescription".equals(cleanHeader) || "newdescription".equals(cleanHeader)) {
            return "description";
        }
        if ("sikeyword".equals(cleanHeader) || "keywords".equals(cleanHeader) || "newkeyword".equals(cleanHeader)) {
            return "keyword";
        }

        return cleanHeader;
    }

    private String readOptionalCell(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return "";
        }

        return readCell(row, columnIndex, formatter);
    }

    private String readCell(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private ReportPropertyUpdateRecord copyWithStatus(
            ReportPropertyUpdateRecord record,
            String status,
            String message) {
        return new ReportPropertyUpdateRecord(
                record.getRowNumber(),
                record.getIdentifier(),
                record.getReportId(),
                record.getReportCuid(),
                record.getCurrentName(),
                record.getNewName(),
                record.getNewDescription(),
                record.getNewKeyword(),
                status,
                message);
    }

    private String escapeCmsQueryValue(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean isPositiveInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmedValue = value.trim();
        for (int index = 0; index < trimmedValue.length(); index++) {
            if (!Character.isDigit(trimmedValue.charAt(index))) {
                return false;
            }
        }

        return Integer.parseInt(trimmedValue) > 0;
    }
}
