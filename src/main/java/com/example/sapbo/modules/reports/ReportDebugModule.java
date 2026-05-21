package com.example.sapbo.modules.reports;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import com.example.sapbo.core.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class ReportDebugModule {
    private static final String[] RELATIONSHIP_NAMES = {
            "Webi-Universe",
            "Webi-DSLUniverse",
            "Unx-To-Webi",
            "Webi-Unx",
            "DSLUniverse-Webi",
            "DSL.MetaDataFile-Webi"
    };

    private final ConnectionManager connectionManager;

    public ReportDebugModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ReportDebugResult inspectReport(int reportId) throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects reports = infoStore.query("SELECT TOP 1 * FROM CI_INFOOBJECTS WHERE SI_ID = " + reportId);

        if (reports.size() == 0) {
            return ReportDebugResult.notFound(reportId);
        }

        IInfoObject report = (IInfoObject) reports.get(0);
        List<ReportDebugResult.DebugRow> properties = new ArrayList<>();
        flattenProperties("", report.properties(), properties, 0);

        List<ReportDebugResult.RelationshipResult> relationshipResults = new ArrayList<>();
        for (String relationshipName : RELATIONSHIP_NAMES) {
            relationshipResults.add(queryRelationship(infoStore, reportId, relationshipName, true));
            relationshipResults.add(queryRelationship(infoStore, reportId, relationshipName, false));
        }

        return new ReportDebugResult(
                reportId,
                report.getTitle(),
                report.getCUID(),
                report.getKind(),
                properties,
                relationshipResults);
    }

    private ReportDebugResult.RelationshipResult queryRelationship(
            IInfoStore infoStore,
            int reportId,
            String relationshipName,
            boolean childRelation) {
        String relationshipFunction = childRelation ? "CHILDREN" : "PARENTS";
        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + relationshipFunction + "(\"SI_NAME='" + relationshipName + "'\", \"SI_ID=" + reportId + "\")";

        try {
            IInfoObjects objects = infoStore.query(query);
            List<ReportDebugResult.DebugRow> rows = new ArrayList<>();
            for (Object object : objects) {
                IInfoObject infoObject = (IInfoObject) object;
                rows.add(new ReportDebugResult.DebugRow(
                        String.valueOf(infoObject.getID()),
                        infoObject.getTitle(),
                        infoObject.getKind(),
                        infoObject.getCUID()));
            }

            return new ReportDebugResult.RelationshipResult(
                    relationshipName,
                    relationshipFunction,
                    query,
                    rows,
                    "");
        } catch (SDKException exception) {
            return new ReportDebugResult.RelationshipResult(
                    relationshipName,
                    relationshipFunction,
                    query,
                    new ArrayList<>(),
                    exception.getMessage());
        }
    }

    private void flattenProperties(
            String prefix,
            IProperties properties,
            List<ReportDebugResult.DebugRow> rows,
            int depth) {
        if (depth > 4) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            IProperty property = properties.getProperty(keyObject);

            if (property != null && property.isContainer()) {
                rows.add(new ReportDebugResult.DebugRow(path, "[container]", "", ""));
                IProperties childProperties = properties.getProperties(keyObject);
                if (childProperties != null) {
                    flattenProperties(path, childProperties, rows, depth + 1);
                }
            } else {
                rows.add(new ReportDebugResult.DebugRow(path, safeString(properties, keyObject), "", ""));
            }
        }
    }

    private String safeString(IProperties properties, Object keyObject) {
        try {
            return properties.getString(String.valueOf(keyObject));
        } catch (RuntimeException exception) {
            return "";
        }
    }
}
