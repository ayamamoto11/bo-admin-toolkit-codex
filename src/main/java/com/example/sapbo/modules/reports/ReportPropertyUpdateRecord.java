package com.example.sapbo.modules.reports;

import java.io.Serializable;

public class ReportPropertyUpdateRecord implements Serializable {
    private final int rowNumber;
    private final String identifier;
    private final String reportId;
    private final String reportCuid;
    private final String currentName;
    private final String newName;
    private final String newDescription;
    private final String newKeyword;
    private final String status;
    private final String message;

    public ReportPropertyUpdateRecord(
            int rowNumber,
            String identifier,
            String reportId,
            String reportCuid,
            String currentName,
            String newName,
            String newDescription,
            String newKeyword,
            String status,
            String message) {
        this.rowNumber = rowNumber;
        this.identifier = identifier;
        this.reportId = reportId;
        this.reportCuid = reportCuid;
        this.currentName = currentName;
        this.newName = newName;
        this.newDescription = newDescription;
        this.newKeyword = newKeyword;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getReportId() {
        return reportId;
    }

    public String getReportCuid() {
        return reportCuid;
    }

    public String getCurrentName() {
        return currentName;
    }

    public String getNewName() {
        return newName;
    }

    public String getNewDescription() {
        return newDescription;
    }

    public String getNewKeyword() {
        return newKeyword;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReady() {
        return "Ready".equals(status);
    }
}
