package com.example.sapbo.core;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ConnectionManager implements AutoCloseable {
    private static final String DEFAULT_CONFIG_FILE = "boe.properties";

    private final Properties properties;
    private final BoCredentials credentials;
    private IEnterpriseSession enterpriseSession;

    public ConnectionManager() {
        this(DEFAULT_CONFIG_FILE);
    }

    public ConnectionManager(String configFileName) {
        this.properties = loadProperties(configFileName);
        this.credentials = null;
    }

    public ConnectionManager(BoCredentials credentials) {
        this.properties = new Properties();
        this.credentials = credentials;
    }

    public IEnterpriseSession getSession() throws SDKException {
        if (enterpriseSession == null) {
            BoCredentials activeCredentials = getActiveCredentials();
            enterpriseSession = CrystalEnterprise.getSessionMgr().logon(
                    activeCredentials.getUsername(),
                    activeCredentials.getPassword(),
                    activeCredentials.getCms(),
                    activeCredentials.getAuthentication());
        }

        return enterpriseSession;
    }

    @Override
    public void close() {
        if (enterpriseSession != null) {
            enterpriseSession.logoff();
            enterpriseSession = null;
        }
    }

    private Properties loadProperties(String configFileName) {
        Properties loadedProperties = new Properties();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(configFileName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration file on classpath: " + configFileName);
            }

            loadedProperties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load configuration file: " + configFileName, exception);
        }

        return loadedProperties;
    }

    private String getRequiredProperty(String key) {
        String value = Objects.toString(properties.getProperty(key), "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }

        return value;
    }

    private BoCredentials getActiveCredentials() {
        if (credentials != null) {
            return credentials;
        }

        return new BoCredentials(
                getRequiredProperty("boe.cms"),
                getRequiredProperty("boe.username"),
                getRequiredProperty("boe.password"),
                getRequiredProperty("boe.authentication"));
    }
}
