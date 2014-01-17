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
 * /framework/src/main/resources/resources/etc/org.apache.karaf.feature.acl.config.cfg
 */
public class FeatureSshCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;
    @Test
    public void testFeatureCommandSecurityViaSsh() throws Exception {
        String vieweruser = "viewer" + System.nanoTime() + "_" + counter++;

        addViewer(vieweruser);

        String r = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("Precondition failed, this test uses the transaction subsystem to test features with...",
                r.contains("transaction"));

        assertCommand(vieweruser, "feature:install transaction", Result.NOT_FOUND);
        String r2 = assertCommand("karaf", "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("Transaction features should not have been installed, as viewer doesn't have credentials",
                r2.contains("transaction"));

        assertCommand("karaf", "feature:install transaction", Result.OK);
        String r3 = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertTrue("Transaction feature should have been installed by 'karaf' user",
                r3.contains("transaction"));

        assertCommand(vieweruser, "feature:uninstall transaction", Result.NOT_FOUND);
        String r4 = assertCommand("karaf", "feature:list -i --no-format", Result.OK);
        Assert.assertTrue("Transaction feature should still be there, as viewer doesn't have credentials",
                r4.contains("transaction"));

        assertCommand("karaf", "feature:uninstall transaction", Result.OK);
        String r5 = assertCommand(vieweruser, "feature:list -i --no-format", Result.OK);
        Assert.assertFalse("The transaction subsystem should have been uninstalled",
                r5.contains("transaction"));
    }
}
