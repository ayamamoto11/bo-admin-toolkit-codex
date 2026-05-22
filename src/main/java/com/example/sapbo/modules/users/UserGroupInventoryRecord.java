package com.example.sapbo.modules.users;

import java.io.Serializable;

public class UserGroupInventoryRecord implements Serializable {
    private final String objectType;
    private final int objectId;
    private final String objectName;
    private final String objectCuid;
    private final String parentGroupId;
    private final String parentGroupName;
    private final String groupPath;
    private final String disabled;

    public UserGroupInventoryRecord(
            String objectType,
            int objectId,
            String objectName,
            String objectCuid,
            String parentGroupId,
            String parentGroupName,
            String groupPath,
            String disabled) {
        this.objectType = objectType;
        this.objectId = objectId;
        this.objectName = objectName;
        this.objectCuid = objectCuid;
        this.parentGroupId = parentGroupId;
        this.parentGroupName = parentGroupName;
        this.groupPath = groupPath;
        this.disabled = disabled;
    }

    public String getObjectType() {
        return objectType;
    }

    public int getObjectId() {
        return objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectCuid() {
        return objectCuid;
    }

    public String getParentGroupId() {
        return parentGroupId;
    }

    public String getParentGroupName() {
        return parentGroupName;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getDisabled() {
        return disabled;
    }
}
