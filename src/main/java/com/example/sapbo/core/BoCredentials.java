package com.example.sapbo.core;

import java.util.Objects;
import java.io.Serializable;

public class BoCredentials implements Serializable {
    public static final String ENTERPRISE_AUTHENTICATION = "secEnterprise";

    private final String cms;
    private final String username;
    private final String password;
    private final String authentication;

    public BoCredentials(String cms, String username, String password) {
        this(cms, username, password, ENTERPRISE_AUTHENTICATION);
    }

    public BoCredentials(String cms, String username, String password, String authentication) {
        this.cms = requireValue(cms, "CMS");
        this.username = requireValue(username, "user");
        this.password = requireValue(password, "password");
        this.authentication = requireValue(authentication, "authentication");
    }

    public String getCms() {
        return cms;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAuthentication() {
        return authentication;
    }

    private static String requireValue(String value, String label) {
        String cleanValue = Objects.toString(value, "").trim();
        if (cleanValue.isEmpty()) {
            throw new IllegalArgumentException("Missing required " + label + " value.");
        }

        return cleanValue;
    }
}
