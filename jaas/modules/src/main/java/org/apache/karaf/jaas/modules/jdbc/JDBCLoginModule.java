/*
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.jdbc;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.apache.karaf.jaas.modules.properties.PropertiesLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class JDBCLoginModule extends AbstractKarafLoginModule {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(PropertiesLoginModule.class);

    public static final String PASSWORD_QUERY = "query.password";
    public static final String USER_QUERY = "query.user";
    public static final String ROLE_QUERY = "query.role";
    public static final String INSERT_USER_STATEMENT = "insert.user";
    public static final String INSERT_ROLE_STATEMENT = "insert.role";
    public static final String DELETE_ROLE_STATEMENT = "delete.role";
    public static final String DELETE_ROLES_STATEMENT = "delete.roles";
    public static final String DELETE_USER_STATEMENT = "delete.user";

    private String datasourceURL;
    protected String passwordQuery = "SELECT PASSWORD FROM USERS WHERE USERNAME=?";
    protected String roleQuery = "SELECT ROLE FROM ROLES WHERE USERNAME=?";

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        datasourceURL = JAASUtils.getString(options, JDBCUtils.DATASOURCE);
        if (datasourceURL == null || datasourceURL.trim().length() == 0) {
            LOGGER.error("No datasource was specified ");
        } else if (!datasourceURL.startsWith(JDBCUtils.JNDI) && !datasourceURL.startsWith(JDBCUtils.OSGI)) {
            LOGGER.error("Invalid datasource lookup protocol");
        }
        if (options.containsKey(PASSWORD_QUERY)) {
            passwordQuery = JAASUtils.getString(options, PASSWORD_QUERY);
        }
        if (options.containsKey(ROLE_QUERY)) {
            roleQuery = JAASUtils.getString(options, ROLE_QUERY);
        }
    }

    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }

        user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }

        String password = new String(tmpPassword);
        principals = new HashSet<>();

        try {
            DataSource datasource = JDBCUtils.createDatasource(bundleContext, datasourceURL);
            try (Connection connection = datasource.getConnection()) {
                List<String> passwords = JDBCUtils.rawSelect(connection, passwordQuery, user);
                if (passwords.isEmpty()) {
                    if (!this.detailedLoginExcepion) {
                        throw new LoginException("login failed");
                    } else {
                        throw new LoginException("User " + user + " does not exist");
                    }
                }
                if (!checkPassword(password, passwords.get(0))) {
                    if (!this.detailedLoginExcepion) {
                        throw new LoginException("login failed");
                    } else {
                        throw new LoginException("Password for " + user + " does not match");
                    }
                }
                principals.add(new UserPrincipal(user));

                if (roleQuery != null && !"".equals(roleQuery.trim())) {
                    List<String> roles = JDBCUtils.rawSelect(connection, roleQuery, user);
                    for (String role : roles) {
                        if (role.startsWith(BackingEngine.GROUP_PREFIX)) {
                            principals.add(new GroupPrincipal(role.substring(BackingEngine.GROUP_PREFIX.length())));
                            for (String r : JDBCUtils.rawSelect(connection, roleQuery, role)) {
                                principals.add(new RolePrincipal(r));
                            }
                        } else {
                            principals.add(new RolePrincipal(role));
                        }
                    }
                } else {
                    LOGGER.debug("No roleQuery specified so no roles have been retrieved for the authenticated user");
                }
            }
        } catch (Exception ex) {
            throw new LoginException("Error has occurred while retrieving credentials from database:" + ex.getMessage());
        }
        succeeded = true;
        return true;
    }

}
