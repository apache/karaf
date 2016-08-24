package org.apache.karaf.jaas.modules.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * Specific LDAPLoginModule to be used with GSSAPI. Uses the specified realm as login context.
 */
public class GSSAPILdapLoginModule extends LDAPLoginModule {

    private static Logger logger = LoggerFactory.getLogger(LDAPLoginModule.class);

    public static final String REALM_PROPERTY = "gssapiRealm";

    private LoginContext context;

    @Override
    public boolean login() throws LoginException {
        if (!options.containsKey(REALM_PROPERTY)) {
            logger.warn(REALM_PROPERTY + " is not set");
            throw new LoginException("cannot authenticate through the delegating realm");
        }

        context = new LoginContext((String) options.get(REALM_PROPERTY), this.subject, this.callbackHandler);
        context.login();

        try {
            return Subject.doAs(context.getSubject(), (PrivilegedExceptionAction<Boolean>) () -> doLogin());
        } catch (PrivilegedActionException pExcp) {
            logger.error("error with delegated authentication", pExcp);
            throw new LoginException(pExcp.getMessage());
        }
    }

    @Override
    protected boolean doLogin() throws LoginException {

        //force GSSAPI for login
        Map<String, Object> opts = new HashMap<>(this.options);
        opts.put(LDAPOptions.AUTHENTICATION, "GSSAPI");

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            return super.doLogin();
        } finally {
            ManagedSSLSocketFactory.setSocketFactory(null);
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }
}
