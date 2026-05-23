package com.example.sapbo.modules.reports;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.example.sapbo.core.ConnectionManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportArchiveModule {
    private static final String STATUS_READY = "Ready";
    private static final String STATUS_ERROR = "Error";
    private static final String STATUS_SKIPPED = "Skipped";

    private final ConnectionManager connectionManager;

    public ReportArchiveModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<ReportArchiveRecord> validateWorkbook(InputStream inputStream, String archiveFolderName)
            throws IOException, SDKException {
        IInfoStore infoStore = getInfoStore();
        List<ReportArchiveRecord> records = new ArrayList<>();

        if (isBlank(archiveFolderName)) {
            records.add(errorRecord(1, "", "", "", "", "Archive folder name is required."));
            return records;
        }

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return records;
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = readHeaderColumns(sheet.getRow(sheet.getFirstRowNum()), formatter);
            Integer cuidIndex = columns.get("cuid");
            Integer reportNameIndex = columns.get("reportname");
            Integer folderPathIndex = columns.get("folderpath");

            if (cuidIndex == null) {
                records.add(errorRecord(1, "", "", "", archiveFolderName, "Missing required CUID column."));
                return records;
            }

            Map<String, Integer> pendingNamesByTarget = new HashMap<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String cuid = readCell(row, cuidIndex, formatter);
                String reportName = readOptionalCell(row, reportNameIndex, formatter);
                String folderPath = readOptionalCell(row, folderPathIndex, formatter);
                if (cuid.isEmpty() && reportName.isEmpty() && folderPath.isEmpty()) {
                    continue;
                }

                records.add(validateReport(
                        infoStore,
                        rowIndex + 1,
                        cuid,
                        reportName,
                        folderPath,
                        archiveFolderName,
                        pendingNamesByTarget));
            }
        }

        return records;
    }

    public List<ReportArchiveRecord> validateKeyword(String keyword, String archiveFolderName) throws SDKException {
        IInfoStore infoStore = getInfoStore();
        List<ReportArchiveRecord> records = new ArrayList<>();
        if (isBlank(keyword)) {
            records.add(errorRecord(1, "", "", "", archiveFolderName, "Keyword is required."));
            return records;
        }
        if (isBlank(archiveFolderName)) {
            records.add(errorRecord(1, "", "", "", "", "Archive folder name is required."));
            return records;
        }

        String query = "SELECT TOP 100000 * FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Webi' AND SI_INSTANCE = 0 "
                + "AND SI_KEYWORD LIKE '%" + escapeCmsQueryValue(keyword.trim()) + "%' "
                + "ORDER BY SI_NAME";
        IInfoObjects reports = infoStore.query(query);
        Map<String, Integer> pendingNamesByTarget = new HashMap<>();

        for (int index = 0; index < reports.size(); index++) {
            IInfoObject report = (IInfoObject) reports.get(index);
            records.add(toReadyRecord(
                    infoStore,
                    index + 1,
                    report,
                    report.getTitle(),
                    "",
                    archiveFolderName,
                    pendingNamesByTarget));
        }

        if (records.isEmpty()) {
            records.add(errorRecord(1, "", "", "", archiveFolderName, "No WebI reports found with keyword."));
        }

        return records;
    }

    public List<ReportArchiveRecord> archiveReports(List<ReportArchiveRecord> validatedRecords) throws SDKException {
        IInfoStore infoStore = getInfoStore();
        List<ReportArchiveRecord> results = new ArrayList<>();
        Map<String, Integer> renamedNamesByTarget = new HashMap<>();
        int rootFolderId = resolvePublicRootFolderId(infoStore);
        Map<String, Integer> folderCache = new HashMap<>();

        for (ReportArchiveRecord record : validatedRecords) {
            if (!record.isReady()) {
                results.add(record);
                continue;
            }

            IInfoObject report = queryReportByCuid(infoStore, record.getCuid());
            if (report == null) {
                results.add(copyWithStatus(record, STATUS_ERROR, "Report was not found at archive time."));
                continue;
            }

            int archiveRootId = ensureChildFolder(infoStore, rootFolderId, record.getArchiveFolderName());
            int targetFolderId = ensureFolderHierarchy(
                    infoStore,
                    archiveRootId,
                    removeArchiveRoot(record.getTargetFolderPath(), record.getArchiveFolderName()),
                    folderCache);
            String finalName = uniqueReportName(
                    infoStore,
                    targetFolderId,
                    report.getTitle(),
                    report.getCUID(),
                    renamedNamesByTarget);

            IInfoObjects changedReports = infoStore.newInfoObjectCollection();
            report.setParentID(targetFolderId);
            if (!finalName.equals(report.getTitle())) {
                report.setTitle(finalName);
            }
            changedReports.add(report);
            infoStore.commit(changedReports);

            results.add(new ReportArchiveRecord(
                    record.getRowNumber(),
                    record.getCuid(),
                    report.getTitle(),
                    record.getCurrentFolderPath(),
                    record.getArchiveFolderName(),
                    record.getTargetFolderPath(),
                    finalName,
                    "Archived",
                    "Report moved to archive folder."));
        }

        return results;
    }

    private ReportArchiveRecord validateReport(
            IInfoStore infoStore,
            int rowNumber,
            String cuid,
            String reportName,
            String folderPath,
            String archiveFolderName,
            Map<String, Integer> pendingNamesByTarget) throws SDKException {
        if (isBlank(cuid)) {
            return errorRecord(rowNumber, "", reportName, folderPath, archiveFolderName, "Missing CUID.");
        }

        IInfoObject report = queryReportByCuid(infoStore, cuid);
        if (report == null) {
            return errorRecord(rowNumber, cuid, reportName, folderPath, archiveFolderName, "Report was not found or is not a WebI report.");
        }

        return toReadyRecord(infoStore, rowNumber, report, reportName, folderPath, archiveFolderName, pendingNamesByTarget);
    }

    private ReportArchiveRecord toReadyRecord(
            IInfoStore infoStore,
            int rowNumber,
            IInfoObject report,
            String reportName,
            String folderPath,
            String archiveFolderName,
            Map<String, Integer> pendingNamesByTarget) throws SDKException {
        String currentFolderPath = resolveFolderPath(infoStore, report.getParentID());
        String relativePath = isBlank(folderPath) ? currentFolderPath : normalizePath(folderPath);
        String targetFolderPath = archiveFolderName.trim() + (relativePath.isEmpty() ? "" : "/" + relativePath);
        String displayReportName = isBlank(reportName) ? report.getTitle() : reportName;
        String finalName = pendingUniqueReportName(targetFolderPath, report.getTitle(), pendingNamesByTarget);

        return new ReportArchiveRecord(
                rowNumber,
                report.getCUID(),
                displayReportName,
                currentFolderPath,
                archiveFolderName.trim(),
                targetFolderPath,
                finalName,
                STATUS_READY,
                "Valid WebI report.");
    }

    private IInfoObject queryReportByCuid(IInfoStore infoStore, String cuid) throws SDKException {
        IInfoObjects reports = infoStore.query("SELECT TOP 1 * FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Webi' AND SI_CUID = '" + escapeCmsQueryValue(cuid) + "'");
        if (reports.size() == 0) {
            return null;
        }

        return (IInfoObject) reports.get(0);
    }

    private int ensureFolderHierarchy(
            IInfoStore infoStore,
            int archiveRootId,
            String relativePath,
            Map<String, Integer> folderCache) throws SDKException {
        int parentId = archiveRootId;
        String normalizedPath = normalizePath(relativePath);
        if (normalizedPath.isEmpty()) {
            return parentId;
        }

        String[] parts = normalizedPath.split("/");
        String cachePath = "";
        for (String part : parts) {
            String folderName = part.trim();
            if (folderName.isEmpty()) {
                continue;
            }

            cachePath = cachePath.isEmpty() ? folderName : cachePath + "/" + folderName;
            String cacheKey = parentId + "|" + cachePath;
            if (folderCache.containsKey(cacheKey)) {
                parentId = folderCache.get(cacheKey);
                continue;
            }

            parentId = ensureChildFolder(infoStore, parentId, folderName);
            folderCache.put(cacheKey, parentId);
        }

        return parentId;
    }

    private int ensureChildFolder(IInfoStore infoStore, int parentId, String folderName) throws SDKException {
        IInfoObject existingFolder = findChildFolder(infoStore, parentId, folderName);
        if (existingFolder != null) {
            return existingFolder.getID();
        }

        IInfoObjects newFolders = infoStore.newInfoObjectCollection();
        IInfoObject folder = newFolders.add("CrystalEnterprise.Folder");
        folder.setTitle(folderName);
        folder.setParentID(parentId);
        infoStore.commit(newFolders);
        return folder.getID();
    }

    private IInfoObject findChildFolder(IInfoStore infoStore, int parentId, String folderName) throws SDKException {
        IInfoObjects folders = infoStore.query("SELECT TOP 1 SI_ID, SI_NAME, SI_PARENTID FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Folder' AND SI_PARENTID = " + parentId
                + " AND SI_NAME = '" + escapeCmsQueryValue(folderName) + "'");
        if (folders.size() == 0) {
            return null;
        }

        return (IInfoObject) folders.get(0);
    }

    private String uniqueReportName(
            IInfoStore infoStore,
            int parentId,
            String baseName,
            String currentCuid,
            Map<String, Integer> renamedNamesByTarget) throws SDKException {
        String targetKey = parentId + "|" + baseName.toLowerCase(Locale.ENGLISH);
        int repeat = renamedNamesByTarget.containsKey(targetKey) ? renamedNamesByTarget.get(targetKey) : 0;
        String candidate = repeat == 0 ? baseName : baseName + " (repeat" + repeat + ")";

        while (reportNameExists(infoStore, parentId, candidate, currentCuid)) {
            repeat++;
            candidate = baseName + " (repeat" + repeat + ")";
        }

        renamedNamesByTarget.put(targetKey, repeat + 1);
        return candidate;
    }

    private boolean reportNameExists(IInfoStore infoStore, int parentId, String reportName, String currentCuid)
            throws SDKException {
        IInfoObjects reports = infoStore.query("SELECT TOP 1 SI_ID, SI_CUID, SI_NAME FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Webi' AND SI_PARENTID = " + parentId
                + " AND SI_NAME = '" + escapeCmsQueryValue(reportName) + "'");
        if (reports.size() == 0) {
            return false;
        }

        IInfoObject existingReport = (IInfoObject) reports.get(0);
        return !currentCuid.equals(existingReport.getCUID());
    }

    private String pendingUniqueReportName(
            String targetFolderPath,
            String baseName,
            Map<String, Integer> pendingNamesByTarget) {
        String targetKey = targetFolderPath.toLowerCase(Locale.ENGLISH) + "|" + baseName.toLowerCase(Locale.ENGLISH);
        int repeat = pendingNamesByTarget.containsKey(targetKey) ? pendingNamesByTarget.get(targetKey) : 0;
        pendingNamesByTarget.put(targetKey, repeat + 1);
        return repeat == 0 ? baseName : baseName + " (repeat" + repeat + ")";
    }

    private int resolvePublicRootFolderId(IInfoStore infoStore) throws SDKException {
        int pluginRootId = infoStore.getPluginMgr().getPluginInfo("CrystalEnterprise.Folder").getRootFolderID();
        if (pluginRootId > 0) {
            return pluginRootId;
        }

        IInfoObjects roots = infoStore.query("SELECT TOP 10 SI_ID, SI_NAME, SI_PARENTID FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Folder' AND SI_PARENTID = 0");
        for (Object rootObject : roots) {
            IInfoObject root = (IInfoObject) rootObject;
            if ("Root Folder".equalsIgnoreCase(root.getTitle())
                    || "Public Folders".equalsIgnoreCase(root.getTitle())) {
                return root.getID();
            }
        }

        throw new IllegalStateException("Unable to find the Public Folders root. Folder creation was stopped to avoid using Favorites.");
    }

    private String resolveFolderPath(IInfoStore infoStore, int folderId) throws SDKException {
        return resolveFolderPath(infoStore, folderId, new LinkedHashMap<Integer, String>());
    }

    private String resolveFolderPath(IInfoStore infoStore, int folderId, Map<Integer, String> pathCache)
            throws SDKException {
        if (folderId <= 0) {
            return "";
        }
        if (pathCache.containsKey(folderId)) {
            return pathCache.get(folderId);
        }

        IInfoObjects folders = infoStore.query("SELECT TOP 1 SI_ID, SI_NAME, SI_PARENTID "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS WHERE SI_ID = " + folderId);
        if (folders.size() == 0) {
            pathCache.put(folderId, "");
            return "";
        }

        IInfoObject folder = (IInfoObject) folders.get(0);
        int parentId = folder.getParentID();
        String parentPath = resolveFolderPath(infoStore, parentId, pathCache);
        String folderName = folder.getTitle();
        String folderPath = parentPath.isEmpty() ? folderName : parentPath + "/" + folderName;
        pathCache.put(folderId, folderPath);
        return folderPath;
    }

    private Map<String, Integer> readHeaderColumns(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        if (headerRow == null) {
            return columns;
        }

        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            if (!header.isEmpty()) {
                columns.put(header, cell.getColumnIndex());
            }
        }

        return columns;
    }

    private String normalizeHeader(String header) {
        String cleanHeader = header == null ? "" : header.trim().toLowerCase(Locale.ENGLISH);
        cleanHeader = cleanHeader.replace(" ", "").replace("_", "");
        if ("sicuid".equals(cleanHeader) || "reportcuid".equals(cleanHeader) || "cuid".equals(cleanHeader)) {
            return "cuid";
        }
        if ("reportname".equals(cleanHeader) || "siname".equals(cleanHeader) || "name".equals(cleanHeader)) {
            return "reportname";
        }
        if ("foldername".equals(cleanHeader) || "folderpath".equals(cleanHeader) || "path".equals(cleanHeader)) {
            return "folderpath";
        }

        return cleanHeader;
    }

    private String readOptionalCell(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return "";
        }

        return readCell(row, columnIndex, formatter);
    }

    private String readCell(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private String removeArchiveRoot(String targetFolderPath, String archiveFolderName) {
        String normalizedPath = normalizePath(targetFolderPath);
        String normalizedArchiveName = archiveFolderName == null ? "" : archiveFolderName.trim();
        if (normalizedPath.equals(normalizedArchiveName)) {
            return "";
        }
        if (normalizedPath.startsWith(normalizedArchiveName + "/")) {
            return normalizedPath.substring(normalizedArchiveName.length() + 1);
        }

        return normalizedPath;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalizedPath = path.trim().replace("\\", "/");
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }

    private ReportArchiveRecord errorRecord(
            int rowNumber,
            String cuid,
            String reportName,
            String folderPath,
            String archiveFolderName,
            String message) {
        return new ReportArchiveRecord(
                rowNumber,
                cuid,
                reportName,
                folderPath,
                archiveFolderName,
                "",
                "",
                STATUS_ERROR,
                message);
    }

    private ReportArchiveRecord copyWithStatus(ReportArchiveRecord record, String status, String message) {
        return new ReportArchiveRecord(
                record.getRowNumber(),
                record.getCuid(),
                record.getReportName(),
                record.getCurrentFolderPath(),
                record.getArchiveFolderName(),
                record.getTargetFolderPath(),
                record.getFinalReportName(),
                status,
                message);
    }

    private IInfoStore getInfoStore() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        return (IInfoStore) session.getService("InfoStore");
    }

    private String escapeCmsQueryValue(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
