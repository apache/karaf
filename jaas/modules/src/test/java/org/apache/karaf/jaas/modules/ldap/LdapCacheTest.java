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

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;

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
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapLoginModuleTest-class",
        partitions = {@CreatePartition(name = "example", suffix = "dc=example,dc=com")})
@ApplyLdifFiles(
        "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapCacheTest extends AbstractLdapTestUnit {

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
            content = content.replaceAll("portno", "" + getLdapServer().getPort());

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
        CallbackHandler cb = new NamePasswordCallbackHandler("admin", "admin123");
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

        DirContext context = new LDAPCache(new LDAPOptions(options)).open();

        // Make "admin" user a member of a new "another" group

//        dn: cn=admin,ou=groups,dc=example,dc=com
//        objectClass: top
//        objectClass: groupOfNames
//        cn: admin
//        member: cn=admin,ou=people,dc=example,dc=com
        Attributes entry = new BasicAttributes();
        entry.put(new BasicAttribute("cn", "another"));
        Attribute oc = new BasicAttribute("objectClass");
        oc.add("top");
        oc.add("groupOfNames");
        entry.put(oc);
        Attribute mb = new BasicAttribute("member");
        mb.add("cn=admin,ou=people,dc=example,dc=com");
        entry.put(mb);
        context.createSubcontext("cn=another,ou=groups,dc=example,dc=com", entry);

        Thread.sleep(100);

        module = new LDAPLoginModule();
        subject = new Subject();
        module.initialize(subject, cb, null, options);
        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());
        assertEquals("Postcondition", 3, subject.getPrincipals().size());
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
