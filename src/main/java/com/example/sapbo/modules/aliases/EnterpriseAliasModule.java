package com.example.sapbo.modules.aliases;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.plugin.desktop.user.IUser;
import com.crystaldecisions.sdk.plugin.desktop.user.IUserAlias;
import com.example.sapbo.core.ConnectionManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnterpriseAliasModule {
    private static final String USER_QUERY = "SELECT TOP 100000 * FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'User' ORDER BY SI_NAME";
    private static final String RANDOM_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final ConnectionManager connectionManager;
    private final SecureRandom random = new SecureRandom();

    public EnterpriseAliasModule(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<EnterpriseAliasRecord> findCandidates() throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects users = infoStore.query(USER_QUERY);
        List<EnterpriseAliasRecord> candidates = new ArrayList<>();
        for (Object object : users) {
            IUser user = (IUser) object;
            if (hasAdAlias(user) && !hasEnterpriseAlias(user)) {
                candidates.add(toRecord(user, "", "", "Ready", "AD user without Enterprise alias."));
            }
        }
        return candidates;
    }

    public List<EnterpriseAliasRecord> createAliases(String passwordMode, String manualPassword,
            boolean disableCreatedAlias) throws SDKException {
        IEnterpriseSession session = connectionManager.getSession();
        IInfoStore infoStore = (IInfoStore) session.getService("InfoStore");
        IInfoObjects users = infoStore.query(USER_QUERY);
        IInfoObjects changedUsers = infoStore.newInfoObjectCollection();
        List<EnterpriseAliasRecord> results = new ArrayList<>();
        for (Object object : users) {
            IUser user = (IUser) object;
            if (!hasAdAlias(user) || hasEnterpriseAlias(user)) {
                continue;
            }
            String password = resolvePassword(passwordMode, manualPassword);
            try {
                IUserAlias alias = user.getAliases().addNew(user.getTitle(), disableCreatedAlias, password);
                alias.setDisabled(disableCreatedAlias);
                user.setNewPassword(password);
                changedUsers.add(user);
                results.add(toRecord(user, alias.getName(), password, "Created", "Enterprise alias was created."));
            } catch (RuntimeException exception) {
                results.add(toRecord(user, user.getTitle(), "", "Error", exception.getMessage()));
            }
        }
        if (!changedUsers.isEmpty()) {
            infoStore.commit(changedUsers);
        }
        return results;
    }

    private EnterpriseAliasRecord toRecord(IUser user, String enterpriseAlias, String password,
            String status, String message) throws SDKException {
        return new EnterpriseAliasRecord(user.getID(), user.getTitle(), safeFullName(user), user.getCUID(),
                getAdAliases(user), enterpriseAlias, password, isEnterpriseAliasDisabled(user, enterpriseAlias),
                status, message);
    }

    private boolean hasAdAlias(IUser user) {
        for (IUserAlias alias : user.getAliases()) {
            String combined = normalize(alias.getAuthentication() + " " + alias.getName() + " " + alias.getID());
            if (combined.contains("secwinad") || combined.contains("winad")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnterpriseAlias(IUser user) {
        for (IUserAlias alias : user.getAliases()) {
            String combined = normalize(alias.getAuthentication() + " " + alias.getName() + " " + alias.getID());
            if (alias.getType() == IUserAlias.ENTERPRISE || combined.contains("secenterprise")
                    || combined.contains("enterprise")) {
                return true;
            }
        }
        return false;
    }

    private String getAdAliases(IUser user) {
        List<String> aliases = new ArrayList<>();
        for (IUserAlias alias : user.getAliases()) {
            String combined = normalize(alias.getAuthentication() + " " + alias.getName() + " " + alias.getID());
            if (combined.contains("secwinad") || combined.contains("winad")) {
                aliases.add(alias.getName());
            }
        }
        return String.join(", ", aliases);
    }

    private String isEnterpriseAliasDisabled(IUser user, String enterpriseAlias) {
        if (enterpriseAlias == null || enterpriseAlias.isEmpty()) {
            return "";
        }
        for (IUserAlias alias : user.getAliases()) {
            if (enterpriseAlias.equals(alias.getName())) {
                return String.valueOf(alias.isDisabled());
            }
        }
        return "";
    }

    private String resolvePassword(String passwordMode, String manualPassword) {
        if ("manual".equalsIgnoreCase(passwordMode) && manualPassword != null && !manualPassword.isEmpty()) {
            return manualPassword;
        }
        return randomPassword(18);
    }

    private String randomPassword(int length) {
        StringBuilder password = new StringBuilder();
        for (int index = 0; index < length; index++) {
            password.append(RANDOM_PASSWORD_CHARS.charAt(random.nextInt(RANDOM_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private String safeFullName(IUser user) {
        try {
            return user.getFullName();
        } catch (SDKException exception) {
            return "";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }
}
