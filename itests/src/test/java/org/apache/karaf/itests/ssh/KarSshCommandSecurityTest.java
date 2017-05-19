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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the kar scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.kar.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)

public class KarSshCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;
        
    @Test
    public void testKarCommandSecurityViaSsh() throws Exception {
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addViewer(vieweruser);

        assertCommand(vieweruser, "kar:list", Result.OK);
        assertCommand(vieweruser, "kar:install", Result.NOT_FOUND);
        assertCommand(vieweruser, "kar:uninstall", Result.NOT_FOUND);

        assertCommand("karaf", "kar:list", Result.OK);
        assertCommand("karaf", "kar:install", Result.OK);
        assertCommand("karaf", "kar:uninstall", Result.OK);
    }
}
