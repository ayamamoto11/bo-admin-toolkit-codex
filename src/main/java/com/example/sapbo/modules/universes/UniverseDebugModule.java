package com.example.sapbo.modules.universes;

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

public class UniverseDebugModule {
    private static final String SL_UNIVERSE_CONNECTIONS_PROPERTY = "SI_SL_UNIVERSE_TO_CONNECTIONS";
    private static final Integer[] SL_UNIVERSE_CONNECTIONS_PROPERTY_IDS = {
            Integer.valueOf(268435598),
            Integer.valueOf(268435564)
    };

    private final ConnectionManager connectionManager;

    public UniverseDebugModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public DebugResult inspectUniverse(int universeId) throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects universes = infoStore.query("SELECT TOP 1 * FROM CI_APPOBJECTS WHERE SI_ID = " + universeId);

        if (universes.size() == 0) {
            return DebugResult.notFound(universeId);
        }

        IInfoObject universe = (IInfoObject) universes.get(0);
        List<DebugResult.Row> properties = new ArrayList<>();
        InfoObjectDebugSupport.flattenProperties("", universe.properties(), properties, 0);

        List<DebugResult.QueryResult> queryResults = new ArrayList<>();
        queryResults.add(InfoObjectDebugSupport.queryObjects(
                infoStore,
                "Exact SL universe query",
                "SELECT TOP 10 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_SL_UNIVERSE_TO_CONNECTIONS "
                        + "FROM CI_APPOBJECTS "
                        + "WHERE SI_SPECIFIC_KIND = 'DSL.Universe' AND SI_ID = " + universeId));
        queryResults.add(InfoObjectDebugSupport.queryObjects(
                infoStore,
                "Connection ID lookup from all properties",
                buildConnectionLookupQuery(universe.properties())));

        return new DebugResult(
                universeId,
                universe.getTitle(),
                universe.getCUID(),
                universe.getKind(),
                properties,
                queryResults);
    }

    private String buildConnectionLookupQuery(IProperties universeProperties) {
        Set<String> connectionIds = new LinkedHashSet<>();
        collectIds(getSlUniverseConnectionsProperties(universeProperties), connectionIds);

        if (connectionIds.isEmpty()) {
            return "SELECT TOP 1 SI_ID, SI_NAME, SI_CUID, SI_KIND FROM CI_INFOOBJECTS WHERE SI_ID = -1";
        }

        List<String> predicates = new ArrayList<>();
        for (String connectionId : connectionIds) {
            predicates.add("SI_ID = " + connectionId);
        }

        return "SELECT TOP 100 SI_ID, SI_NAME, SI_CUID, SI_KIND, SI_PARENTID "
                + "FROM CI_INFOOBJECTS, CI_APPOBJECTS, CI_SYSTEMOBJECTS "
                + "WHERE " + String.join(" OR ", predicates);
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

    private IProperties getSlUniverseConnectionsProperties(IProperties properties) {
        IProperties connections = getProperties(properties, SL_UNIVERSE_CONNECTIONS_PROPERTY);
        if (connections != null) {
            return connections;
        }

        for (Integer propertyId : SL_UNIVERSE_CONNECTIONS_PROPERTY_IDS) {
            connections = getProperties(properties, propertyId);
            if (connections != null) {
                return connections;
            }
        }

        return null;
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
