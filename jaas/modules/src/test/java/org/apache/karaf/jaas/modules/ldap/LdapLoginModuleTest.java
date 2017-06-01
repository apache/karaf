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

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapLoginModuleTest-class",
        partitions = {@CreatePartition(name = "example", suffix = "dc=example,dc=com")})
@ApplyLdifFiles(
        "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapLoginModuleTest extends AbstractLdapTestUnit {
    
    private static boolean portUpdated;

    @Before
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
    public void testAdminLogin() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "admin123"), null, options);

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

    protected Properties ldapLoginModuleOptions() throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldap.properties");
        return new Properties(file);
    }

    @Test
    public void testNonAdminLogin() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("cheese", "foodie"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(1, subject.getPrincipals().size());

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal pr : subject.getPrincipals()) {
            if (pr instanceof UserPrincipal) {
                assertEquals("cheese", pr.getName());
                foundUser = true;
            } else if (pr instanceof RolePrincipal) {
                assertEquals("admin", pr.getName());
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        // cheese is not an admin so no roles should be returned
        assertFalse(foundRole);

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testTrimmedUsernameLogin() throws Exception {
        Properties options = ldapLoginModuleOptions();
        options.put("usernames.trim", "true");
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("cheese   ", "foodie"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(1, subject.getPrincipals().size());

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal pr : subject.getPrincipals()) {
            if (pr instanceof UserPrincipal) {
                assertEquals("cheese", pr.getName());
                foundUser = true;
            } else if (pr instanceof RolePrincipal) {
                assertEquals("admin", pr.getName());
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        // cheese is not an admin so no roles should be returned
        assertFalse(foundRole);

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testBadPassword() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "blahblah"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Should have thrown LoginException");
        } catch (LoginException e) {
            assertTrue(e.getMessage().startsWith("Authentication failed"));
        }
    }

    @Test
    public void testUserNotFound() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("imnothere", "admin123"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertFalse(module.login());
    }

    @Test
    public void testEmptyPassword() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("imnothere", ""), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        try {
            module.login();
            fail("Should have failed");
        } catch (LoginException e) {
            assertTrue(e.getMessage().equals("Empty passwords not allowed"));
        }
    }

    @Test
    public void testRoleMappingSimple() throws Exception {
        Properties options = ldapLoginModuleOptions();
        options.put(LDAPOptions.ROLE_MAPPING, "admin=karaf");
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "admin123"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(2, subject.getPrincipals().size());

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof UserPrincipal) {
                assertEquals("admin", principal.getName());
                foundUser = true;
            } else if (principal instanceof RolePrincipal) {
                assertEquals("karaf", principal.getName());
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        assertTrue(foundRole);

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testRoleMappingAdvanced() throws Exception {
        Properties options = ldapLoginModuleOptions();
        options.put(LDAPOptions.ROLE_MAPPING, "admin=karaf,test;admin=another");
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "admin123"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(4, subject.getPrincipals().size());

        final List<String> roles = new ArrayList<>(Arrays.asList("karaf", "test", "another"));

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof UserPrincipal) {
                assertEquals("admin", principal.getName());
                foundUser = true;
            } else if (principal instanceof RolePrincipal) {
                assertTrue(roles.remove(principal.getName()));
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        assertTrue(foundRole);
        assertTrue(roles.isEmpty());

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testRoleMappingParsing() throws Exception {
        Properties options = ldapLoginModuleOptions();
        options.put(LDAPOptions.ROLE_MAPPING, "admin = karaf, test; admin = another");
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "admin123"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(4, subject.getPrincipals().size());

        final List<String> roles = new ArrayList<>(Arrays.asList("karaf", "test", "another"));

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal pr : subject.getPrincipals()) {
            if (pr instanceof UserPrincipal) {
                assertEquals("admin", pr.getName());
                foundUser = true;
            } else if (pr instanceof RolePrincipal) {
                assertTrue(roles.remove(pr.getName()));
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        assertTrue(foundRole);
        assertTrue(roles.isEmpty());

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testRoleMappingFqdn() throws Exception {
        Properties options = ldapLoginModuleOptions();
        options.put(LDAPOptions.ROLE_MAPPING, "cn=admin,ou=groups,dc=example,dc=com=karaf;cn=admin,ou=mygroups,dc=example,dc=com=another");
        options.put(LDAPOptions.ROLE_BASE_DN, "ou=groups,dc=example,dc=com");
        options.put(LDAPOptions.ROLE_SEARCH_SUBTREE, "true");
        options.put(LDAPOptions.ROLE_FILTER, "(member=%fqdn)");
        options.put(LDAPOptions.ROLE_NAME_ATTRIBUTE, "description");
        LDAPLoginModule module = new LDAPLoginModule();
        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("admin", "admin123"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(2, subject.getPrincipals().size());

        final List<String> roles = new ArrayList<>(Arrays.asList("karaf"));

        boolean foundUser = false;
        boolean foundRole = false;
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof UserPrincipal) {
                assertEquals("admin", principal.getName());
                foundUser = true;
            } else if (principal instanceof RolePrincipal) {
                assertTrue(roles.remove(principal.getName()));
                foundRole = true;
            }
        }
        assertTrue(foundUser);
        assertTrue(foundRole);
        assertTrue(roles.isEmpty());

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }
}
            
