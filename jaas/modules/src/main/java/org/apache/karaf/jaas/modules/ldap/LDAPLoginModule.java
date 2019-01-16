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
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf JAAS login module which uses a LDAP backend.
 */
public class LDAPLoginModule extends AbstractKarafLoginModule {

    private static Logger logger = LoggerFactory.getLogger(LDAPLoginModule.class);
    
        
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        LDAPCache.clear();
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

        user = Util.doRFC2254Encoding(((NameCallback) callbacks[0]).getName());

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();

        // If either a username or password is specified don't allow authentication = "none".
        // This is to prevent someone from logging into Karaf as any user without providing a 
        // valid password (because if authentication = none, the password could be any 
        // value - it is ignored).
        LDAPOptions options = new LDAPOptions(this.options);
        if(options.isUsernameTrim()){
            if(user != null){
                user = user.trim();
            }
        }
        String authentication = options.getAuthentication();
        if ("none".equals(authentication) && (user != null || tmpPassword != null)) {
            logger.debug("Changing from authentication = none to simple since user or password was specified.");
            // default to simple so that the provided user/password will get checked
            authentication = "simple";
            Map<String, Object> opts = new HashMap<>(this.options);
            opts.put(LDAPOptions.AUTHENTICATION, authentication);
            options = new LDAPOptions(opts);
        }
        boolean allowEmptyPasswords = options.getAllowEmptyPasswords();
        if (!"none".equals(authentication) && !allowEmptyPasswords
                && (tmpPassword == null || tmpPassword.length == 0)) {
            throw new LoginException("Empty passwords not allowed");
        }

        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        String password = new String(tmpPassword);
        principals = new HashSet<>();

        LDAPCache cache = LDAPCache.getCache(options);

        // step 1: get the user DN
        final String[] userDnAndNamespace;
        try {
            logger.debug("Get the user DN.");
            userDnAndNamespace = cache.getUserDnAndNamespace(user);
            if (userDnAndNamespace == null) {
                return false;
            }
        } catch (Exception e) {
            logger.warn("Can't connect to the LDAP server: {}", e.getMessage(), e);
            throw new LoginException("Can't connect to the LDAP server: " + e.getMessage());
        }
        // step 2: bind the user using the DN
        DirContext context = null;
        try {
            // switch the credentials to the Karaf login user so that we can verify his password is correct
            logger.debug("Bind user (authentication).");
            Hashtable<String, Object> env = options.getEnv();
            env.put(Context.SECURITY_AUTHENTICATION, authentication);
            logger.debug("Set the security principal for " + userDnAndNamespace[0] + "," + options.getUserBaseDn());
            env.put(Context.SECURITY_PRINCIPAL, userDnAndNamespace[0] + "," + options.getUserBaseDn());
            env.put(Context.SECURITY_CREDENTIALS, password);
            logger.debug("Binding the user.");
            context = new InitialDirContext(env);
            logger.debug("User " + user + " successfully bound.");
            context.close();
        } catch (Exception e) {
            logger.warn("User " + user + " authentication failed.", e);
            throw new LoginException("Authentication failed: " + e.getMessage());
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        principals.add(new UserPrincipal(user));
        // step 3: retrieving user roles
        try {
            String[] roles = cache.getUserRoles(user, userDnAndNamespace[0], userDnAndNamespace[1]);
            for (String role : roles) {
                principals.add(new RolePrincipal(role));
            }
        } catch (Exception e) {
            throw new LoginException("Can't get user " + user + " roles: " + e.getMessage());
        }
        succeeded = true;
        return true;
    }

}
