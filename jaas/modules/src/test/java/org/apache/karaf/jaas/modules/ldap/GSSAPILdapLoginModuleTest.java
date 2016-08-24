package org.apache.karaf.jaas.modules.ldap;

import org.apache.commons.io.IOUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapLoginModuleTest-class",
        partitions = {@CreatePartition(name = "example", suffix = "dc=example,dc=com")})
@ApplyLdifFiles(
        "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class GSSAPILdapLoginModuleTest extends AbstractLdapTestUnit {
    private static boolean portUpdated;

    @Before
    public void init() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File config = new File(basedir + "/src/test/resources/org/apache/karaf/jaas/modules/ldap/gssapi.login.config");

        System.setProperty("java.security.auth.login.config", config.toString());

        updatePort();
    }

    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // Read in ldap.properties and substitute in the correct port
            File f = new File(basedir + "/src/test/resources/org/apache/karaf/jaas/modules/ldap/ldap.properties");

            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("portno", "" + super.getLdapServer().getPort());

            File f2 = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldap.properties");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            portUpdated = true;
        }

    }

    @After
    public void tearDown() {
        LDAPCache.clear();
    }

    @Test
    public void testSuccess() throws Exception {

        Properties options = ldapLoginModuleOptions();
        options.setProperty(GSSAPILdapLoginModule.REALM_PROPERTY, "propertiesTestRealm");
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        CallbackHandler cb = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback cb : callbacks) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName("admin");
                    } else if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword("admin123".toCharArray());
                    }
                }
            }
        };
        Subject subject = new Subject();
        module.initialize(subject, cb, null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(2, subject.getPrincipals().size());

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal pr : subject.getPrincipals()) {
            if (pr instanceof UserPrincipal) {
                assertEquals("admin", pr.getName());
                foundUser = true;
            } else if (pr instanceof RolePrincipal) {
                assertEquals("admin", pr.getName());
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        assertTrue(foundRole);

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test(expected = LoginException.class)
    public void testUsernameFailure() throws Exception {

        Properties options = ldapLoginModuleOptions();
        options.setProperty(GSSAPILdapLoginModule.REALM_PROPERTY, "propertiesTestRealm");
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        CallbackHandler cb = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback cb : callbacks) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName("admin0");
                    } else if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword("admin123".toCharArray());
                    }
                }
            }
        };
        Subject subject = new Subject();
        module.initialize(subject, cb, null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login()); // should throw LoginException
    }

    @Test(expected = LoginException.class)
    public void testPasswordFailure() throws Exception {

        Properties options = ldapLoginModuleOptions();
        options.setProperty(GSSAPILdapLoginModule.REALM_PROPERTY, "propertiesTestRealm");
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        CallbackHandler cb = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback cb : callbacks) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName("admin");
                    } else if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword("admin1230".toCharArray());
                    }
                }
            }
        };
        Subject subject = new Subject();
        module.initialize(subject, cb, null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
    }

    @Test
    public void testUserNotFound() throws Exception {

        Properties options = ldapLoginModuleOptions();
        options.setProperty(GSSAPILdapLoginModule.REALM_PROPERTY, "propertiesTestRealm");
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        CallbackHandler cb = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback cb : callbacks) {
                    if (cb instanceof NameCallback) {
                        ((NameCallback) cb).setName("test");
                    } else if (cb instanceof PasswordCallback) {
                        ((PasswordCallback) cb).setPassword("test".toCharArray());
                    }
                }
            }
        };
        Subject subject = new Subject();
        module.initialize(subject, cb, null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertFalse(module.login());
    }

    protected Properties ldapLoginModuleOptions() throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldap.properties");
        return new Properties(file);
    }
}
