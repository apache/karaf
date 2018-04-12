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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
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
import org.apache.karaf.jaas.modules.NamePubkeyCallbackHandler;
import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.apache.karaf.jaas.modules.ldap.LdapPropsUpdater.ldapProps;
import org.apache.log4j.Level;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.containsInAnyOrder;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
    @CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapPubkeyLoginModuleTest-class",
        partitions = {
            @CreatePartition(name = "example", suffix = "dc=example,dc=com")})
@ApplyLdifFiles(
        "org/apache/karaf/jaas/modules/ldap/example.com_pubkey.ldif"
)
public class LDAPPubkeyLoginModuleTest extends AbstractLdapTestUnit {

    private static final String LDAP_PROPERTIES_FILE = "org/apache/karaf/jaas/modules/ldap/ldap_pubkey.properties";

    @Before
    public void updatePort() throws Exception {
        ldapProps(LDAP_PROPERTIES_FILE, LdapLoginModuleTest::replacePort);
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
        LDAPPubkeyLoginModule module = new LDAPPubkeyLoginModule();
        Subject subject = new Subject();
        Path pubkeyFile = srcTestResourcePath("org/apache/karaf/jaas/modules/ldap/ldaptest.admin.id_rsa");
        module.initialize(subject, new NamePubkeyCallbackHandler("admin", pubkeyFile), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(2, subject.getPrincipals().size());
        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("admin"));

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test
    public void testNonAdminLogin() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPPubkeyLoginModule module = new LDAPPubkeyLoginModule();
        Subject subject = new Subject();
        Path pubkeyFile = srcTestResourcePath("org/apache/karaf/jaas/modules/ldap/ldaptest.cheese.id_rsa");
        module.initialize(subject, new NamePubkeyCallbackHandler("cheese", pubkeyFile), null, options);

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
    public void testBadPrivateKey() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPPubkeyLoginModule module = new LDAPPubkeyLoginModule();
        Subject subject = new Subject();
        Path pubkeyFile = srcTestResourcePath("org/apache/karaf/jaas/modules/ldap/ldaptest.cheese.id_rsa");
        module.initialize(subject, new NamePubkeyCallbackHandler("admin", pubkeyFile), null, options);

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
        LDAPPubkeyLoginModule module = new LDAPPubkeyLoginModule();
        Subject subject = new Subject();
        Path pubkeyFile = srcTestResourcePath("org/apache/karaf/jaas/modules/ldap/ldaptest.admin.id_rsa");
        module.initialize(subject, new NamePubkeyCallbackHandler("imnothere", pubkeyFile), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertFalse(module.login());
    }

    private Path srcTestResourcePath(String relativePath) throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        Path pubkeyFile = Paths.get(basedir, "/src/test/resources/", relativePath);
        return pubkeyFile;
    }

    protected Properties ldapLoginModuleOptions() throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/" + LDAP_PROPERTIES_FILE);
        return new Properties(file);
    }

}
