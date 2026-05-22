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
    private static final String USER_GROUPS_PROPERTY = "SI_USERGROUPS";
    private static final Integer[] USER_GROUPS_PROPERTY_IDS = {
            Integer.valueOf(16777256),
            Integer.valueOf(16780918),
            Integer.valueOf(16777726)
    };
    private static final String PROPERTY_TOTAL = "SI_TOTAL";
    private static final String PROPERTY_TOTAL_ID = "16777248";

    private static final String GROUP_QUERY = "SELECT TOP 100000 * "
            + "FROM CI_SYSTEMOBJECTS "
            + "WHERE SI_KIND = 'UserGroup' "
            + "ORDER BY SI_NAME";

    private static final String USER_QUERY = "SELECT TOP 100000 * "
            + "FROM CI_SYSTEMOBJECTS "
            + "WHERE SI_KIND = 'User' "
            + "ORDER BY SI_NAME";

    private static final String[] MEMBER_CONTAINER_NAMES = {
            "SI_USERS",
            "SI_USERGROUPS",
            "SI_MEMBERS",
            "SI_GROUP_MEMBERS",
            "SI_GROUPMEMBERS",
            "SI_SUBGROUPS",
            "SI_CHILDREN"
    };

    private final ConnectionManager connectionManager;

    public UserGroupInventoryModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<UserGroupInventoryRecord> findUsersAndGroups() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        Map<Integer, PrincipalReference> principalsById = queryUsers(infoStore);
        IInfoObjects groupObjects = infoStore.query(GROUP_QUERY);

        if (groupObjects.size() == 0) {
            return Collections.emptyList();
        }

        Map<Integer, GroupReference> groupsById = new HashMap<>();
        List<GroupReference> groups = new ArrayList<>();
        for (Object groupObject : groupObjects) {
            IInfoObject infoObject = (IInfoObject) groupObject;
            GroupReference group = toGroupReference(infoObject);
            groups.add(group);
            groupsById.put(group.id, group);
            if (!principalsById.containsKey(group.id)) {
                principalsById.put(group.id, new PrincipalReference(
                        group.id,
                        group.name,
                        group.cuid,
                        "UserGroup",
                        new LinkedHashSet<Integer>(),
                        ""));
            }
        }

        addMembershipsFromPrincipalGroups(principalsById, groupsById);

        List<UserGroupInventoryRecord> records = new ArrayList<>();
        for (PrincipalReference principal : principalsById.values()) {
            records.addAll(toUserGroupRecords(principal, groupsById));
        }

        return records;
    }

    private Map<Integer, PrincipalReference> queryUsers(IInfoStore infoStore) throws SDKException {
        IInfoObjects objects = infoStore.query(USER_QUERY);
        Map<Integer, PrincipalReference> principalsById = new HashMap<>();

        for (Object object : objects) {
            IInfoObject infoObject = (IInfoObject) object;
            Set<Integer> parentGroupIds = new LinkedHashSet<>();
            collectUserGroupIds(infoObject.properties(), parentGroupIds);
            principalsById.put(infoObject.getID(), new PrincipalReference(
                    infoObject.getID(),
                    getUserDisplayName(infoObject),
                    infoObject.getCUID(),
                    "User",
                    parentGroupIds,
                    getDisabled(infoObject.properties())));
        }

        return principalsById;
    }

    private String getUserDisplayName(IInfoObject infoObject) throws SDKException {
        String fullName = safeGetString(infoObject.properties(), "SI_USERFULLNAME");
        if (fullName.isEmpty() || fullName.equals(infoObject.getTitle())) {
            return infoObject.getTitle();
        }

        return infoObject.getTitle() + " - " + fullName;
    }

    private void addMembershipsFromPrincipalGroups(
            Map<Integer, PrincipalReference> principalsById,
            Map<Integer, GroupReference> groupsById) {
        for (PrincipalReference principal : principalsById.values()) {
            for (Integer parentGroupId : principal.parentGroupIds) {
                GroupReference parentGroup = groupsById.get(parentGroupId);
                if (parentGroup != null && parentGroup.id != principal.id) {
                    parentGroup.memberIds.add(principal.id);
                }
            }
        }
    }

    private GroupReference toGroupReference(IInfoObject infoObject) throws SDKException {
        IProperties properties = infoObject.properties();
        Set<Integer> parentGroupIds = new LinkedHashSet<>();
        collectIds(getProperties(properties, "SI_GROUPS"), parentGroupIds);

        Set<Integer> memberIds = new LinkedHashSet<>();
        for (String containerName : MEMBER_CONTAINER_NAMES) {
            collectIds(getProperties(properties, containerName), memberIds);
        }
        collectUserGroupIds(properties, memberIds);
        collectMemberIdsFromNamedContainers(properties, memberIds, 0);
        memberIds.remove(infoObject.getID());

        return new GroupReference(
                infoObject.getID(),
                infoObject.getTitle(),
                infoObject.getCUID(),
                parentGroupIds,
                memberIds,
                getDisabled(properties));
    }

    private List<UserGroupInventoryRecord> toUserGroupRecords(
            PrincipalReference principal,
            Map<Integer, GroupReference> groupsById) {
        if (principal.parentGroupIds.isEmpty()) {
            return Collections.singletonList(new UserGroupInventoryRecord(
                    principal.getDisplayType(),
                    principal.id,
                    principal.name,
                    principal.cuid,
                    "",
                    "",
                    "",
                    principal.disabled));
        }

        List<UserGroupInventoryRecord> records = new ArrayList<>();
        for (Integer parentGroupId : principal.parentGroupIds) {
            GroupReference parentGroup = groupsById.get(parentGroupId);
            if (parentGroup == null) {
                continue;
            }
            records.add(new UserGroupInventoryRecord(
                    principal.getDisplayType(),
                    principal.id,
                    principal.name,
                    principal.cuid,
                    String.valueOf(parentGroupId),
                    parentGroup.name,
                    buildGroupPath(parentGroup.id, groupsById, new HashSet<Integer>()),
                    principal.disabled));
        }

        if (records.isEmpty()) {
            return Collections.singletonList(new UserGroupInventoryRecord(
                    principal.getDisplayType(),
                    principal.id,
                    principal.name,
                    principal.cuid,
                    "",
                    "",
                    "",
                    principal.disabled));
        }

        return records;
    }

    private String buildGroupPath(
            Integer groupId,
            Map<Integer, GroupReference> groupsById,
            Set<Integer> visitedGroupIds) {
        GroupReference group = groupsById.get(groupId);
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

    private void collectMemberIdsFromNamedContainers(
            IProperties properties,
            Set<Integer> memberIds,
            int depth) {
        if (properties == null || depth > 5) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject).toUpperCase(Locale.ENGLISH);
            IProperty property = properties.getProperty(keyObject);
            if (property == null || !property.isContainer()) {
                continue;
            }

            IProperties childProperties = getProperties(properties, keyObject);
            if (childProperties == null) {
                continue;
            }

            if (isMemberContainerName(key)) {
                collectIds(childProperties, memberIds);
            } else {
                collectMemberIdsFromNamedContainers(childProperties, memberIds, depth + 1);
            }
        }
    }

    private boolean isMemberContainerName(String key) {
        if ("SI_GROUPS".equals(key)) {
            return false;
        }

        return key.contains("MEMBER")
                || key.contains("USER")
                || key.contains("SUBGROUP")
                || key.contains("CHILD");
    }

    private void collectIds(IProperties properties, Set<Integer> ids) {
        if (properties == null) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            if (isTotalProperty(keyObject)) {
                continue;
            }
            IProperty property = properties.getProperty(keyObject);
            if (property != null && property.isContainer()) {
                collectIds(getProperties(properties, keyObject), ids);
                continue;
            }

            addPositiveInteger(ids, safeGetString(properties, keyObject));
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

    private boolean isTotalProperty(Object keyObject) {
        String key = String.valueOf(keyObject);
        return PROPERTY_TOTAL.equalsIgnoreCase(key) || PROPERTY_TOTAL_ID.equals(key);
    }

    private void collectUserGroupIds(IProperties properties, Set<Integer> ids) {
        collectIds(getProperties(properties, USER_GROUPS_PROPERTY), ids);
        for (Integer propertyId : USER_GROUPS_PROPERTY_IDS) {
            collectIds(getProperties(properties, propertyId), ids);
        }
    }

    private String safeGetString(IProperties properties, Object keyObject) {
        try {
            return properties.getString(String.valueOf(keyObject));
        } catch (RuntimeException exception) {
            try {
                return String.valueOf(properties.getInt(String.valueOf(keyObject)));
            } catch (RuntimeException intException) {
                IProperty property = properties.getProperty(keyObject);
                if (property != null && property.getValue() != null) {
                    return String.valueOf(property.getValue()).trim();
                }
                return "";
            }
        }
    }

    private static final class GroupReference {
        private final int id;
        private final String name;
        private final String cuid;
        private final Set<Integer> parentGroupIds;
        private final Set<Integer> memberIds;
        private final String disabled;

        private GroupReference(
                int id,
                String name,
                String cuid,
                Set<Integer> parentGroupIds,
                Set<Integer> memberIds,
                String disabled) {
            this.id = id;
            this.name = name;
            this.cuid = cuid;
            this.parentGroupIds = parentGroupIds;
            this.memberIds = memberIds;
            this.disabled = disabled;
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

        private String getDisplayType() {
            return "usergroup".equals(kind.toLowerCase(Locale.ENGLISH)) ? "Group" : "User";
        }
    }
}
