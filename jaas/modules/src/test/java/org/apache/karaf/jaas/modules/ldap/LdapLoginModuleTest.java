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
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.apache.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.apache.karaf.jaas.modules.ldap.LdapPropsUpdater.ldapProps;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

    @Before
    public void updatePort() throws Exception {
        ldapProps("org/apache/karaf/jaas/modules/ldap/ldap.properties",
                                   LdapLoginModuleTest::replacePort);
    }

    public static String replacePort(String line) {
        return line.replaceAll("portno", "" + getLdapServer().getPort());
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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("admin"));

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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("cheese"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), Matchers.empty());

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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("cheese"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), Matchers.empty());

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
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LDAPLoginModule.class);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            module.login();
            fail("Should have thrown LoginException");
        } catch (LoginException e) {
            assertTrue(e.getMessage().startsWith("Authentication failed"));
        } finally {
            logger.setLevel(oldLevel);
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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("karaf"));

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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("karaf", "test", "another"));

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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("karaf", "test", "another"));

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
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("karaf"));

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }
}
            
