package com.example.sapbo.modules.users;

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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UserGroupInventoryModule {
    private static final String USER_GROUP_QUERY = "SELECT TOP 100000 SI_ID, SI_NAME, SI_CUID, SI_KIND, "
            + "SI_GROUPS, SI_DISABLED "
            + "FROM CI_SYSTEMOBJECTS "
            + "WHERE SI_KIND IN ('User', 'UserGroup') "
            + "ORDER BY SI_KIND, SI_NAME";

    private final ConnectionManager connectionManager;

    public UserGroupInventoryModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<UserGroupInventoryRecord> findUsersAndGroups() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects objects = infoStore.query(USER_GROUP_QUERY);

        if (objects.size() == 0) {
            return Collections.emptyList();
        }

        Map<Integer, PrincipalReference> groupsById = new HashMap<>();
        List<PrincipalReference> principals = new ArrayList<>();

        for (Object object : objects) {
            IInfoObject infoObject = (IInfoObject) object;
            PrincipalReference principal = toPrincipalReference(infoObject);
            principals.add(principal);
            if (principal.isGroup()) {
                groupsById.put(principal.id, principal);
            }
        }

        List<UserGroupInventoryRecord> records = new ArrayList<>();
        for (PrincipalReference principal : principals) {
            records.addAll(toRecords(principal, groupsById));
        }

        return records;
    }

    private PrincipalReference toPrincipalReference(IInfoObject infoObject) throws SDKException {
        IProperties properties = infoObject.properties();
        Set<Integer> parentGroupIds = new LinkedHashSet<>();
        collectGroupIds(getProperties(properties, "SI_GROUPS"), parentGroupIds);

        return new PrincipalReference(
                infoObject.getID(),
                infoObject.getTitle(),
                infoObject.getCUID(),
                infoObject.getKind(),
                parentGroupIds,
                getDisabled(properties));
    }

    private List<UserGroupInventoryRecord> toRecords(
            PrincipalReference principal,
            Map<Integer, PrincipalReference> groupsById) {
        if (principal.parentGroupIds.isEmpty()) {
            return Collections.singletonList(new UserGroupInventoryRecord(
                    principal.getDisplayType(),
                    principal.id,
                    principal.name,
                    principal.cuid,
                    "",
                    "",
                    principal.isGroup() ? principal.name : "",
                    principal.disabled));
        }

        List<UserGroupInventoryRecord> records = new ArrayList<>();
        for (Integer parentGroupId : principal.parentGroupIds) {
            PrincipalReference parentGroup = groupsById.get(parentGroupId);
            String parentGroupName = parentGroup == null ? "" : parentGroup.name;
            records.add(new UserGroupInventoryRecord(
                    principal.getDisplayType(),
                    principal.id,
                    principal.name,
                    principal.cuid,
                    String.valueOf(parentGroupId),
                    parentGroupName,
                    buildGroupPath(parentGroupId, groupsById, new HashSet<Integer>()),
                    principal.disabled));
        }

        return records;
    }

    private String buildGroupPath(
            Integer groupId,
            Map<Integer, PrincipalReference> groupsById,
            Set<Integer> visitedGroupIds) {
        PrincipalReference group = groupsById.get(groupId);
        if (group == null) {
            return String.valueOf(groupId);
        }
        if (!visitedGroupIds.add(groupId)) {
            return group.name;
        }
        if (group.parentGroupIds.isEmpty()) {
            return group.name;
        }

        List<String> paths = new ArrayList<>();
        for (Integer parentGroupId : group.parentGroupIds) {
            paths.add(buildGroupPath(parentGroupId, groupsById, new HashSet<Integer>(visitedGroupIds))
                    + "/" + group.name);
        }

        return String.join(", ", paths);
    }

    private void collectGroupIds(IProperties properties, Set<Integer> groupIds) {
        if (properties == null) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            IProperty property = properties.getProperty(keyObject);
            if (property != null && property.isContainer()) {
                collectGroupIds(getProperties(properties, keyObject), groupIds);
                continue;
            }

            addPositiveInteger(groupIds, safeGetString(properties, keyObject));
        }
    }

    private String getDisabled(IProperties properties) {
        if (!properties.containsKey("SI_DISABLED")) {
            return "";
        }

        return String.valueOf(properties.getBoolean("SI_DISABLED"));
    }

    private void addPositiveInteger(Set<Integer> values, String value) {
        if (isPositiveInteger(value)) {
            values.add(Integer.valueOf(value.trim()));
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
        if (properties == null) {
            return null;
        }
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

    private static final class PrincipalReference {
        private final int id;
        private final String name;
        private final String cuid;
        private final String kind;
        private final Set<Integer> parentGroupIds;
        private final String disabled;

        private PrincipalReference(
                int id,
                String name,
                String cuid,
                String kind,
                Set<Integer> parentGroupIds,
                String disabled) {
            this.id = id;
            this.name = name;
            this.cuid = cuid;
            this.kind = kind;
            this.parentGroupIds = parentGroupIds;
            this.disabled = disabled;
        }

        private boolean isGroup() {
            return "usergroup".equals(kind.toLowerCase(Locale.ENGLISH));
        }

        private String getDisplayType() {
            return isGroup() ? "Group" : "User";
        }
    }
}
