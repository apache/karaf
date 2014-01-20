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
package org.apache.karaf.itests;

import org.junit.Assert;
import org.junit.Test;

/**
 * This test exercises the Shell Command ACL for the feature scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.feature.cfg
 */
public class FeatureSshCommandSecurityTest extends SshCommandTestBase {
    @Test
    public void testFeatureCommandSecurityViaSsh() throws Exception {
        String vieweruser = "viewer" + System.nanoTime() + "_features";

        addViewer(vieweruser);

        String r = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("Precondition failed, this test uses the eventadmin subsystem to test features with...",
                r.contains("eventadmin"));

        assertCommand(vieweruser, "feature:install eventadmin", Result.NOT_FOUND);
        String r2 = assertCommand("karaf", "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("eventadmin features should not have been installed, as viewer doesn't have credentials",
                r2.contains("eventadmin"));

        assertCommand("karaf", "feature:install eventadmin", Result.OK);
        String r3 = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertTrue("eventadmin feature should have been installed by 'karaf' user",
                r3.contains("eventadmin"));

        assertCommand(vieweruser, "feature:uninstall eventadmin", Result.NOT_FOUND);
        String r4 = assertCommand("karaf", "feature:list -i --no-format", Result.OK);
        Assert.assertTrue("eventadmin feature should still be there, as viewer doesn't have credentials",
                r4.contains("eventadmin"));

        assertCommand("karaf", "feature:uninstall eventadmin", Result.OK);
        String r5 = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("The eventadmin subsystem should have been uninstalled",
                r5.contains("eventadmin"));
    }
}
