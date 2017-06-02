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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
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
import org.apache.karaf.jaas.modules.ldap.LdapLoginModuleTest;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP")})
@CreateDS(name = "LdapSpecialCharsInPasswordTest-class",
 partitions = { @CreatePartition(name = "example", suffix = "dc=example,dc=com") })
@ApplyLdifFiles(
   "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapSpecialCharsInPasswordTest extends LdapLoginModuleTest {
    
    private static boolean portUpdated;
    private static final String NEW_CONNECTION_PASSWORD = "#a&b{>c=<12~d%";
    
    @Before
    @Override
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // Read in ldap.properties and substitute in the correct port
            File f = new File(basedir + "/src/test/resources/org/apache/karaf/jaas/modules/ldap/ldap_special_char_in_password.properties");

            FileInputStream inputStream = new FileInputStream(f);
            String content = IOUtils.toString(inputStream, "UTF-8");
            inputStream.close();
            content = content.replaceAll("portno", "" + getLdapServer().getPort());

            File f2 = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldap_special_char_in_password.properties");
            FileOutputStream outputStream = new FileOutputStream(f2);
            IOUtils.write(content, outputStream, "UTF-8");
            outputStream.close();
            portUpdated = true;
        }
    }

    protected Properties ldapLoginModuleOptions() throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/ldap_special_char_in_password.properties");
        return new Properties(file);
    }
    
    @Before
    public void changeAdminPassword() throws Exception {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        connection.bind( "uid=admin,ou=system", "secret");
        Dn adminDn = new Dn( "uid=admin,ou=system" );
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( adminDn );
        modReq.replace( SchemaConstants.USER_PASSWORD_AT, NEW_CONNECTION_PASSWORD );
        connection.modify( modReq );
        connection.close();
        
        // check that we actually changed the admin connection password
        connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        connection.bind( "uid=admin,ou=system", NEW_CONNECTION_PASSWORD);
        connection.close();
    }
}
            