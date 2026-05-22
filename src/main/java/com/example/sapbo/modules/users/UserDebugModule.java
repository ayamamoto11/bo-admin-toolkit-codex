package com.example.sapbo.modules.users;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;
import com.example.sapbo.core.ConnectionManager;
import com.example.sapbo.core.DebugResult;
import com.example.sapbo.core.InfoObjectDebugSupport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UserDebugModule {
    private static final String USER_GROUPS_PROPERTY = "SI_USERGROUPS";
    private static final Integer USER_GROUPS_PROPERTY_ID = Integer.valueOf(16780918);

    private final ConnectionManager connectionManager;

    public UserDebugModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public DebugResult inspectUser(int userId) throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects users = infoStore.query("SELECT TOP 1 * FROM CI_SYSTEMOBJECTS WHERE SI_ID = " + userId);

        if (users.size() == 0) {
            return DebugResult.notFound(userId);
        }

        IInfoObject user = (IInfoObject) users.get(0);
        List<DebugResult.Row> properties = new ArrayList<>();
        InfoObjectDebugSupport.flattenProperties("", user.properties(), properties, 0);

        List<DebugResult.QueryResult> queryResults = new ArrayList<>();
        queryResults.add(InfoObjectDebugSupport.queryObjects(
                infoStore,
                "Exact user group query",
                "SELECT TOP 10 SI_ID, SI_NAME, SI_USERFULLNAME, SI_USERGROUPS "
                        + "FROM CI_SYSTEMOBJECTS "
                        + "WHERE SI_KIND = 'User' AND SI_ID = " + userId));
        queryResults.add(InfoObjectDebugSupport.queryObjects(
                infoStore,
                "Group ID lookup from all properties",
                buildGroupLookupQuery(user.properties())));

        return new DebugResult(
                userId,
                user.getTitle(),
                user.getCUID(),
                user.getKind(),
                properties,
                queryResults);
    }

    private String buildGroupLookupQuery(IProperties userProperties) {
        Set<String> groupIds = new LinkedHashSet<>();
        collectIds(getUserGroupsProperties(userProperties), groupIds);

        List<String> predicates = new ArrayList<>();
        for (String groupId : groupIds) {
            predicates.add("SI_ID = " + groupId);
        }

        String whereClause = predicates.isEmpty() ? "SI_ID = -1" : String.join(" OR ", predicates);
        return "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_SYSTEMOBJECTS "
                + "WHERE " + whereClause;
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

            String value = InfoObjectDebugSupport.safeString(properties, keyObject);
            if (isPositiveInteger(value)) {
                ids.add(value.trim());
            }
        }
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

    private IProperties getUserGroupsProperties(IProperties properties) {
        IProperties userGroups = getProperties(properties, USER_GROUPS_PROPERTY);
        if (userGroups != null) {
            return userGroups;
        }

        return getProperties(properties, USER_GROUPS_PROPERTY_ID);
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
}
