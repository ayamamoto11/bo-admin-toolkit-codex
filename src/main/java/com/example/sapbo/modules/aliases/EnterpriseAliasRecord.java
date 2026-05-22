package com.example.sapbo.modules.aliases;

import java.io.Serializable;

public class EnterpriseAliasRecord implements Serializable {
    private final int userId;
    private final String username;
    private final String fullName;
    private final String userCuid;
    private final String adAliases;
    private final String enterpriseAlias;
    private final String password;
    private final String disabled;
    private final String status;
    private final String message;

    public EnterpriseAliasRecord(int userId, String username, String fullName, String userCuid,
            String adAliases, String enterpriseAlias, String password, String disabled,
            String status, String message) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.userCuid = userCuid;
        this.adAliases = adAliases;
        this.enterpriseAlias = enterpriseAlias;
        this.password = password;
        this.disabled = disabled;
        this.status = status;
        this.message = message;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getUserCuid() { return userCuid; }
    public String getAdAliases() { return adAliases; }
    public String getEnterpriseAlias() { return enterpriseAlias; }
    public String getPassword() { return password; }
    public String getDisabled() { return disabled; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
