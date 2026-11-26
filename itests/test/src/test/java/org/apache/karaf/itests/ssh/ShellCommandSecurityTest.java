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

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.jline.keymap.KeyMap.ctrl;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testFileBuiltinsViaSsh() throws Exception {
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addViewer(vieweruser);

        // A viewer can't read, list or write files and URLs with the built-ins, or run a script from a file
        assertCommand(vieweruser, "cat etc/users.properties", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "grep karaf etc/users.properties", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "head etc/host.key", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "shell:ls etc", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "cat http://127.0.0.1:1/", Result.NO_CREDENTIALS);
        File written = new File(System.getProperty("karaf.data"), vieweruser + ".txt");
        assertCommand(vieweruser, "echo foo | tac " + written.getAbsolutePath(), Result.NO_CREDENTIALS);
        assertFalse(written.exists());
        assertCommand(vieweruser, "shell:source etc/users.properties", Result.NOT_FOUND);

        // Standard input is open to every user
        // -n prefixes the line number, so that the match can't be confused with the echoed command line
        String output = assertCommand(vieweruser, "echo hello-from-viewer | grep -n --color=never viewer", Result.OK);
        assertTrue(output, output.contains("1:hello-from-viewer"));
        assertCommand(vieweruser, "feature:list | grep -i shell | wc -l", Result.OK);

        output = assertCommand("karaf", "cat etc/users.properties", Result.OK);
        assertTrue(output, output.contains("admingroup"));
        assertCommand("karaf", "shell:ls etc", Result.OK);
    }
}
