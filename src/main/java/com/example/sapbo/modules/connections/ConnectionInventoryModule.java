package com.example.sapbo.modules.connections;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import com.example.sapbo.core.ConnectionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConnectionInventoryModule {
    private static final String CONNECTION_QUERY = "SELECT TOP 100000 * "
            + "FROM CI_APPOBJECTS "
            + "ORDER BY SI_NAME";

    private final ConnectionManager connectionManager;

    public ConnectionInventoryModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<ConnectionInventoryRecord> findConnections() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects objects = infoStore.query(CONNECTION_QUERY);
        FolderPathResolver folderPathResolver = new FolderPathResolver(infoStore);

        List<ConnectionInventoryRecord> records = new ArrayList<>();
        for (Object object : objects) {
            IInfoObject infoObject = (IInfoObject) object;
            if (!looksLikeConnection(infoObject)) {
                continue;
            }
            records.add(toRecord(infoObject, folderPathResolver));
        }
        return records;
    }

    private ConnectionInventoryRecord toRecord(IInfoObject connection, FolderPathResolver folderPathResolver)
            throws SDKException {
        Map<String, String> metadata = new LinkedHashMap<>();
        flattenProperties("", connection.properties(), metadata, 0);
        int parentFolderId = safeGetInt(connection.properties(), "SI_PARENTID");
        return new ConnectionInventoryRecord(
                connection.getID(),
                connection.getTitle(),
                connection.getCUID(),
                connection.getKind(),
                parentFolderId,
                folderPathResolver.resolve(parentFolderId),
                safeGetString(connection.properties(), "SI_DESCRIPTION"),
                firstMatchingValue(metadata, "TYPE", "DRIVER", "NETWORKLAYER", "RDBMS"),
                firstMatchingValue(metadata, "DATABASE", "DBNAME", "CATALOG", "SCHEMA"),
                firstMatchingValue(metadata, "SERVER", "HOST", "HOSTNAME", "MACHINE"),
                firstMatchingValue(metadata, "DATASOURCE", "DATA_SOURCE", "DSN", "SERVICE"),
                firstMatchingValue(metadata, "NETWORKLAYER", "NETWORK_LAYER", "MIDDLEWARE"),
                firstMatchingValue(metadata, "USER", "USERNAME", "DBUSER", "OWNER"),
                summarizeMetadata(metadata));
    }

    private boolean looksLikeConnection(IInfoObject infoObject) throws SDKException {
        String kind = normalize(infoObject.getKind());
        String title = normalize(infoObject.getTitle());
        String specificKind = normalize(safeGetString(infoObject.properties(), "SI_SPECIFIC_KIND"));
        return kind.contains("connection")
                || title.contains("connection")
                || specificKind.contains("connection")
                || "ccis.dataconnection".equals(kind);
    }

    private String firstMatchingValue(Map<String, String> metadata, String... patterns) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = normalize(entry.getKey()).replace("_", "");
            for (String pattern : patterns) {
                if (key.contains(normalize(pattern).replace("_", ""))) {
                    String value = scrubSensitiveValue(entry.getValue());
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "";
    }

    private String summarizeMetadata(Map<String, String> metadata) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = scrubSensitiveValue(entry.getValue());
            if (value.isEmpty() || isNoisyMetadataKey(key)) {
                continue;
            }
            parts.add(key + "=" + value);
            if (parts.size() >= 20) {
                break;
            }
        }
        return String.join("; ", parts);
    }

    private boolean isNoisyMetadataKey(String key) {
        String normalizedKey = normalize(key);
        return normalizedKey.endsWith("16777248")
                || normalizedKey.contains("password")
                || normalizedKey.contains("pwd")
                || normalizedKey.contains("token")
                || normalizedKey.contains("secret");
    }

    private String scrubSensitiveValue(String value) {
        String cleanValue = value == null ? "" : value.trim();
        String lowerValue = cleanValue.toLowerCase(Locale.ENGLISH);
        if (lowerValue.contains("password") || lowerValue.contains("pwd=")
                || lowerValue.contains("token") || lowerValue.contains("secret")) {
            return "";
        }
        return cleanValue;
    }

    private void flattenProperties(String prefix, IProperties properties, Map<String, String> metadata, int depth) {
        if (properties == null || depth > 6) {
            return;
        }
        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            IProperty property = properties.getProperty(keyObject);
            if (property != null && property.isContainer()) {
                flattenProperties(path, getProperties(properties, keyObject), metadata, depth + 1);
            } else {
                String value = safeGetString(properties, keyObject);
                if (!value.isEmpty()) {
                    metadata.put(path, value);
                }
            }
        }
    }

    private IProperties getProperties(IProperties properties, Object propertyName) {
        if (properties == null) {
            return null;
        }
        if (properties.containsKey(propertyName)) {
            return properties.getProperties(propertyName);
        }
        return null;
    }

    private String safeGetString(IProperties properties, Object keyObject) {
        if (properties == null) {
            return "";
        }
        try {
            return properties.getString(String.valueOf(keyObject)).trim();
        } catch (RuntimeException exception) {
            try {
                return String.valueOf(properties.getInt(String.valueOf(keyObject))).trim();
            } catch (RuntimeException intException) {
                IProperty property = properties.getProperty(keyObject);
                if (property != null && property.getValue() != null) {
                    return String.valueOf(property.getValue()).trim();
                }
                return "";
            }
        }
    }

    private int safeGetInt(IProperties properties, String key) {
        try {
            return properties.getInt(key);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }

    private static final class FolderPathResolver {
        private final IInfoStore infoStore;
        private final Map<Integer, String> pathCache = new HashMap<>();

        private FolderPathResolver(IInfoStore infoStore) {
            this.infoStore = infoStore;
        }

        private String resolve(int folderId) throws SDKException {
            if (folderId <= 0) {
                return "";
            }
            if (pathCache.containsKey(folderId)) {
                return pathCache.get(folderId);
            }
            IInfoObjects folders = infoStore.query("SELECT SI_NAME, SI_ID, SI_PARENTID "
                    + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS WHERE SI_ID = " + folderId);
            if (folders.size() == 0) {
                pathCache.put(folderId, "");
                return "";
            }
            IInfoObject folder = (IInfoObject) folders.get(0);
            int parentId = folder.properties().getInt("SI_PARENTID");
            String parentPath = resolve(parentId);
            String folderPath = parentPath.isEmpty() ? folder.getTitle() : parentPath + "/" + folder.getTitle();
            pathCache.put(folderId, folderPath);
            return folderPath;
        }
    }
}
