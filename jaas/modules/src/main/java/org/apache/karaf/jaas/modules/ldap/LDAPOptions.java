/*
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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.jaas.config.KeystoreManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDAPOptions {

    public static final String CONNECTION_URL = "connection.url";
    public static final String CONNECTION_USERNAME = "connection.username";
    public static final String CONNECTION_PASSWORD = "connection.password";
    public static final String USER_BASE_DN = "user.base.dn";
    public static final String USER_FILTER = "user.filter";
    public static final String USER_SEARCH_SUBTREE = "user.search.subtree";
    public static final String USER_PUBKEY_ATTRIBUTE = "user.pubkey.attribute";
    public static final String ROLE_BASE_DN = "role.base.dn";
    public static final String ROLE_FILTER = "role.filter";
    public static final String ROLE_NAME_ATTRIBUTE = "role.name.attribute";
    public static final String ROLE_SEARCH_SUBTREE = "role.search.subtree";
    public static final String ROLE_MAPPING = "role.mapping";
    public static final String AUTHENTICATION = "authentication";
    public static final String ALLOW_EMPTY_PASSWORDS = "allowEmptyPasswords";
    public static final String DISABLE_CACHE = "disableCache";
    public static final String INITIAL_CONTEXT_FACTORY = "initial.context.factory";
    public static final String CONTEXT_PREFIX = "context.";
    public static final String SSL = "ssl";
    public static final String SSL_PROVIDER = "ssl.provider";
    public static final String SSL_PROTOCOL = "ssl.protocol";
    public static final String SSL_ALGORITHM = "ssl.algorithm";
    public static final String SSL_KEYSTORE = "ssl.keystore";
    public static final String SSL_KEYALIAS = "ssl.keyalias";
    public static final String SSL_TRUSTSTORE = "ssl.truststore";
    public static final String SSL_TIMEOUT = "ssl.timeout";
    public static final String USERNAMES_TRIM = "usernames.trim";
    public static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String DEFAULT_AUTHENTICATION = "simple";
    public static final int DEFAULT_SSL_TIMEOUT = 10;

    private static Logger LOGGER = LoggerFactory.getLogger(LDAPLoginModule.class);

    private final Map<String, ?> options;

    public LDAPOptions(Map<String, ?> options) {
        this.options = new HashMap<>(options);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LDAPOptions that = (LDAPOptions) o;
        return options.equals(that.options);

    }

    @Override
    public int hashCode() {
        return options.hashCode();
    }

    public boolean isUsernameTrim() {
        return Boolean.parseBoolean((String) options.get(USERNAMES_TRIM));
    }

    public String getUserFilter() {
        return (String) options.get(USER_FILTER);
    }

    public String getUserBaseDn() {
        return (String) options.get(USER_BASE_DN);
    }

    public boolean getUserSearchSubtree() {
        return Boolean.parseBoolean((String) options.get(USER_SEARCH_SUBTREE));
    }

    public String getUserPubkeyAttribute() {
        return (String) options.get(USER_PUBKEY_ATTRIBUTE);
    }

    public String getRoleFilter() {
        return (String) options.get(ROLE_FILTER);
    }

    public String getRoleBaseDn() {
        return (String) options.get(ROLE_BASE_DN);
    }

    public boolean getRoleSearchSubtree() {
        return Boolean.parseBoolean((String) options.get(ROLE_SEARCH_SUBTREE));
    }

    public String getRoleNameAttribute() {
        return (String) options.get(ROLE_NAME_ATTRIBUTE);
    }

    public Map<String, Set<String>> getRoleMapping() {
        return parseRoleMapping((String) options.get(ROLE_MAPPING));
    }

    private Map<String, Set<String>> parseRoleMapping(String option) {
        Map<String, Set<String>> roleMapping = new HashMap<String, Set<String>>();
        if (option != null) {
            LOGGER.debug("Parse role mapping {}", option);
            String[] mappings = option.split(";");
            for (String mapping : mappings) {
                int index = mapping.lastIndexOf("=");
                String ldapRole = mapping.substring(0,index).trim();
                String[] karafRoles = mapping.substring(index+1).split(",");
                final Set<String> karafRolesSet = roleMapping.computeIfAbsent(ldapRole, k -> new HashSet<>());
                for (String karafRole : karafRoles) {
                    karafRolesSet.add(karafRole.trim());
                }
            }
        }
        return roleMapping;
    }

    public Hashtable<String, Object> getEnv() throws NamingException {
        final Hashtable<String, Object> env = new Hashtable<>();
        for (String key : options.keySet()) {
            if (key.startsWith(CONTEXT_PREFIX)) {
                env.put(key.substring(CONTEXT_PREFIX.length()), options.get(key));
            }
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, getInitialContextFactory());
        env.put(Context.PROVIDER_URL, getConnectionURL());
        if (getConnectionUsername() != null && getConnectionUsername().trim().length() > 0) {
            String auth = getAuthentication();
            if (auth == null) {
                auth = DEFAULT_AUTHENTICATION;
            }
            env.put(Context.SECURITY_AUTHENTICATION, auth);
            env.put(Context.SECURITY_PRINCIPAL, getConnectionUsername());
            env.put(Context.SECURITY_CREDENTIALS, getConnectionPassword());
        } else if (getAuthentication() != null) {
            env.put(Context.SECURITY_AUTHENTICATION, getAuthentication());
        }
        if (getSsl()) {
            setupSsl(env);
        }
        return env;
    }

    protected void setupSsl(Hashtable<String, Object> env) throws NamingException {
        BundleContext bundleContext = FrameworkUtil.getBundle(LDAPOptions.class).getBundleContext();
        ServiceReference ref = null;
        try {
            LOGGER.debug("Setting up SSL");
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", ManagedSSLSocketFactory.class.getName());
            ref = bundleContext.getServiceReference(KeystoreManager.class.getName());
            KeystoreManager manager = (KeystoreManager) bundleContext.getService(ref);
            SSLSocketFactory factory = manager.createSSLFactory(
                    getSslProvider(), getSslProtocol(), getSslAlgorithm(), getSslKeystore(),
                    getSslKeyAlias(), getSslTrustStore(), getSslTimeout());
            ManagedSSLSocketFactory.setSocketFactory(new ManagedSSLSocketFactory(factory));
            Thread.currentThread().setContextClassLoader(ManagedSSLSocketFactory.class.getClassLoader());
        } catch (Exception e) {
            throw new NamingException("Unable to setup SSL support for LDAP: " + e.getMessage());
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    public Object getInitialContextFactory() {
        String initialContextFactory = (String) options.get(INITIAL_CONTEXT_FACTORY);
        if (initialContextFactory == null) {
            initialContextFactory = DEFAULT_INITIAL_CONTEXT_FACTORY;
        }
        return initialContextFactory;
    }

    public String getConnectionURL() {
        String connectionURL = (String) options.get(CONNECTION_URL);
        if (connectionURL == null || connectionURL.trim().length() == 0) {
            LOGGER.error("No LDAP URL specified.");
        } else if (!connectionURL.startsWith("ldap:") && !connectionURL.startsWith("ldaps:")) {
            LOGGER.error("Invalid LDAP URL: " + connectionURL);
        }
        return connectionURL;
    }

    public String getConnectionUsername() {
        return (String) options.get(CONNECTION_USERNAME);
    }

    public String getConnectionPassword() {
        return (String) options.get(CONNECTION_PASSWORD);
    }

    public String getAuthentication() {
        return (String) options.get(AUTHENTICATION);
    }

    public boolean getSsl() {
        Object val = options.get(SSL);
        if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val != null) {
            return Boolean.parseBoolean(val.toString());
        } else {
            return getConnectionURL().startsWith("ldaps:");
        }
    }

    public String getSslProvider() {
        return (String) options.get(SSL_PROVIDER);
    }

    public String getSslProtocol() {
        return (String) options.get(SSL_PROTOCOL);
    }

    public String getSslAlgorithm() {
        return (String) options.get(SSL_ALGORITHM);
    }

    public String getSslKeystore() {
        return (String) options.get(SSL_KEYSTORE);
    }

    public String getSslKeyAlias() {
        return (String) options.get(SSL_KEYALIAS);
    }

    public String getSslTrustStore() {
        return (String) options.get(SSL_TRUSTSTORE);
    }

    public int getSslTimeout() {
        Object val = options.get(SSL_TIMEOUT);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val != null) {
            return Integer.parseInt(val.toString());
        } else {
            return DEFAULT_SSL_TIMEOUT;
        }
    }

    public boolean getAllowEmptyPasswords() {
        return Boolean.parseBoolean((String) options.get(ALLOW_EMPTY_PASSWORDS));
    }

    public boolean getDisableCache() {
        final Object object = options.get(DISABLE_CACHE);
        return object == null ? true : Boolean.parseBoolean((String) object);
    }

}
