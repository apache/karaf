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

import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.apache.karaf.jaas.modules.properties.PropertiesLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

public class JDBCLoginModule extends AbstractKarafLoginModule {

    private final Logger logger = LoggerFactory.getLogger(PropertiesLoginModule.class);

    public static final String PASSWORD_QUERY = "query.password";
    public static final String ROLE_QUERY = "query.role";
    public static final String INSERT_USER_STATEMENT = "insert.user";
    public static final String INSERT_ROLE_STATEMENT = "insert.role";
    public static final String DELETE_ROLE_STATEMENT = "delete.role";
    public static final String DELETE_ROLES_STATEMENT = "delete.roles";
    public static final String DELETE_USER_STATEMENT = "delete.roles";

    private String datasourceURL;
    protected String passwordQuery = "SELECT PASSWORD FROM USERS WHERE USERNAME=?";
    protected String roleQuery = "SELECT ROLE FROM ROLES WHERE USERNAME=?";

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        datasourceURL = (String) options.get(JDBCUtils.DATASOURCE);
        passwordQuery = (String) options.get(PASSWORD_QUERY);
        roleQuery = (String) options.get(ROLE_QUERY);
        if (datasourceURL == null || datasourceURL.trim().length() == 0) {
            logger.error("No datasource was specified ");
        } else if (!datasourceURL.startsWith(JDBCUtils.JNDI) && !datasourceURL.startsWith(JDBCUtils.OSGI)) {
            logger.error("Invalid datasource lookup protocol");
        }
    }


    public boolean login() throws LoginException {
        Connection connection = null;

        PreparedStatement passwordStatement = null;
        PreparedStatement roleStatement = null;

        ResultSet passwordResultSet = null;
        ResultSet roleResultSet = null;

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
        principals = new HashSet<Principal>();

        try {
            Object credentialsDatasource = JDBCUtils.createDatasource(bundleContext, datasourceURL);

            if (credentialsDatasource == null) {
                throw new LoginException("Cannot obtain data source:" + datasourceURL);
            } else if (credentialsDatasource instanceof DataSource) {
                connection = ((DataSource) credentialsDatasource).getConnection();
            } else if (credentialsDatasource instanceof XADataSource) {
                connection = ((XADataSource) credentialsDatasource).getXAConnection().getConnection();
            } else {
                throw new LoginException("Unknow dataSource type " + credentialsDatasource.getClass());
            }

            //Retrieve user credentials from database.
            passwordStatement = connection.prepareStatement(passwordQuery);
            passwordStatement.setString(1, user);
            passwordResultSet = passwordStatement.executeQuery();

            if (!passwordResultSet.next()) {
            	if (!this.detailedLoginExcepion) {
            		throw new LoginException("login failed");
            	} else {
            		throw new LoginException("User " + user + " does not exist");
            	}
            } else {
                String storedPassword = passwordResultSet.getString(1);

                if (!checkPassword(password, storedPassword)) {
                	if (!this.detailedLoginExcepion) {
                		throw new LoginException("login failed");
                	} else {
                		throw new LoginException("Password for " + user + " does not match");
                	}
                }
                principals.add(new UserPrincipal(user));
            }

            //Retrieve user roles from database
            roleStatement = connection.prepareStatement(roleQuery);
            roleStatement.setString(1, user);
            roleResultSet = roleStatement.executeQuery();
            while (roleResultSet.next()) {
                String role = roleResultSet.getString(1);
                principals.add(new RolePrincipal(role));
            }
        } catch (Exception ex) {
            throw new LoginException("Error has occured while retrieving credentials from database:" + ex.getMessage());
        } finally {
            try {
                if (passwordResultSet != null) {
                    passwordResultSet.close();
                }
                if (passwordStatement != null) {
                    passwordStatement.close();
                }
                if (roleResultSet != null) {
                    roleResultSet.close();
                }
                if (roleStatement != null) {
                    roleStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                logger.warn("Failed to clearly close connection to the database:", ex);
            }
        }
        return true;
    }

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        if (debug) {
            logger.debug("logout");
        }
        return true;
    }
}
