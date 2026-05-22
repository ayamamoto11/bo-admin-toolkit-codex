package com.example.sapbo.modules.reports;

import java.io.Serializable;

public class ReportArchiveRecord implements Serializable {
    private final int rowNumber;
    private final String cuid;
    private final String reportName;
    private final String currentFolderPath;
    private final String archiveFolderName;
    private final String targetFolderPath;
    private final String finalReportName;
    private final String status;
    private final String message;

    public ReportArchiveRecord(
            int rowNumber,
            String cuid,
            String reportName,
            String currentFolderPath,
            String archiveFolderName,
            String targetFolderPath,
            String finalReportName,
            String status,
            String message) {
        this.rowNumber = rowNumber;
        this.cuid = cuid;
        this.reportName = reportName;
        this.currentFolderPath = currentFolderPath;
        this.archiveFolderName = archiveFolderName;
        this.targetFolderPath = targetFolderPath;
        this.finalReportName = finalReportName;
        this.status = status;
        this.message = message;
    }

    public int getRowNumber() { return rowNumber; }
    public String getCuid() { return cuid; }
    public String getReportName() { return reportName; }
    public String getCurrentFolderPath() { return currentFolderPath; }
    public String getArchiveFolderName() { return archiveFolderName; }
    public String getTargetFolderPath() { return targetFolderPath; }
    public String getFinalReportName() { return finalReportName; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }

    public boolean isReady() { return "Ready".equals(status); }
}
