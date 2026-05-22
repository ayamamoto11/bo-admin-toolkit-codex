package com.example.sapbo.modules.reports;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import com.example.sapbo.core.ConnectionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportInventoryModule {
    private static final String SI_DSL_UNIVERSE_PROPERTY_ID = "16777350";

    private static final String REPORT_QUERY = "SELECT TOP 100000 SI_NAME, SI_ID, SI_CUID, SI_OWNERID, SI_PARENTID, "
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
            inventory.addAll(toRecords(report, folderPathResolver));
        }

        return inventory;
    }

    private void printHeader() {
        System.out.printf("%-50s %-10s %-30s %-10s %-10s %-15s%n",
                "Report name",
                "Object ID",
                "CUID",
                "Owner ID",
                "Parent ID",
                "Universe ID");
        System.out.println(repeat("-", 131));
    }

    private void printReport(ReportInventoryRecord report) {
        System.out.printf("%-50s %-10d %-30s %-10d %-10d %-15s%n",
                report.getReportName(),
                report.getObjectId(),
                report.getCuid(),
                report.getOwnerId(),
                report.getParentFolderId(),
                report.getUniverseId());
    }

    private List<ReportInventoryRecord> toRecords(IInfoObject report, FolderPathResolver folderPathResolver)
            throws SDKException {
        IProperties properties = report.properties();
        int parentFolderId = getIntProperty(properties, "SI_PARENTID");
        List<UniverseReference> universeReferences = getUniverseReferences(report, folderPathResolver);
        List<ReportInventoryRecord> records = new ArrayList<>();

        for (UniverseReference universeReference : universeReferences) {
            records.add(new ReportInventoryRecord(
                report.getTitle(),
                report.getID(),
                report.getCUID(),
                getIntProperty(properties, "SI_OWNERID"),
                parentFolderId,
                folderPathResolver.resolve(parentFolderId),
                universeReference.getDataProviderType(),
                universeReference.dataProviderName,
                universeReference.id,
                universeReference.name,
                universeReference.type,
                universeReference.cuid,
                universeReference.path));
        }

        return records;
    }

    private List<UniverseReference> getUniverseReferences(IInfoObject report, FolderPathResolver folderPathResolver)
            throws SDKException {
        UniverseReference relationshipUniverse = readUniverseRelationships(report, folderPathResolver);
        if (!relationshipUniverse.isEmpty()) {
            return Collections.singletonList(relationshipUniverse);
        }

        IProperties properties = report.properties();
        List<UniverseReference> dslUniverses = readUniverseBag(
                properties,
                "SI_DSL_UNIVERSE",
                "UNX",
                folderPathResolver);
        if (!dslUniverses.isEmpty()) {
            return dslUniverses;
        }

        List<UniverseReference> universes = readUniverseBag(
                properties,
                "SI_UNIVERSE",
                "UNV",
                folderPathResolver);
        if (!universes.isEmpty()) {
            return universes;
        }

        List<UniverseReference> webiDocPropertiesUniverses =
                readWebiDocPropertiesUniverses(properties, folderPathResolver);
        if (!webiDocPropertiesUniverses.isEmpty()) {
            return webiDocPropertiesUniverses;
        }

        return Collections.singletonList(UniverseReference.freehandSqlOrOther());
    }

    private List<UniverseReference> readWebiDocPropertiesUniverses(
            IProperties properties,
            FolderPathResolver folderPathResolver) throws SDKException {
        List<WebiDataProvider> dataProviders = getWebiDocDataProviders(properties);
        if (dataProviders.isEmpty()) {
            return Collections.emptyList();
        }

        List<UniverseReference> references = new ArrayList<>();

        for (WebiDataProvider dataProvider : dataProviders) {
            UniverseReference reference = queryUniverseByName(dataProvider.dataSourceName, folderPathResolver);
            if (reference.isEmpty()) {
                continue;
            }
            references.add(reference.withDataProviderName(dataProvider.dataProviderName));
        }

        return references;
    }

    private List<WebiDataProvider> getWebiDocDataProviders(IProperties properties) {
        List<WebiDataProvider> dataProviders = new ArrayList<>();

        for (Object keyObject : properties.keySet()) {
            String propertyValue = safeGetString(properties, keyObject);
            if (!propertyValue.contains("<WEBI_DOC_PROPS")) {
                continue;
            }

            dataProviders.addAll(parseWebiDocDataProviders(propertyValue));
        }

        return dataProviders;
    }

    private List<WebiDataProvider> parseWebiDocDataProviders(String xml) {
        List<WebiDataProvider> dataProviders = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList dataProviderNodes = document.getElementsByTagName("WEBI_DP");

            for (int index = 0; index < dataProviderNodes.getLength(); index++) {
                Element dataProvider = (Element) dataProviderNodes.item(index);
                String dataSourceName = dataProvider.getAttribute("DSNAME");
                if (dataSourceName != null && !dataSourceName.trim().isEmpty()) {
                    String dataProviderName = dataProvider.getAttribute("DPNAME");
                    dataProviders.add(new WebiDataProvider(
                            dataProviderName == null ? "" : dataProviderName.trim(),
                            dataSourceName.trim()));
                }
            }
        } catch (Exception exception) {
            return Collections.emptyList();
        }

        return dataProviders;
    }

    private UniverseReference queryUniverseByName(String universeName, FolderPathResolver folderPathResolver)
            throws SDKException {
        String escapedUniverseName = universeName.replace("'", "''");
        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE SI_NAME = '" + escapedUniverseName + "' "
                + "AND SI_KIND != 'Webi'";
        IInfoObjects universes = folderPathResolver.infoStore.query(query);

        if (universes.size() == 0) {
            return UniverseReference.empty();
        }

        Set<String> ids = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        Set<String> cuids = new LinkedHashSet<>();
        Set<String> types = new LinkedHashSet<>();
        Set<String> paths = new LinkedHashSet<>();

        for (Object universeObject : universes) {
            IInfoObject universe = (IInfoObject) universeObject;
            ids.add(String.valueOf(universe.getID()));
            names.add(universe.getTitle());
            cuids.add(universe.getCUID());
            types.add(getUniverseType(universe, ""));
            paths.add(folderPathResolver.resolve(universe.properties().getInt("SI_PARENTID")));
        }

        return new UniverseReference(
                "",
                String.join(", ", ids),
                String.join(", ", names),
                String.join(", ", types),
                String.join(", ", cuids),
                String.join(", ", paths));
    }

    private UniverseReference readUniverseRelationships(IInfoObject report, FolderPathResolver folderPathResolver)
            throws SDKException {
        UniverseReference universeReference =
                safeQueryRelatedUniverses(report.getID(), folderPathResolver, "Webi-Universe", "UNV", true);
        UniverseReference dslUniverseReference =
                UniverseReference.merge(
                        safeQueryRelatedUniverses(report.getID(), folderPathResolver, "Webi-DSLUniverse", "UNX", true),
                        safeQueryRelatedUniverses(report.getID(), folderPathResolver, "Unx-To-Webi", "UNX", false));

        return UniverseReference.merge(universeReference, dslUniverseReference);
    }

    private UniverseReference safeQueryRelatedUniverses(
            int reportId,
            FolderPathResolver folderPathResolver,
            String relationName,
            String universeType,
            boolean childRelation) throws SDKException {
        try {
            return queryRelatedUniverses(reportId, folderPathResolver, relationName, universeType, childRelation);
        } catch (SDKException exception) {
            return UniverseReference.empty();
        }
    }

    private UniverseReference queryRelatedUniverses(
            int reportId,
            FolderPathResolver folderPathResolver,
            String relationName,
            String universeType,
            boolean childRelation) throws SDKException {
        String relationshipFunction = childRelation ? "CHILDREN" : "PARENTS";
        String query = "SELECT TOP 100000 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + relationshipFunction + "(\"SI_NAME='" + relationName + "'\", \"SI_ID=" + reportId + "\")";
        IInfoObjects universes = folderPathResolver.infoStore.query(query);

        if (universes.size() == 0) {
            return UniverseReference.empty();
        }

        Set<String> ids = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        Set<String> cuids = new LinkedHashSet<>();
        Set<String> types = new LinkedHashSet<>();
        Set<String> paths = new LinkedHashSet<>();

        for (Object universeObject : universes) {
            IInfoObject universe = (IInfoObject) universeObject;
            ids.add(String.valueOf(universe.getID()));
            names.add(universe.getTitle());
            cuids.add(universe.getCUID());
            types.add(getUniverseType(universe, universeType));
            paths.add(folderPathResolver.resolve(universe.properties().getInt("SI_PARENTID")));
        }

        return new UniverseReference(
                "",
                String.join(", ", ids),
                String.join(", ", names),
                String.join(", ", types),
                String.join(", ", cuids),
                String.join(", ", paths));
    }

    private String getUniverseType(IInfoObject universe, String defaultType) throws SDKException {
        String kind = universe.getKind();
        if (kind != null && kind.equalsIgnoreCase("DSL.MetaDataFile")) {
            return "UNX";
        }
        if (kind != null && kind.toLowerCase().contains("dsl")) {
            return "UNX";
        }
        if (kind != null && kind.equalsIgnoreCase("Universe")) {
            return "UNV";
        }

        return defaultType;
    }

    private List<UniverseReference> readUniverseBag(
            IProperties properties,
            String propertyName,
            String universeType,
            FolderPathResolver folderPathResolver) throws SDKException {
        IProperties universeBag = getUniverseBagProperties(properties, propertyName);
        if (universeBag == null) {
            return Collections.emptyList();
        }

        Set<String> ids = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        Set<String> cuids = new LinkedHashSet<>();

        collectUniverseValues(universeBag, ids, names, cuids);
        if (ids.isEmpty() && names.isEmpty() && cuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<UniverseReference> universesById = queryUniverseByIds(ids, universeType, folderPathResolver);
        if (!universesById.isEmpty()) {
            return addGeneratedDataProviderNames(universesById);
        }

        return Collections.singletonList(new UniverseReference(
                "",
                String.join(", ", ids),
                String.join(", ", names),
                names.isEmpty() ? "" : universeType,
                String.join(", ", cuids),
                ""));
    }

    private IProperties getUniverseBagProperties(IProperties properties, String propertyName) {
        IProperties universeBag = getProperties(properties, propertyName);
        if (universeBag != null) {
            return universeBag;
        }

        if ("SI_DSL_UNIVERSE".equals(propertyName)) {
            return getProperties(properties, SI_DSL_UNIVERSE_PROPERTY_ID);
        }

        return null;
    }

    private void collectUniverseValues(
            IProperties properties,
            Set<String> ids,
            Set<String> names,
            Set<String> cuids) {
        if (properties.containsKey("SI_ID")) {
            ids.add(String.valueOf(properties.getInt("SI_ID")));
        }
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
                    collectUniverseValues(childProperties, ids, names, cuids);
                }
            } else {
                addPositiveInteger(ids, safeGetString(properties, keyObject));
            }
        }
    }

    private List<UniverseReference> queryUniverseByIds(
            Set<String> universeIds,
            String defaultType,
            FolderPathResolver folderPathResolver) throws SDKException {
        Set<String> numericUniverseIds = new LinkedHashSet<>();
        for (String universeId : universeIds) {
            if (isPositiveInteger(universeId)) {
                numericUniverseIds.add(universeId);
            }
        }

        if (numericUniverseIds.isEmpty()) {
            return Collections.emptyList();
        }

        String query = "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_APPOBJECTS "
                + "WHERE SI_KIND IN ('Universe', 'DSL.Universe', 'DSL.MetaDataFile') "
                + "AND (" + buildIdPredicate(numericUniverseIds) + ")";
        IInfoObjects universes = folderPathResolver.infoStore.query(query);

        if (universes.size() == 0) {
            return Collections.emptyList();
        }

        List<UniverseReference> references = new ArrayList<>();

        for (Object universeObject : universes) {
            IInfoObject universe = (IInfoObject) universeObject;
            references.add(new UniverseReference(
                    "",
                    String.valueOf(universe.getID()),
                    universe.getTitle(),
                    getUniverseType(universe, defaultType),
                    universe.getCUID(),
                    folderPathResolver.resolve(universe.properties().getInt("SI_PARENTID"))));
        }

        return references;
    }

    private List<UniverseReference> addGeneratedDataProviderNames(List<UniverseReference> references) {
        if (references.size() <= 1) {
            return references;
        }

        List<UniverseReference> namedReferences = new ArrayList<>();
        for (int index = 0; index < references.size(); index++) {
            namedReferences.add(references.get(index).withDataProviderName("Data Provider " + (index + 1)));
        }

        return namedReferences;
    }

    private String buildIdPredicate(Set<String> universeIds) {
        List<String> predicates = new ArrayList<>();
        for (String universeId : universeIds) {
            predicates.add("SI_ID = " + universeId);
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

    private int getIntProperty(IProperties properties, String propertyName) {
        return properties.getInt(propertyName);
    }

    private String repeat(String value, int count) {
        StringBuilder repeatedValue = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            repeatedValue.append(value);
        }

        return repeatedValue.toString();
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

    private static void addCsvValues(Set<String> values, String csvValues) {
        if (csvValues == null || csvValues.trim().isEmpty()) {
            return;
        }

        String[] splitValues = csvValues.split(",");
        for (String splitValue : splitValues) {
            String value = splitValue.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
    }

    private static final class UniverseReference {
        private final String dataProviderName;
        private final String id;
        private final String name;
        private final String type;
        private final String cuid;
        private final String path;

        private UniverseReference(String dataProviderName, String id, String name, String type, String cuid, String path) {
            this.dataProviderName = dataProviderName;
            this.id = id;
            this.name = name;
            this.type = type;
            this.cuid = cuid;
            this.path = path;
        }

        private static UniverseReference empty() {
            return new UniverseReference("", "", "", "", "", "");
        }

        private static UniverseReference freehandSqlOrOther() {
            return new UniverseReference("", "", "", "", "", "");
        }

        private UniverseReference withDataProviderName(String dataProviderName) {
            return new UniverseReference(dataProviderName, id, name, type, cuid, path);
        }

        private static UniverseReference merge(UniverseReference first, UniverseReference second) {
            if (first.isEmpty()) {
                return second;
            }
            if (second.isEmpty()) {
                return first;
            }

            return new UniverseReference(
                    joinValues(first.dataProviderName, second.dataProviderName),
                    joinValues(first.id, second.id),
                    joinValues(first.name, second.name),
                    joinValues(first.type, second.type),
                    joinValues(first.cuid, second.cuid),
                    joinValues(first.path, second.path));
        }

        private static String joinValues(String first, String second) {
            Set<String> values = new LinkedHashSet<>();
            addValues(values, first);
            addValues(values, second);
            return String.join(", ", values);
        }

        private static void addValues(Set<String> values, String csvValues) {
            addCsvValues(values, csvValues);
        }

        private boolean isEmpty() {
            return id.isEmpty() && name.isEmpty() && cuid.isEmpty();
        }

        private String getDataProviderType() {
            return isEmpty() ? "Freehand SQL / Other" : "Universe";
        }
    }

    private static final class WebiDataProvider {
        private final String dataProviderName;
        private final String dataSourceName;

        private WebiDataProvider(String dataProviderName, String dataSourceName) {
            this.dataProviderName = dataProviderName;
            this.dataSourceName = dataSourceName;
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
