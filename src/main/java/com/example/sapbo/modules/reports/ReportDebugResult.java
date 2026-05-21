package com.example.sapbo.modules.reports;

import java.util.Collections;
import java.util.List;

public class ReportDebugResult {
    private final int reportId;
    private final String reportName;
    private final String reportCuid;
    private final String reportKind;
    private final boolean found;
    private final List<DebugRow> properties;
    private final List<RelationshipResult> relationshipResults;

    public ReportDebugResult(
            int reportId,
            String reportName,
            String reportCuid,
            String reportKind,
            List<DebugRow> properties,
            List<RelationshipResult> relationshipResults) {
        this.reportId = reportId;
        this.reportName = reportName;
        this.reportCuid = reportCuid;
        this.reportKind = reportKind;
        this.found = true;
        this.properties = properties;
        this.relationshipResults = relationshipResults;
    }

    private ReportDebugResult(int reportId) {
        this.reportId = reportId;
        this.reportName = "";
        this.reportCuid = "";
        this.reportKind = "";
        this.found = false;
        this.properties = Collections.emptyList();
        this.relationshipResults = Collections.emptyList();
    }

    public static ReportDebugResult notFound(int reportId) {
        return new ReportDebugResult(reportId);
    }

    public int getReportId() {
        return reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public String getReportCuid() {
        return reportCuid;
    }

    public String getReportKind() {
        return reportKind;
    }

    public boolean isFound() {
        return found;
    }

    public List<DebugRow> getProperties() {
        return properties;
    }

    public List<RelationshipResult> getRelationshipResults() {
        return relationshipResults;
    }

    public static class DebugRow {
        private final String name;
        private final String value;
        private final String kind;
        private final String cuid;

        public DebugRow(String name, String value, String kind, String cuid) {
            this.name = name;
            this.value = value;
            this.kind = kind;
            this.cuid = cuid;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getKind() {
            return kind;
        }

        public String getCuid() {
            return cuid;
        }
    }

    public static class RelationshipResult {
        private final String relationshipName;
        private final String relationshipFunction;
        private final String query;
        private final List<DebugRow> rows;
        private final String error;

        public RelationshipResult(
                String relationshipName,
                String relationshipFunction,
                String query,
                List<DebugRow> rows,
                String error) {
            this.relationshipName = relationshipName;
            this.relationshipFunction = relationshipFunction;
            this.query = query;
            this.rows = rows;
            this.error = error;
        }

        public String getRelationshipName() {
            return relationshipName;
        }

        public String getRelationshipFunction() {
            return relationshipFunction;
        }

        public String getQuery() {
            return query;
        }

        public List<DebugRow> getRows() {
            return rows;
        }

        public String getError() {
            return error;
        }
    }
}
