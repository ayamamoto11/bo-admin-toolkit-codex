package com.example.sapbo.core;

import java.util.Collections;
import java.util.List;

public class DebugResult {
    private final int objectId;
    private final String objectName;
    private final String objectCuid;
    private final String objectKind;
    private final boolean found;
    private final List<Row> properties;
    private final List<QueryResult> queryResults;

    public DebugResult(
            int objectId,
            String objectName,
            String objectCuid,
            String objectKind,
            List<Row> properties,
            List<QueryResult> queryResults) {
        this.objectId = objectId;
        this.objectName = objectName;
        this.objectCuid = objectCuid;
        this.objectKind = objectKind;
        this.found = true;
        this.properties = properties;
        this.queryResults = queryResults;
    }

    private DebugResult(int objectId) {
        this.objectId = objectId;
        this.objectName = "";
        this.objectCuid = "";
        this.objectKind = "";
        this.found = false;
        this.properties = Collections.emptyList();
        this.queryResults = Collections.emptyList();
    }

    public static DebugResult notFound(int objectId) {
        return new DebugResult(objectId);
    }

    public int getObjectId() {
        return objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectCuid() {
        return objectCuid;
    }

    public String getObjectKind() {
        return objectKind;
    }

    public boolean isFound() {
        return found;
    }

    public List<Row> getProperties() {
        return properties;
    }

    public List<QueryResult> getQueryResults() {
        return queryResults;
    }

    public static class Row {
        private final String id;
        private final String name;
        private final String kind;
        private final String cuid;

        public Row(String id, String name, String kind, String cuid) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.cuid = cuid;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getKind() {
            return kind;
        }

        public String getCuid() {
            return cuid;
        }
    }

    public static class QueryResult {
        private final String label;
        private final String query;
        private final List<Row> rows;
        private final String error;

        public QueryResult(String label, String query, List<Row> rows, String error) {
            this.label = label;
            this.query = query;
            this.rows = rows;
            this.error = error;
        }

        public String getLabel() {
            return label;
        }

        public String getQuery() {
            return query;
        }

        public List<Row> getRows() {
            return rows;
        }

        public String getError() {
            return error;
        }
    }
}
