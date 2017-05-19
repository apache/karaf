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

import static org.jline.keymap.KeyMap.ctrl;

/**
 * This test exercises the Shell Command ACL for the shell scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.shell.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ShellCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;
        
    @Test
    public void testShellCommandSecurityViaSsh() throws Exception {
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addViewer(vieweruser);

        assertCommand(vieweruser, "shell:date", Result.OK);
        assertCommand(vieweruser, "shell:nano", Result.NOT_FOUND);
        assertCommand(vieweruser, "shell:exec", Result.NOT_FOUND);
        assertCommand(vieweruser, "shell:new", Result.NOT_FOUND);
        assertCommand(vieweruser, "shell:java", Result.NOT_FOUND);

        assertCommand("karaf", "shell:date", Result.OK);
        assertCommand("karaf", "shell:nano\n" + ctrl('X'), Result.OK);
        assertCommand("karaf", "shell:exec", Result.OK);
        assertCommand("karaf", "shell:new", Result.OK);
        assertCommand("karaf", "shell:java", Result.OK);
    }
}
