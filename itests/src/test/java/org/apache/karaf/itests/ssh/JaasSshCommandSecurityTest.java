/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests.ssh;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * This test exercises the Shell Command ACL for the jaas scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.jaas.cfg
 */
public class JaasSshCommandSecurityTest extends SshCommandTestBase {
    
        
    @Test
    public void testJaasCommandSecurityViaSsh() throws Exception {
        String vieweruser = "viewer" + System.nanoTime() + "_jaas";

        addViewer(vieweruser);

        String userName = "XXX" + System.nanoTime();
        assertCommand(vieweruser, "jaas:realm-manage --realm karaf;" +
        		"jaas:user-add " + userName + " pwd;" +
				"jaas:update", Result.NOT_FOUND);
        String r = assertCommand(vieweruser, "jaas:realm-manage --realm karaf;" +
				"jaas:user-list", Result.OK);
        assertFalse("The viewer should not have the credentials to add the new user",
                r.contains(userName));

        assertCommand("karaf", "jaas:realm-manage --realm karaf;" +
                "jaas:user-add " + userName + " pwd;" +
                "jaas:update", Result.OK);
        String r2 = assertCommand(vieweruser, "jaas:realm-manage --realm karaf;" +
                "jaas:user-list", Result.OK);
        assertTrue("The admin user should have the rights to add the new user",
                r2.contains(userName));
    }
}
