package com.example.sapbo.modules.folders;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.example.sapbo.core.ConnectionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FolderReplicationModule {
    public static final String SCOPE_ALL = "all";
    public static final String SCOPE_SPECIFIC = "specific";
    public static final String IDENTIFIER_ID = "id";
    public static final String IDENTIFIER_CUID = "cuid";

    private static final String STATUS_READY = "Ready";
    private static final String STATUS_EXISTS = "Exists";
    private static final String STATUS_ERROR = "Error";

    private final ConnectionManager connectionManager;

    public FolderReplicationModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<FolderReplicationRecord> validate(
            String scope,
            String identifierType,
            String identifier,
            String destinationRootName) throws SDKException {
        IInfoStore infoStore = getInfoStore();
        List<FolderReplicationRecord> records = new ArrayList<>();
        if (isBlank(destinationRootName)) {
            records.add(errorRecord("", "", destinationRootName, "Destination root folder name is required."));
            return records;
        }

        FolderIndex folderIndex = loadFolders(infoStore);
        int publicRootId = resolvePublicRootFolderId(infoStore);
        IInfoObject publicRoot = folderIndex.findById(publicRootId);
        if (publicRoot == null) {
            records.add(errorRecord("", "", destinationRootName, "Unable to find the public root folder."));
            return records;
        }

        IInfoObject destinationRoot = folderIndex.findChild(publicRoot.getID(), destinationRootName.trim());
        List<IInfoObject> sourceFolders;
        if (SCOPE_ALL.equals(scope)) {
            sourceFolders = folderIndex.descendants(publicRoot.getID());
        } else {
            IInfoObject sourceFolder = findSourceFolder(infoStore, identifierType, identifier);
            if (sourceFolder == null) {
                records.add(errorRecord(identifier, "", destinationRootName, "Source folder was not found."));
                return records;
            }
            sourceFolders = folderIndex.selfAndDescendants(sourceFolder.getID());
        }

        Map<Integer, String> relativePaths = buildRelativePaths(folderIndex, publicRoot, sourceFolders, scope);
        for (IInfoObject sourceFolder : sourceFolders) {
            if (destinationRoot != null && folderIndex.isSameOrDescendant(sourceFolder.getID(), destinationRoot.getID())) {
                continue;
            }

            String relativePath = relativePaths.get(sourceFolder.getID());
            if (isBlank(relativePath)) {
                continue;
            }

            String destinationPath = destinationRootName.trim() + "/" + relativePath;
            boolean exists = destinationRoot != null && folderIndex.existsPath(destinationRoot.getID(), relativePath);
            records.add(new FolderReplicationRecord(
                    sourceFolder.getID(),
                    sourceFolder.getCUID(),
                    sourceFolder.getTitle(),
                    folderIndex.path(sourceFolder.getID()),
                    destinationRootName.trim(),
                    destinationPath,
                    exists ? STATUS_EXISTS : STATUS_READY,
                    exists ? "Folder already exists in destination." : "Folder will be created if missing."));
        }

        Collections.sort(records, Comparator.comparing(FolderReplicationRecord::getDestinationFolderPath));
        if (records.isEmpty()) {
            records.add(errorRecord(identifier, "", destinationRootName, "No source folders were found to replicate."));
        }

        return records;
    }

    public List<FolderReplicationRecord> replicate(List<FolderReplicationRecord> validatedRecords) throws SDKException {
        IInfoStore infoStore = getInfoStore();
        List<FolderReplicationRecord> results = new ArrayList<>();
        int publicRootId = resolvePublicRootFolderId(infoStore);
        Map<String, Integer> folderCache = new HashMap<>();

        for (FolderReplicationRecord record : validatedRecords) {
            if (STATUS_ERROR.equals(record.getStatus())) {
                results.add(record);
                continue;
            }

            int destinationRootId = ensureChildFolder(infoStore, publicRootId, record.getDestinationRootName());
            ensureFolderHierarchy(
                    infoStore,
                    destinationRootId,
                    removeDestinationRoot(record.getDestinationFolderPath(), record.getDestinationRootName()),
                    folderCache);
            results.add(new FolderReplicationRecord(
                    record.getSourceFolderId(),
                    record.getSourceFolderCuid(),
                    record.getSourceFolderName(),
                    record.getSourceFolderPath(),
                    record.getDestinationRootName(),
                    record.getDestinationFolderPath(),
                    STATUS_EXISTS.equals(record.getStatus()) ? STATUS_EXISTS : "Created",
                    STATUS_EXISTS.equals(record.getStatus())
                            ? "Verified existing destination folder."
                            : "Created or verified destination folder hierarchy."));
        }

        return results;
    }

    private Map<Integer, String> buildRelativePaths(
            FolderIndex folderIndex,
            IInfoObject publicRoot,
            List<IInfoObject> sourceFolders,
            String scope) throws SDKException {
        Map<Integer, String> paths = new HashMap<>();
        for (IInfoObject folder : sourceFolders) {
            String fullPath = folderIndex.path(folder.getID());
            String rootPath = folderIndex.path(publicRoot.getID());
            String relativePath = removePrefix(fullPath, rootPath);
            if (SCOPE_SPECIFIC.equals(scope)) {
                paths.put(folder.getID(), relativePath);
            } else {
                paths.put(folder.getID(), relativePath);
            }
        }

        return paths;
    }

    private IInfoObject findSourceFolder(IInfoStore infoStore, String identifierType, String identifier)
            throws SDKException {
        if (isBlank(identifier)) {
            return null;
        }

        String predicate;
        if (IDENTIFIER_CUID.equals(identifierType)) {
            predicate = "SI_CUID = '" + escapeCmsQueryValue(identifier.trim()) + "'";
        } else if (isPositiveInteger(identifier)) {
            predicate = "SI_ID = " + identifier.trim();
        } else {
            return null;
        }

        IInfoObjects folders = infoStore.query("SELECT TOP 1 * FROM CI_INFOOBJECTS "
                + "WHERE SI_KIND = 'Folder' AND " + predicate);
        if (folders.size() == 0) {
            return null;
        }

        return (IInfoObject) folders.get(0);
    }

    private FolderIndex loadFolders(IInfoStore infoStore) throws SDKException {
        IInfoObjects folders = infoStore.query("SELECT TOP 100000 * FROM CI_INFOOBJECTS WHERE SI_KIND = 'Folder'");
        return new FolderIndex(folders);
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

    private int ensureFolderHierarchy(
            IInfoStore infoStore,
            int destinationRootId,
            String relativePath,
            Map<String, Integer> folderCache) throws SDKException {
        int parentId = destinationRootId;
        String normalizedPath = normalizePath(relativePath);
        if (normalizedPath.isEmpty()) {
            return parentId;
        }

        String cachePath = "";
        for (String part : normalizedPath.split("/")) {
            String folderName = part.trim();
            if (folderName.isEmpty()) {
                continue;
            }

            cachePath = cachePath.isEmpty() ? folderName : cachePath + "/" + folderName;
            String cacheKey = parentId + "|" + cachePath.toLowerCase(Locale.ENGLISH);
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

    private String removeDestinationRoot(String destinationFolderPath, String destinationRootName) {
        String normalizedPath = normalizePath(destinationFolderPath);
        String normalizedRootName = destinationRootName == null ? "" : destinationRootName.trim();
        if (normalizedPath.equals(normalizedRootName)) {
            return "";
        }
        if (normalizedPath.startsWith(normalizedRootName + "/")) {
            return normalizedPath.substring(normalizedRootName.length() + 1);
        }

        return normalizedPath;
    }

    private String removePrefix(String value, String prefix) {
        String normalizedValue = normalizePath(value);
        String normalizedPrefix = normalizePath(prefix);
        if (normalizedValue.equals(normalizedPrefix)) {
            return "";
        }
        if (!normalizedPrefix.isEmpty() && normalizedValue.startsWith(normalizedPrefix + "/")) {
            return normalizedValue.substring(normalizedPrefix.length() + 1);
        }

        return normalizedValue;
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

    private FolderReplicationRecord errorRecord(
            String identifier,
            String sourceFolderPath,
            String destinationRootName,
            String message) {
        return new FolderReplicationRecord(
                0,
                "",
                identifier,
                sourceFolderPath,
                destinationRootName == null ? "" : destinationRootName,
                "",
                STATUS_ERROR,
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

    private boolean isPositiveInteger(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmedValue = value.trim();
        for (int index = 0; index < trimmedValue.length(); index++) {
            if (!Character.isDigit(trimmedValue.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    private static final class FolderIndex {
        private final Map<Integer, IInfoObject> byId = new LinkedHashMap<>();
        private final Map<Integer, List<IInfoObject>> childrenByParentId = new HashMap<>();
        private final Map<Integer, String> pathCache = new HashMap<>();

        private FolderIndex(IInfoObjects folders) {
            for (Object folderObject : folders) {
                IInfoObject folder = (IInfoObject) folderObject;
                byId.put(folder.getID(), folder);
                if (!childrenByParentId.containsKey(folder.getParentID())) {
                    childrenByParentId.put(folder.getParentID(), new ArrayList<IInfoObject>());
                }
                childrenByParentId.get(folder.getParentID()).add(folder);
            }
        }

        private IInfoObject findById(int folderId) {
            return byId.get(folderId);
        }

        private IInfoObject findChild(int parentId, String childName) throws SDKException {
            List<IInfoObject> children = childrenByParentId.get(parentId);
            if (children == null) {
                return null;
            }
            for (IInfoObject child : children) {
                if (child.getTitle().equalsIgnoreCase(childName.trim())) {
                    return child;
                }
            }

            return null;
        }

        private boolean existsPath(int rootId, String relativePath) throws SDKException {
            int parentId = rootId;
            String normalizedPath = relativePath == null ? "" : relativePath.trim().replace("\\", "/");
            if (normalizedPath.isEmpty()) {
                return true;
            }

            for (String part : normalizedPath.split("/")) {
                if (part.trim().isEmpty()) {
                    continue;
                }
                IInfoObject child = findChild(parentId, part);
                if (child == null) {
                    return false;
                }
                parentId = child.getID();
            }

            return true;
        }

        private List<IInfoObject> descendants(int folderId) {
            List<IInfoObject> folders = new ArrayList<>();
            addDescendants(folderId, folders);
            return folders;
        }

        private List<IInfoObject> selfAndDescendants(int folderId) {
            List<IInfoObject> folders = new ArrayList<>();
            IInfoObject folder = byId.get(folderId);
            if (folder != null) {
                folders.add(folder);
                addDescendants(folderId, folders);
            }

            return folders;
        }

        private void addDescendants(int folderId, List<IInfoObject> folders) {
            List<IInfoObject> children = childrenByParentId.get(folderId);
            if (children == null) {
                return;
            }
            Collections.sort(children, Comparator.comparing(folder -> folder.getTitle().toLowerCase(Locale.ENGLISH)));
            for (IInfoObject child : children) {
                folders.add(child);
                addDescendants(child.getID(), folders);
            }
        }

        private String path(int folderId) throws SDKException {
            if (folderId <= 0) {
                return "";
            }
            if (pathCache.containsKey(folderId)) {
                return pathCache.get(folderId);
            }
            IInfoObject folder = byId.get(folderId);
            if (folder == null) {
                pathCache.put(folderId, "");
                return "";
            }

            String parentPath = path(folder.getParentID());
            String folderPath = parentPath.isEmpty() ? folder.getTitle() : parentPath + "/" + folder.getTitle();
            pathCache.put(folderId, folderPath);
            return folderPath;
        }

        private boolean isSameOrDescendant(int folderId, int possibleAncestorId) {
            int currentId = folderId;
            while (currentId > 0) {
                if (currentId == possibleAncestorId) {
                    return true;
                }
                IInfoObject folder = byId.get(currentId);
                if (folder == null) {
                    return false;
                }
                currentId = folder.getParentID();
            }

            return false;
        }
    }
}
