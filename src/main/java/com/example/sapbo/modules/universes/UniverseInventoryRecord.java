package com.example.sapbo.modules.universes;

import java.io.Serializable;

public class UniverseInventoryRecord implements Serializable {
    private final int universeId;
    private final String universeName;
    private final String universeCuid;
    private final String universeType;
    private final int parentFolderId;
    private final String folderPath;
    private final String connectionId;
    private final String connectionName;
    private final String connectionCuid;
    private final String connectionKind;
    private final String connectionType;
    private final String databaseName;

    public UniverseInventoryRecord(
            int universeId,
            String universeName,
            String universeCuid,
            String universeType,
            int parentFolderId,
            String folderPath,
            String connectionId,
            String connectionName,
            String connectionCuid,
            String connectionKind,
            String connectionType,
            String databaseName) {
        this.universeId = universeId;
        this.universeName = universeName;
        this.universeCuid = universeCuid;
        this.universeType = universeType;
        this.parentFolderId = parentFolderId;
        this.folderPath = folderPath;
        this.connectionId = connectionId;
        this.connectionName = connectionName;
        this.connectionCuid = connectionCuid;
        this.connectionKind = connectionKind;
        this.connectionType = connectionType;
        this.databaseName = databaseName;
    }

    public int getUniverseId() {
        return universeId;
    }

    public String getUniverseName() {
        return universeName;
    }

    public String getUniverseCuid() {
        return universeCuid;
    }

    public String getUniverseType() {
        return universeType;
    }

    public int getParentFolderId() {
        return parentFolderId;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getConnectionCuid() {
        return connectionCuid;
    }

    public String getConnectionKind() {
        return connectionKind;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
