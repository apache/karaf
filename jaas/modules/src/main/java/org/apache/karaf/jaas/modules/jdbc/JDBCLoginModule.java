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

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import javax.naming.InitialContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.apache.karaf.jaas.modules.properties.PropertiesLoginModule;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author iocanel
 */
public class JDBCLoginModule extends AbstractKarafLoginModule {

    private static final Log LOG = LogFactory.getLog(PropertiesLoginModule.class);
    private static final String DATASOURCE = "datasource";
    private static final String PASSWORD_QUERY = "query.password";
    private static final String ROLE_QUERY = "query.role";
    private static final String JNDI = "jndi:";
    private static final String OSGI = "osgi:";
    private String datasourceURL;
    protected String passwordQuery = "SELECT PASSWORD FROM USERS WHERE USERNAME=?";
    protected String roleQuery = "SELECT ROLE FROM ROLES WHERE USERNAME=?";

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        datasourceURL = (String) options.get(DATASOURCE);
        passwordQuery = (String) options.get(PASSWORD_QUERY);
        roleQuery = (String) options.get(ROLE_QUERY);
        if (datasourceURL == null || datasourceURL.trim().length() == 0) {
            LOG.error("No datasource was specified ");
        } else if (!datasourceURL.startsWith(JNDI) && !datasourceURL.startsWith(OSGI)) {
            LOG.error("Invalid datasource lookup protocol");
        }
    }

    /**
     * Looks up a datasource from the url. The datasource can be passed either as jndi name or osgi ldap filter.
     * @param url
     * @return
     * @throws Exception
     */
    public Object createDatasource(String url) throws Exception {
        if (url == null) {
            throw new Exception("Illegal datasource url format. Datasource URL cannot be null.");
        } else if (url.trim().length() == 0) {
            throw new Exception("Illegal datasource url format. Datasource URL cannot be empty.");
        } else if (url.startsWith(JNDI)) {
            String jndiName = url.substring(JNDI.length());
            InitialContext ic = new InitialContext();
            Object ds =  ic.lookup(jndiName);
            return ds;
        } else if (url.startsWith(OSGI)) {
            String osgiFilter = url.substring(OSGI.length());
            String clazz = null;
            String filter = null;
            String[] tokens = osgiFilter.split("/", 2);
            if (tokens != null) {
                if (tokens.length > 0) {
                    clazz = tokens[0];
                }
                if (tokens.length > 1) {
                    filter = tokens[1];
                }
            }
            ServiceReference[] references = bundleContext.getServiceReferences(clazz, filter);
            if (references != null) {
                ServiceReference ref = references[0];
                Object ds = bundleContext.getService(ref);
                bundleContext.ungetService(ref);
                return ds;
            } else {
                throw new Exception("Unable to find service reference for datasource: " + clazz + "/" + filter);
            }
        } else {
            throw new Exception("Illegal datasource url format");
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
            Object credentialsDatasource = createDatasource(datasourceURL);

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
                throw new LoginException("User " + user + " does not exist");
            } else {
                String storedPassword = passwordResultSet.getString(1);

                if (!checkPassword(password, storedPassword)) {
                    throw new LoginException("Password for " + user + " does not match");
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
            throw new LoginException("Error has occured while retrieving credentials from databse:" + ex.getMessage());
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
                LOG.warn("Failed to clearly close connection to the database:", ex);
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
            LOG.debug("logout");
        }
        return true;
    }
}
