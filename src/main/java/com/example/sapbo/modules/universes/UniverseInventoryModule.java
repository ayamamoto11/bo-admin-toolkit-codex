package com.example.sapbo.modules.universes;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import com.example.sapbo.core.ConnectionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UniverseInventoryModule {
    private static final String UNIVERSE_QUERY = "SELECT TOP 100000 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID, "
            + "SI_SPECIFIC_KIND, SI_CONNECTION, SI_DATACONNECTION, SI_DATACONNECTIONS, "
            + "SI_SL_UNIVERSE_TO_CONNECTIONS "
            + "FROM CI_APPOBJECTS "
            + "WHERE SI_KIND IN ('Universe', 'DSL.Universe', 'DSL.MetaDataFile') "
            + "OR SI_SPECIFIC_KIND = 'DSL.Universe' "
            + "ORDER BY SI_NAME";

    private static final String[] CONNECTION_RELATIONSHIPS = {
            "Universe-Connection",
            "Connection-Universe",
            "Universe-DataConnection",
            "DataConnection-Universe",
            "Universe-RelationalConnection",
            "RelationalConnection-Universe",
            "DSLUniverse-Connection",
            "Connection-DSLUniverse",
            "DSL.MetaDataFile-Connection",
            "Connection-DSL.MetaDataFile"
    };

    private final ConnectionManager connectionManager;

    public UniverseInventoryModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<UniverseInventoryRecord> findUniverses() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects universes = infoStore.query(UNIVERSE_QUERY);

        if (universes.size() == 0) {
            return Collections.emptyList();
        }

        FolderPathResolver folderPathResolver = new FolderPathResolver(infoStore);
        List<UniverseInventoryRecord> records = new ArrayList<>();

        for (Object universeObject : universes) {
            IInfoObject universe = (IInfoObject) universeObject;
            records.addAll(toRecords(universe, folderPathResolver));
        }

        return records;
    }

    private List<UniverseInventoryRecord> toRecords(
            IInfoObject universe,
            FolderPathResolver folderPathResolver) throws SDKException {
        int parentFolderId = universe.properties().getInt("SI_PARENTID");
        String folderPath = folderPathResolver.resolve(parentFolderId);
        List<ConnectionReference> connections = findConnections(universe, folderPathResolver);

        if (connections.isEmpty()) {
            connections = Collections.singletonList(ConnectionReference.empty());
        }

        List<UniverseInventoryRecord> records = new ArrayList<>();
        for (ConnectionReference connection : connections) {
            records.add(new UniverseInventoryRecord(
                    universe.getID(),
                    universe.getTitle(),
                    universe.getCUID(),
                    getUniverseType(universe),
                    parentFolderId,
                    folderPath,
                    connection.id,
                    connection.name,
                    connection.cuid,
                    connection.kind,
                    connection.connectionType,
                    connection.databaseName));
        }

        return records;
    }

    private List<ConnectionReference> findConnections(
            IInfoObject universe,
            FolderPathResolver folderPathResolver) throws SDKException {
        List<ConnectionReference> connections = new ArrayList<>();
        Set<String> connectionKeys = new LinkedHashSet<>();

        for (ConnectionReference connection : querySlUniverseConnections(universe, folderPathResolver)) {
            if (connectionKeys.add(connection.key())) {
                connections.add(connection);
            }
        }

        for (ConnectionReference connection : queryConnectionRelationships(universe.getID(), folderPathResolver)) {
            if (connectionKeys.add(connection.key())) {
                connections.add(connection);
            }
        }

        for (ConnectionReference connection : queryConnectionsFromProperties(universe, folderPathResolver)) {
            if (connectionKeys.add(connection.key())) {
                connections.add(connection);
            }
        }

        for (ConnectionReference connection : queryConnectionsFromRelatedMetadataFile(universe, folderPathResolver)) {
            if (connectionKeys.add(connection.key())) {
                connections.add(connection);
            }
        }

        return connections;
    }

    private List<ConnectionReference> querySlUniverseConnections(
            IInfoObject universe,
            FolderPathResolver folderPathResolver) throws SDKException {
        if (!isDslUniverse(universe)) {
            return Collections.emptyList();
        }

        Set<String> connectionIds = new LinkedHashSet<>();
        collectIds(getProperties(universe.properties(), "SI_SL_UNIVERSE_TO_CONNECTIONS"), connectionIds);

        if (connectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID, SI_DESCRIPTION "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + buildIdPredicate(connectionIds);
        return toConnectionReferences(folderPathResolver.infoStore.query(query));
    }

    private List<ConnectionReference> queryConnectionRelationships(
            int universeId,
            FolderPathResolver folderPathResolver) {
        List<ConnectionReference> connections = new ArrayList<>();
        Set<String> connectionKeys = new LinkedHashSet<>();

        for (String relationshipName : CONNECTION_RELATIONSHIPS) {
            addConnections(connections, connectionKeys,
                    queryConnectionRelationship(universeId, folderPathResolver, relationshipName, true));
            addConnections(connections, connectionKeys,
                    queryConnectionRelationship(universeId, folderPathResolver, relationshipName, false));
        }

        return connections;
    }

    private void addConnections(
            List<ConnectionReference> target,
            Set<String> connectionKeys,
            List<ConnectionReference> source) {
        for (ConnectionReference connection : source) {
            if (connectionKeys.add(connection.key())) {
                target.add(connection);
            }
        }
    }

    private List<ConnectionReference> queryConnectionRelationship(
            int universeId,
            FolderPathResolver folderPathResolver,
            String relationshipName,
            boolean childRelationship) {
        String relationshipFunction = childRelationship ? "CHILDREN" : "PARENTS";
        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID, SI_DESCRIPTION "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + relationshipFunction + "(\"SI_NAME='" + relationshipName + "'\", \"SI_ID=" + universeId + "\")";

        try {
            return toConnectionReferences(folderPathResolver.infoStore.query(query));
        } catch (SDKException exception) {
            return Collections.emptyList();
        }
    }

    private List<ConnectionReference> queryConnectionsFromProperties(
            IInfoObject universe,
            FolderPathResolver folderPathResolver) throws SDKException {
        Set<String> connectionIds = new LinkedHashSet<>();
        collectConnectionIds(universe.properties(), connectionIds, false, 0);

        if (connectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID, SI_DESCRIPTION "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + buildIdPredicate(connectionIds);
        return toConnectionReferences(folderPathResolver.infoStore.query(query));
    }

    private List<ConnectionReference> queryConnectionsFromRelatedMetadataFile(
            IInfoObject universe,
            FolderPathResolver folderPathResolver) throws SDKException {
        if (!isDslUniverse(universe)) {
            return Collections.emptyList();
        }

        String query = "SELECT TOP 20 * "
                + "FROM CI_APPOBJECTS "
                + "WHERE SI_KIND = 'DSL.MetaDataFile' "
                + "AND SI_NAME = '" + escapeCmsQueryValue(universe.getTitle()) + "'";
        IInfoObjects metadataFiles = folderPathResolver.infoStore.query(query);
        if (metadataFiles.size() == 0) {
            return Collections.emptyList();
        }

        List<ConnectionReference> connections = new ArrayList<>();
        Set<String> connectionKeys = new LinkedHashSet<>();
        for (Object metadataFileObject : metadataFiles) {
            IInfoObject metadataFile = (IInfoObject) metadataFileObject;
            for (ConnectionReference connection : queryConnectionsFromProperties(metadataFile, folderPathResolver)) {
                if (connectionKeys.add(connection.key())) {
                    connections.add(connection);
                }
            }
            for (ConnectionReference connection : queryConnectionRelationships(metadataFile.getID(), folderPathResolver)) {
                if (connectionKeys.add(connection.key())) {
                    connections.add(connection);
                }
            }
        }

        return connections;
    }

    private void collectConnectionIds(
            IProperties properties,
            Set<String> connectionIds,
            boolean insideConnectionContainer,
            int depth) {
        if (depth > 5) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject).toUpperCase(Locale.ENGLISH);
            IProperty property = properties.getProperty(keyObject);
            boolean connectionKey = key.contains("CONNECTION");

            if (property != null && property.isContainer()) {
                IProperties childProperties = getProperties(properties, keyObject);
                if (childProperties != null) {
                    collectConnectionIds(
                            childProperties,
                            connectionIds,
                            insideConnectionContainer || connectionKey,
                            depth + 1);
                }
                continue;
            }

            if (insideConnectionContainer || connectionKey) {
                addPositiveInteger(connectionIds, safeGetString(properties, keyObject));
            }
        }
    }

    private void collectIds(IProperties properties, Set<String> ids) {
        if (properties == null) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            IProperty property = properties.getProperty(keyObject);
            if (property != null && property.isContainer()) {
                collectIds(getProperties(properties, keyObject), ids);
                continue;
            }

            addPositiveInteger(ids, safeGetString(properties, keyObject));
        }
    }

    private List<ConnectionReference> toConnectionReferences(IInfoObjects objects) throws SDKException {
        List<ConnectionReference> connections = new ArrayList<>();

        for (Object object : objects) {
            IInfoObject infoObject = (IInfoObject) object;
            Map<String, String> metadata = new HashMap<>();
            flattenProperties("", infoObject.properties(), metadata, 0);
            connections.add(new ConnectionReference(
                    String.valueOf(infoObject.getID()),
                    infoObject.getTitle(),
                    infoObject.getCUID(),
                    infoObject.getKind(),
                    inferConnectionType(infoObject, metadata),
                    inferDatabaseName(metadata)));
        }

        return connections;
    }

    private boolean looksLikeConnection(IInfoObject infoObject) throws SDKException {
        String kind = normalize(infoObject.getKind());
        String name = normalize(infoObject.getTitle());
        return kind.contains("connection") || name.contains("connection");
    }

    private void flattenProperties(
            String prefix,
            IProperties properties,
            Map<String, String> metadata,
            int depth) {
        if (depth > 5) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            IProperty property = properties.getProperty(keyObject);

            if (property != null && property.isContainer()) {
                IProperties childProperties = getProperties(properties, keyObject);
                if (childProperties != null) {
                    flattenProperties(path, childProperties, metadata, depth + 1);
                }
            } else {
                String value = safeGetString(properties, keyObject);
                if (!value.isEmpty()) {
                    metadata.put(path, value);
                }
            }
        }
    }

    private String inferConnectionType(IInfoObject connection, Map<String, String> metadata) throws SDKException {
        String combined = normalize(connection.getKind() + " " + connection.getTitle() + " " + joinValues(metadata));
        if (combined.contains("odbc")) {
            return "ODBC";
        }
        if (combined.contains("oledb") || combined.contains("ole db")) {
            return "OLE DB";
        }
        if (combined.contains("jdbc")) {
            return "JDBC";
        }
        if (combined.contains("oracle")) {
            return "Oracle Client";
        }
        if (combined.contains("sql server") || combined.contains("sqlserver")) {
            return "SQL Server";
        }
        if (combined.contains("hana")) {
            return "SAP HANA";
        }
        if (combined.contains("db2")) {
            return "DB2";
        }
        if (combined.contains("sybase")) {
            return "Sybase";
        }
        if (combined.contains("teradata")) {
            return "Teradata";
        }

        return "";
    }

    private String inferDatabaseName(Map<String, String> metadata) {
        String[] databasePatterns = {
                "DATABASE",
                "DBNAME",
                "CATALOG",
                "DATASOURCE",
                "DATA_SOURCE",
                "DSN",
                "SERVER"
        };

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = normalize(entry.getKey());
            for (String pattern : databasePatterns) {
                if (key.contains(pattern.toLowerCase(Locale.ENGLISH))) {
                    String value = scrubSensitiveValue(entry.getValue());
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }

        return "";
    }

    private String scrubSensitiveValue(String value) {
        String cleanValue = value == null ? "" : value.trim();
        String lowerValue = cleanValue.toLowerCase(Locale.ENGLISH);
        if (lowerValue.contains("password") || lowerValue.contains("pwd=")) {
            return "";
        }

        return cleanValue;
    }

    private String getUniverseType(IInfoObject universe) throws SDKException {
        String kind = universe.getKind();
        if (kind != null && (kind.equalsIgnoreCase("DSL.Universe") || kind.equalsIgnoreCase("DSL.MetaDataFile"))) {
            return "UNX";
        }
        if (kind != null && kind.toLowerCase(Locale.ENGLISH).contains("dsl")) {
            return "UNX";
        }
        if (kind != null && kind.equalsIgnoreCase("Universe")) {
            return "UNV";
        }

        return kind == null ? "" : kind;
    }

    private boolean isDslUniverse(IInfoObject universe) throws SDKException {
        String kind = universe.getKind();
        return kind != null && kind.toLowerCase(Locale.ENGLISH).contains("dsl");
    }

    private String escapeCmsQueryValue(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String buildIdPredicate(Set<String> ids) {
        List<String> predicates = new ArrayList<>();
        for (String id : ids) {
            predicates.add("SI_ID = " + id);
        }

        return String.join(" OR ", predicates);
    }

    private void addPositiveInteger(Set<String> values, String value) {
        if (isPositiveInteger(value)) {
            values.add(value.trim());
        }
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

    private IProperties getProperties(IProperties properties, Object propertyName) {
        if (properties.containsKey(propertyName)) {
            return properties.getProperties(propertyName);
        }

        if (propertyName instanceof String && isPositiveInteger((String) propertyName)) {
            Integer numericPropertyName = Integer.valueOf((String) propertyName);
            if (properties.containsKey(numericPropertyName)) {
                return properties.getProperties(numericPropertyName);
            }
        }

        return null;
    }

    private String safeGetString(IProperties properties, Object keyObject) {
        try {
            return properties.getString(String.valueOf(keyObject));
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private String joinValues(Map<String, String> metadata) {
        StringBuilder builder = new StringBuilder();
        for (String value : metadata.values()) {
            builder.append(' ').append(value);
        }

        return builder.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }

    private static final class ConnectionReference {
        private final String id;
        private final String name;
        private final String cuid;
        private final String kind;
        private final String connectionType;
        private final String databaseName;

        private ConnectionReference(
                String id,
                String name,
                String cuid,
                String kind,
                String connectionType,
                String databaseName) {
            this.id = id;
            this.name = name;
            this.cuid = cuid;
            this.kind = kind;
            this.connectionType = connectionType;
            this.databaseName = databaseName;
        }

        private static ConnectionReference empty() {
            return new ConnectionReference("", "", "", "", "", "");
        }

        private String key() {
            if (!id.isEmpty()) {
                return id;
            }

            return name + "|" + cuid;
        }
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
