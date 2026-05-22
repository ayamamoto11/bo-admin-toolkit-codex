package com.example.sapbo.modules.folders;

import java.io.Serializable;

public class FolderReplicationRecord implements Serializable {
    private final int sourceFolderId;
    private final String sourceFolderCuid;
    private final String sourceFolderName;
    private final String sourceFolderPath;
    private final String destinationRootName;
    private final String destinationFolderPath;
    private final String status;
    private final String message;

    public FolderReplicationRecord(int sourceFolderId, String sourceFolderCuid, String sourceFolderName,
            String sourceFolderPath, String destinationRootName, String destinationFolderPath, String status,
            String message) {
        this.sourceFolderId = sourceFolderId;
        this.sourceFolderCuid = sourceFolderCuid;
        this.sourceFolderName = sourceFolderName;
        this.sourceFolderPath = sourceFolderPath;
        this.destinationRootName = destinationRootName;
        this.destinationFolderPath = destinationFolderPath;
        this.status = status;
        this.message = message;
    }

    public int getSourceFolderId() { return sourceFolderId; }
    public String getSourceFolderCuid() { return sourceFolderCuid; }
    public String getSourceFolderName() { return sourceFolderName; }
    public String getSourceFolderPath() { return sourceFolderPath; }
    public String getDestinationRootName() { return destinationRootName; }
    public String getDestinationFolderPath() { return destinationFolderPath; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public boolean isReady() { return "Ready".equals(status); }
}
