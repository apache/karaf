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
package org.apache.karaf.jaas.modules.ldap;

import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

/**
 * <p>
 * Karaf JAAS login module which uses a LDAP backend.
 * </p>
 *
 * @author jbonofre
 * @author gnodet
 */
public class LDAPLoginModule extends AbstractKarafLoginModule {

    private static Logger logger = LoggerFactory.getLogger(LDAPLoginModule.class);

    public final static String CONNECTION_URL = "connection.url";
    public final static String CONNECTION_USERNAME = "connection.username";
    public final static String CONNECTION_PASSWORD = "connection.password";
    public final static String USER_BASE_DN = "user.base.dn";
    public final static String USER_FILTER = "user.filter";
    public final static String USER_SEARCH_SUBTREE = "user.search.subtree";
    public final static String ROLE_BASE_DN = "role.base.dn";
    public final static String ROLE_FILTER = "role.filter";
    public final static String ROLE_NAME_ATTRIBUTE = "role.name.attribute";
    public final static String ROLE_SEARCH_SUBTREE = "role.search.subtree";
    public final static String AUTHENTICATION = "authentication";
    public final static String INITIAL_CONTEXT_FACTORY = "initial.context.factory";
    public final static String SSL = "ssl";
    public final static String SSL_PROVIDER = "ssl.provider";
    public final static String SSL_PROTOCOL = "ssl.protocol";
    public final static String SSL_ALGORITHM = "ssl.algorithm";
    public final static String SSL_KEYSTORE = "ssl.keystore";
    public final static String SSL_KEYALIAS = "ssl.keyalias";
    public final static String SSL_TRUSTSTORE = "ssl.truststore";

