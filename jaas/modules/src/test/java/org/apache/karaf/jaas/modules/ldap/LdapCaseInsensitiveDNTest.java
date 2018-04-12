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

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.apache.karaf.jaas.modules.ldap.LdapPropsUpdater.ldapProps;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.security.auth.Subject;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapCaseInsensitiveDNTest-class",
 partitions = { @CreatePartition(name = "example", suffix = "dc=example,dc=com") })
@ApplyLdifFiles(
   "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapCaseInsensitiveDNTest extends LdapLoginModuleTest {
    
    @Before
    @Override
    public void updatePort() throws Exception {
        ldapProps("org/apache/karaf/jaas/modules/ldap/ldapCaseInsensitiveDN.properties", 
                  LdapLoginModuleTest::replacePort);
    }
    
    @Test
    public void testCaseInsensitiveDN() throws Exception {
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
        File file = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldapCaseInsensitiveDN.properties");
        return new Properties(file);
    }
}
            