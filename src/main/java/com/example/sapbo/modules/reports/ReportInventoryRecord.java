package com.example.sapbo.modules.reports;

import java.io.Serializable;

public class ReportInventoryRecord implements Serializable {
    private final String reportName;
    private final int objectId;
    private final String cuid;
    private final int ownerId;
    private final int parentFolderId;
    private final String folderPath;
    private final String universeName;
    private final String universeType;
    private final String universeCuid;

    public ReportInventoryRecord(
            String reportName,
            int objectId,
            String cuid,
            int ownerId,
            int parentFolderId,
            String folderPath,
            String universeName,
            String universeType,
            String universeCuid) {
        this.reportName = reportName;
        this.objectId = objectId;
        this.cuid = cuid;
        this.ownerId = ownerId;
        this.parentFolderId = parentFolderId;
        this.folderPath = folderPath;
        this.universeName = universeName;
        this.universeType = universeType;
        this.universeCuid = universeCuid;
    }

    public String getReportName() {
        return reportName;
    }

    public int getObjectId() {
        return objectId;
    }

    public String getCuid() {
        return cuid;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getParentFolderId() {
        return parentFolderId;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getUniverseName() {
        return universeName;
    }

    public String getUniverseType() {
        return universeType;
    }

    public String getUniverseCuid() {
        return universeCuid;
    }
}
