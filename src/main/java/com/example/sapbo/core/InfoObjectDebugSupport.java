package com.example.sapbo.core;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.IProperty;

import java.util.ArrayList;
import java.util.List;

public final class InfoObjectDebugSupport {
    private InfoObjectDebugSupport() {
    }

    public static void flattenProperties(
            String prefix,
            IProperties properties,
            List<DebugResult.Row> rows,
            int depth) {
        if (properties == null || depth > 6) {
            return;
        }

        for (Object keyObject : properties.keySet()) {
            String key = String.valueOf(keyObject);
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            IProperty property = properties.getProperty(keyObject);

            if (property != null && property.isContainer()) {
                rows.add(new DebugResult.Row(path, "[container]", "", ""));
                IProperties childProperties = getProperties(properties, keyObject);
                if (childProperties != null) {
                    flattenProperties(path, childProperties, rows, depth + 1);
                }
            } else {
                rows.add(new DebugResult.Row(path, safeString(properties, keyObject), "", ""));
            }
        }
    }

    public static DebugResult.QueryResult queryObjects(
            IInfoStore infoStore,
            String label,
            String query) {
        try {
            IInfoObjects objects = infoStore.query(query);
            List<DebugResult.Row> rows = new ArrayList<>();
            for (Object object : objects) {
                IInfoObject infoObject = (IInfoObject) object;
                rows.add(new DebugResult.Row(
                        String.valueOf(infoObject.getID()),
                        infoObject.getTitle(),
                        safeKind(infoObject),
                        safeCuid(infoObject)));
            }

            return new DebugResult.QueryResult(label, query, rows, "");
        } catch (SDKException exception) {
            return new DebugResult.QueryResult(label, query, new ArrayList<DebugResult.Row>(), exception.getMessage());
        }
    }

    private static String safeKind(IInfoObject infoObject) {
        try {
            return infoObject.getKind();
        } catch (SDKException exception) {
            return "";
        }
    }

    private static String safeCuid(IInfoObject infoObject) {
        try {
            return infoObject.getCUID();
        } catch (SDKException exception) {
            return "";
        }
    }

    public static String safeString(IProperties properties, Object keyObject) {
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

    private static IProperties getProperties(IProperties properties, Object propertyName) {
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

    private static boolean isPositiveInteger(String value) {
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
