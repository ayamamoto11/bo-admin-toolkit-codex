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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportInventoryModule {
    private static final String REPORT_QUERY = "SELECT SI_NAME, SI_ID, SI_CUID, SI_OWNERID, SI_PARENTID, "
            + "SI_PATH, SI_UNIVERSE, SI_DSL_UNIVERSE "
            + "FROM CI_INFOOBJECTS "
            + "WHERE SI_KIND = 'Webi' "
            + "ORDER BY SI_NAME";

    private final ConnectionManager connectionManager;

    public ReportInventoryModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void printWebIntelligenceReports() throws SDKException {
        List<ReportInventoryRecord> reports = findWebIntelligenceReports();

        if (reports.isEmpty()) {
            System.out.println("No Web Intelligence reports found.");
            return;
        }

        printHeader();
        for (ReportInventoryRecord report : reports) {
            printReport(report);
        }
    }

    public List<ReportInventoryRecord> findWebIntelligenceReports() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects reports = infoStore.query(REPORT_QUERY);

        if (reports.size() == 0) {
            return Collections.emptyList();
        }

        FolderPathResolver folderPathResolver = new FolderPathResolver(infoStore);
        List<ReportInventoryRecord> inventory = new ArrayList<>();

        for (Object reportObject : reports) {
            IInfoObject report = (IInfoObject) reportObject;
            inventory.add(toRecord(report, folderPathResolver));
        }

        return inventory;
    }

    private void printHeader() {
        System.out.printf("%-50s %-10s %-30s %-10s %-10s%n",
                "Report name",
                "Object ID",
                "CUID",
                "Owner ID",
                "Parent ID");
        System.out.println("-".repeat(115));
    }

    private void printReport(ReportInventoryRecord report) {
        System.out.printf("%-50s %-10d %-30s %-10d %-10d%n",
                report.getReportName(),
                report.getObjectId(),
                report.getCuid(),
                report.getOwnerId(),
                report.getParentFolderId());
    }

    private ReportInventoryRecord toRecord(IInfoObject report, FolderPathResolver folderPathResolver)
            throws SDKException {
        IProperties properties = report.properties();
        int parentFolderId = getIntProperty(properties, "SI_PARENTID");
        UniverseReference universeReference = getUniverseReference(properties);

        return new ReportInventoryRecord(
                report.getTitle(),
                report.getID(),
                report.getCUID(),
                getIntProperty(properties, "SI_OWNERID"),
                parentFolderId,
                folderPathResolver.resolve(parentFolderId),
                universeReference.name,
                universeReference.type,
                universeReference.cuid);
    }

    private UniverseReference getUniverseReference(IProperties properties) {
        UniverseReference dslUniverse = readUniverseBag(properties, "SI_DSL_UNIVERSE", "UNX");
        if (!dslUniverse.isEmpty()) {
            return dslUniverse;
        }

        return readUniverseBag(properties, "SI_UNIVERSE", "UNV");
    }

    private UniverseReference readUniverseBag(IProperties properties, String propertyName, String universeType) {
        IProperties universeBag = getProperties(properties, propertyName);
        if (universeBag == null) {
            return UniverseReference.empty();
        }

        Set<String> names = new LinkedHashSet<>();
        Set<String> cuids = new LinkedHashSet<>();

        collectUniverseValues(universeBag, names, cuids);
        return new UniverseReference(
                String.join(", ", names),
                names.isEmpty() ? "" : universeType,
                String.join(", ", cuids));
    }

    private void collectUniverseValues(IProperties properties, Set<String> names, Set<String> cuids) {
        if (properties.containsKey("SI_NAME")) {
            names.add(properties.getString("SI_NAME"));
        }
        if (properties.containsKey("SI_CUID")) {
            cuids.add(properties.getString("SI_CUID"));
        }

        for (Object keyObject : properties.keySet()) {
            IProperty property = properties.getProperty(keyObject);
            if (property != null && property.isContainer()) {
                IProperties childProperties = getProperties(properties, keyObject);
                if (childProperties != null) {
                    collectUniverseValues(childProperties, names, cuids);
                }
            }
        }
    }

    private int getIntProperty(IProperties properties, String propertyName) {
        return properties.getInt(propertyName);
    }

    private IProperties getProperties(IProperties properties, Object propertyName) {
        if (!properties.containsKey(propertyName)) {
            return null;
        }

        return properties.getProperties(propertyName);
    }

    private static final class UniverseReference {
        private final String name;
        private final String type;
        private final String cuid;

        private UniverseReference(String name, String type, String cuid) {
            this.name = name;
            this.type = type;
            this.cuid = cuid;
        }

        private static UniverseReference empty() {
            return new UniverseReference("", "", "");
        }

        private boolean isEmpty() {
            return name.isEmpty() && cuid.isEmpty();
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
                    + "FROM CI_INFOOBJECTS WHERE SI_ID = " + folderId);
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
