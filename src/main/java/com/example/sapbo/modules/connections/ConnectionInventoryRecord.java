package com.example.sapbo.modules.connections;

import java.io.Serializable;

public class ConnectionInventoryRecord implements Serializable {
    private final int connectionId;
    private final String connectionName;
    private final String connectionCuid;
    private final String connectionKind;
    private final int parentFolderId;
    private final String folderPath;
    private final String description;
    private final String connectionType;
    private final String databaseName;
    private final String serverName;
    private final String dataSource;
    private final String networkLayer;
    private final String userName;
    private final String metadataSummary;

    public ConnectionInventoryRecord(
            int connectionId,
            String connectionName,
            String connectionCuid,
            String connectionKind,
            int parentFolderId,
            String folderPath,
            String description,
            String connectionType,
            String databaseName,
            String serverName,
            String dataSource,
            String networkLayer,
            String userName,
            String metadataSummary) {
        this.connectionId = connectionId;
        this.connectionName = connectionName;
        this.connectionCuid = connectionCuid;
        this.connectionKind = connectionKind;
        this.parentFolderId = parentFolderId;
        this.folderPath = folderPath;
        this.description = description;
        this.connectionType = connectionType;
        this.databaseName = databaseName;
        this.serverName = serverName;
        this.dataSource = dataSource;
        this.networkLayer = networkLayer;
        this.userName = userName;
        this.metadataSummary = metadataSummary;
    }

    public int getConnectionId() { return connectionId; }
    public String getConnectionName() { return connectionName; }
    public String getConnectionCuid() { return connectionCuid; }
    public String getConnectionKind() { return connectionKind; }
    public int getParentFolderId() { return parentFolderId; }
    public String getFolderPath() { return folderPath; }
    public String getDescription() { return description; }
    public String getConnectionType() { return connectionType; }
    public String getDatabaseName() { return databaseName; }
    public String getServerName() { return serverName; }
    public String getDataSource() { return dataSource; }
    public String getNetworkLayer() { return networkLayer; }
    public String getUserName() { return userName; }
    public String getMetadataSummary() { return metadataSummary; }
}
