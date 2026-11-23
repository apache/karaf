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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Assume;
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

    @Test
    public void testShellReflectionAndRedirectSecurityViaSsh() throws Exception {
        // Skip on Windows where PTY output can be garbled,
        // when upgrading to Junit5, this can be replaced with @DisabledOnOs(OS.WINDOWS)
        Assume.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"));

        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // Reflection (.invoke) must be restricted to admin
        assertCommand(vieweruser, "($.session class)", Result.NOT_FOUND);
        assertCommand(vieweruser, "((($.session class) classLoader) loadClass java.lang.Runtime)", Result.NOT_FOUND);
        assertCommand(manageruser, "($.session class)", Result.NOT_FOUND);

        // addCommand / removeCommand / alias --persist bypasses must also be restricted to admin
        assertCommand(vieweruser, "shell:addCommand evil $.context installBundle", Result.NOT_FOUND);
        assertCommand(vieweruser, "shell:removeCommand shell echo", Result.NOT_FOUND);
        assertCommand(vieweruser, "shell:alias -p \"tmp = { shell:date }\"", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "shell:alias --persist \"tmp = { shell:date }\"", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "shell:alias \"tmp = { shell:date }\"", Result.OK);

        assertCommand(manageruser, "shell:addCommand evil $.context installBundle", Result.NOT_FOUND);
        assertCommand(manageruser, "shell:removeCommand shell echo", Result.NOT_FOUND);
        assertCommand(manageruser, "shell:alias -p \"tmp = { shell:date }\"", Result.NO_CREDENTIALS);

        String adminReflect = assertCommand("karaf", "($.session class)", Result.OK);
        Assert.assertTrue("Admin should be able to invoke methods via reflection: " + adminReflect,
                adminReflect.contains("ConsoleSessionImpl"));

        // Redirection (.redirect) must be restricted to admin
        Path tempDir = Files.createTempDirectory("karaf-shell-redirect-test");
        Path targetFile = tempDir.resolve("redirect-test.txt");
        try {
            assertCommand(vieweruser, "echo VIEWER_REDIRECT > " + targetFile.toAbsolutePath(), Result.NOT_FOUND);
            Assert.assertFalse("Viewer must not create file via redirection", Files.exists(targetFile));

            assertCommand(vieweruser, "echo VIEWER_APPEND >> " + targetFile.toAbsolutePath(), Result.NOT_FOUND);
            Assert.assertFalse("Viewer must not create file via append redirection", Files.exists(targetFile));

            assertCommand(manageruser, "echo MANAGER_REDIRECT > " + targetFile.toAbsolutePath(), Result.NOT_FOUND);
            Assert.assertFalse("Manager must not create file via redirection", Files.exists(targetFile));

            assertCommand("karaf", "echo ADMIN_REDIRECT_OK > " + targetFile.toAbsolutePath(), Result.OK);
            Assert.assertTrue("Admin should be able to write file via redirection", Files.exists(targetFile));
            Assert.assertTrue("Redirected file should contain expected output",
                    Files.readString(targetFile).contains("ADMIN_REDIRECT_OK"));
        } finally {
            Files.deleteIfExists(targetFile);
            Files.deleteIfExists(tempDir);
        }
    }
}
