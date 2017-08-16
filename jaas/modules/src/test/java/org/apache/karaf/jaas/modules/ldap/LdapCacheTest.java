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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

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
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapLoginModuleTest-class",
        partitions = {@CreatePartition(name = "example", suffix = "dc=example,dc=com")})
@ApplyLdifFiles(
        "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapCacheTest extends AbstractLdapTestUnit {

    @Before
    public void updatePort() throws Exception {
        ldapProps("org/apache/karaf/jaas/modules/ldap/ldap.properties", 
                  LdapLoginModuleTest::replacePort);
    }

    @After
    public void tearDown() {
        LDAPCache.clear();
    }

    @Test
    public void testAdminLogin() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
        CallbackHandler cb = new NamePasswordCallbackHandler("admin", "admin123");
        Subject subject = new Subject();
        module.initialize(subject, cb, null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(2, subject.getPrincipals().size());

        assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("admin"));
        assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("admin"));

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());

        LDAPCache ldapCache = new LDAPCache(new LDAPOptions(options));
        DirContext context = ldapCache.open();
        addUserToGroup(context, "cn=admin,ou=people,dc=example,dc=com", "another");
        ldapCache.close();

        Thread.sleep(100);

        module = new LDAPLoginModule();
        subject = new Subject();
        module.initialize(subject, cb, null, options);
        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());
        assertEquals("Postcondition", 3, subject.getPrincipals().size());
    }

    private void addUserToGroup(DirContext context, String userCn, String group) throws NamingException {
        Attributes entry = new BasicAttributes();
        entry.put(new BasicAttribute("cn", group));
        Attribute oc = new BasicAttribute("objectClass");
        oc.add("top");
        oc.add("groupOfNames");
        entry.put(oc);
        Attribute mb = new BasicAttribute("member");
        mb.add(userCn);
        entry.put(mb);
        context.createSubcontext("cn=" + group +",ou=groups,dc=example,dc=com", entry);
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
