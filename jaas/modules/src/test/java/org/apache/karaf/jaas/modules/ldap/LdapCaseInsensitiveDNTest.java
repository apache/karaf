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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.ldap.LdapLoginModuleTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP", port=9999)})
@CreateDS(name = "LdapCaseInsensitiveDNTest-class",
 partitions = { @CreatePartition(name = "example", suffix = "dc=example,dc=com") })
@ApplyLdifFiles(
   "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapCaseInsensitiveDNTest extends LdapLoginModuleTest {
    
    @Test
    public void testCaseInsensitiveDN() throws Exception {
        Properties options = ldapLoginModuleOptions();
        LDAPLoginModule module = new LDAPLoginModule();
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

    protected Properties ldapLoginModuleOptions() throws IOException {
        return new Properties(new File("src/test/resources/org/apache/karaf/jaas/modules/ldap/ldapCaseInsensitiveDN.properties"));
    }
}
            