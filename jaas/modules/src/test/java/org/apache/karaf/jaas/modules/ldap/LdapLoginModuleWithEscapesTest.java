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

import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.junit.runner.RunWith;

@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP", port=9999)})
@CreateDS(name = "LdapLoginModuleWithEscapesTest-class",
 partitions = { @CreatePartition(name = "example", suffix = "dc=example,dc=com") })
@ApplyLdifFiles(
   "org/apache/karaf/jaas/modules/ldap/example.com_with_escapes.ldif"
)
public class LdapLoginModuleWithEscapesTest extends LdapLoginModuleTest {
}
            