    public final static String DEFAULT_INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    private String connectionURL;
    private String connectionUsername;
    private String connectionPassword;
    private String userBaseDN;
    private String userFilter;
    private boolean userSearchSubtree = true;
    private String roleBaseDN;
    private String roleFilter;
    private String roleNameAttribute;
    private boolean roleSearchSubtree = true;
    private String authentication = "simple";
    private String initialContextFactory = null;
    private boolean ssl;
    private String sslProvider;
    private String sslProtocol;
    private String sslAlgorithm;
    private String sslKeystore;
    private String sslKeyAlias;
    private String sslTrustStore;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        connectionURL = (String) options.get(CONNECTION_URL);
        connectionUsername = (String) options.get(CONNECTION_USERNAME);
        connectionPassword = (String) options.get(CONNECTION_PASSWORD);
        userBaseDN =  (String) options.get(USER_BASE_DN);
        userFilter = (String) options.get(USER_FILTER);
        userSearchSubtree = Boolean.parseBoolean((String) options.get(USER_SEARCH_SUBTREE));
        roleBaseDN = (String) options.get(ROLE_BASE_DN);
        roleFilter = (String) options.get(ROLE_FILTER);
        roleNameAttribute = (String) options.get(ROLE_NAME_ATTRIBUTE);
        roleSearchSubtree = Boolean.parseBoolean((String) options.get(ROLE_SEARCH_SUBTREE));
        initialContextFactory = (String) options.get(INITIAL_CONTEXT_FACTORY);
        if (initialContextFactory == null) {
            initialContextFactory = DEFAULT_INITIAL_CONTEXT_FACTORY;
        }
        authentication = (String) options.get(AUTHENTICATION);
        if (connectionURL == null || connectionURL.trim().length() == 0) {
            logger.error("No LDAP URL specified.");
        } else if (!connectionURL.startsWith("ldap:") && !connectionURL.startsWith("ldaps:")) {
            logger.error("Invalid LDAP URL: " + connectionURL);
        }
        if (options.get(SSL) != null) {
            ssl = Boolean.parseBoolean((String) options.get(SSL));
        } else {
            ssl = connectionURL.startsWith("ldaps:");
        }
        sslProvider = (String) options.get(SSL_PROVIDER);
        sslProtocol = (String) options.get(SSL_PROTOCOL);
        sslAlgorithm = (String) options.get(SSL_ALGORITHM);
        sslKeystore = (String) options.get(SSL_KEYSTORE);
        sslKeyAlias = (String) options.get(SSL_KEYALIAS);
        sslTrustStore = (String) options.get(SSL_TRUSTSTORE);
    }

    public boolean login() throws LoginException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            return doLogin();
        } finally {
            ManagedSSLSocketFactory.setSocketFactory(null);
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    protected boolean doLogin() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioException) {
            throw new LoginException(ioException.getMessage());
        } catch (UnsupportedCallbackException unsupportedCallbackException) {
            throw new LoginException(unsupportedCallbackException.getMessage() + " not available to obtain information from user.");
        }

        user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        String password = new String(tmpPassword);
        principals = new HashSet<Principal>();

        // step 1: get the user DN
        Hashtable env = new Hashtable();
        logger.debug("Create the LDAP initial context.");
        env.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
        env.put(Context.PROVIDER_URL, connectionURL);
        if (connectionUsername != null && connectionUsername.trim().length() > 0) {
            logger.debug("Bound access requested.");
            env.put(Context.SECURITY_AUTHENTICATION, authentication);
            env.put(Context.SECURITY_PRINCIPAL, connectionUsername);
            env.put(Context.SECURITY_CREDENTIALS, connectionPassword);
        }
        if (ssl) {
            setupSsl(env);
        }
        logger.debug("Get the user DN.");
        String userDN;
        try {
            logger.debug("Initialize the JNDI LDAP Dir Context.");
            DirContext context = new InitialDirContext(env);
            logger.debug("Define the subtree scope search control.");
            SearchControls controls = new SearchControls();
            if (userSearchSubtree) {
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }
            logger.debug("Looking for the user in LDAP with ");
            logger.debug("  base DN: " + userBaseDN);
            userFilter = userFilter.replaceAll("%u", user);
            logger.debug("  filter: " + userFilter);
            NamingEnumeration namingEnumeration = context.search(userBaseDN, userFilter, controls);
            if (!namingEnumeration.hasMore()) {
                logger.warn("User " + user + " not found in LDAP.");
                return false;
            }
            logger.debug("Get the user DN.");
            SearchResult result = (SearchResult) namingEnumeration.next();
            userDN = (String) result.getName();
        } catch (Exception e) {
            throw new LoginException("Can't connect to the LDAP server: " + e.getMessage());
        }
        // step 2: bind the user using the DN
        try {
            logger.debug("Bind user (authentication).");
            env.put(Context.SECURITY_AUTHENTICATION, authentication);
            logger.debug("Set the security principal for " + userDN + "," + userBaseDN);
            env.put(Context.SECURITY_PRINCIPAL, userDN + "," + userBaseDN);
            env.put(Context.SECURITY_CREDENTIALS, password);
            logger.debug("Binding the user.");
            DirContext context = new InitialDirContext(env);
            logger.debug("User " + user + " successfully bound.");
            context.close();
        } catch (Exception e) {
            logger.warn("User " + user + " authentication failed.", e);
            return false;
        }
        principals.add(new UserPrincipal(user));
        // step 3: retrieving user roles
        try {
            logger.debug("Get user roles.");
            DirContext context = new InitialDirContext(env);
            SearchControls controls = new SearchControls();
            if (roleSearchSubtree) {
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }
            logger.debug("Looking for the user roles in LDAP with ");
            logger.debug("  base DN: " + roleBaseDN);
            roleFilter = roleFilter.replaceAll("%u", user);
            logger.debug("  filter: " + roleFilter);
            NamingEnumeration namingEnumeration = context.search(roleBaseDN, roleFilter, controls);
            while (namingEnumeration.hasMore()) {
                SearchResult result = (SearchResult) namingEnumeration.next();
                Attributes attributes = result.getAttributes();
                String role = (String) attributes.get(roleNameAttribute).get();
                if (role != null) {
                    principals.add(new RolePrincipal(role));
                }
            }
        } catch (Exception e) {
            throw new LoginException("Can't get user " + user + " roles: " + e.getMessage());
        }
        return true;
    }

    protected void setupSsl(Hashtable env) throws LoginException {
        ServiceReference ref = null;
        try {
            logger.debug("Setting up SSL");
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", ManagedSSLSocketFactory.class.getName());
            ref = bundleContext.getServiceReference(KeystoreManager.class.getName());
            KeystoreManager manager = (KeystoreManager) bundleContext.getService(ref);
            SSLSocketFactory factory = manager.createSSLFactory(sslProvider, sslProtocol, sslAlgorithm, sslKeystore, sslKeyAlias, sslTrustStore);
            ManagedSSLSocketFactory.setSocketFactory(factory);
            Thread.currentThread().setContextClassLoader(ManagedSSLSocketFactory.class.getClassLoader());
        } catch (Exception e) {
            throw new LoginException("Unable to setup SSL support for LDAP: " + e.getMessage());
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    public static abstract class ManagedSSLSocketFactory extends SSLSocketFactory {

        private static final ThreadLocal<SSLSocketFactory> factories = new ThreadLocal<SSLSocketFactory>();

        public static void setSocketFactory(SSLSocketFactory factory) {
            factories.set(factory);
        }

        public static SSLSocketFactory getDefault() {
            SSLSocketFactory factory = factories.get();
            if (factory == null) {
                throw new IllegalStateException("No SSLSocketFactory parameters have been set!");
            }
            return factory;
        }

    }

}
