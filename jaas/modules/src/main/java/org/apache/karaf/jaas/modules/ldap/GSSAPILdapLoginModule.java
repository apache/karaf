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

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Specific LDAPLoginModule to be used with GSSAPI. Uses the specified realm as login context.
 */
public class GSSAPILdapLoginModule extends AbstractKarafLoginModule {

    private static Logger logger = LoggerFactory.getLogger(LDAPLoginModule.class);

    public static final String REALM_PROPERTY = "gssapiRealm";

    private LoginContext context;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
    }

    @Override
    public boolean login() throws LoginException {
        if (!options.containsKey(REALM_PROPERTY)) {
            logger.warn(REALM_PROPERTY + " is not set");
            throw new LoginException("cannot authenticate through the delegating realm");
        }

        context = new LoginContext(JAASUtils.getString(options, REALM_PROPERTY), this.subject, this.callbackHandler);
        context.login();

        try {
            succeeded = Subject.doAs(context.getSubject(), (PrivilegedExceptionAction<Boolean>) this::doLogin);
            return succeeded;
        } catch (PrivilegedActionException pExcp) {
            logger.error("error with delegated authentication", pExcp);
            throw new LoginException(pExcp.getMessage());
        }
    }

    protected boolean doLogin() throws LoginException {

        //force GSSAPI for login
        Map<String, Object> opts = new HashMap<>(this.options);
        opts.put(LDAPOptions.AUTHENTICATION, "GSSAPI");

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            LDAPOptions lOptions = new LDAPOptions(opts);

            NameCallback[] callbacks = new NameCallback[1];
            callbacks[0] = new NameCallback("Username: ");

            try {
                callbackHandler.handle(callbacks);
            } catch (IOException ioException) {
                logger.error("error with callback handler", ioException);
                throw new LoginException(ioException.getMessage());
            } catch (UnsupportedCallbackException unsupportedCallbackException) {
                logger.error("error with callback handler", unsupportedCallbackException);
                throw new LoginException(unsupportedCallbackException.getMessage() + " not available to obtain information from user.");
            }

            user = callbacks[0].getName();

            principals = new HashSet<>();

            String[] userDnAndNamespace;
            try (LDAPCache cache = LDAPCache.getCache(lOptions)) {

                try {
                    logger.debug("Get the user DN.");
                    userDnAndNamespace = cache.getUserDnAndNamespace(user);

                } catch (Exception e) {
                    logger.warn("Can't connect to the LDAP server: {}", e.getMessage(), e);
                    throw new LoginException("Can't connect to the LDAP server: " + e.getMessage());
                }

                if (userDnAndNamespace == null) {
                    return false;
                }

                principals.add(new UserPrincipal(user));

                try {
                    String[] roles = cache.getUserRoles(user, userDnAndNamespace[0], userDnAndNamespace[1]);
                    for (String role : roles) {
                        principals.add(new RolePrincipal(role));
                    }
                } catch (Exception e) {
                    throw new LoginException("Can't get user " + user + " roles: " + e.getMessage());
                }

                return true;
            }
        } finally {
            ManagedSSLSocketFactory.setSocketFactory(null);
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Override
    public boolean commit() throws LoginException {
        boolean ret = super.commit();
        if (ret) {
            principals.addAll(subject.getPrincipals(KerberosPrincipal.class));
        }
        return ret;
    }
}
