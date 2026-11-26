/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileAccessGuardTest {

    private static final String SECRET = "s3cr3t-content";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final List<String> checked = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean deny;
    private ThreadIOImpl threadIO;
    private SessionFactoryImpl factory;
    private Path dir;
    private Path secret;

    @Before
    public void setUp() throws Exception {
        threadIO = new ThreadIOImpl();
        threadIO.start();
        factory = new SessionFactoryImpl(threadIO) {
            @Override
            protected void checkFileAccess(CommandSession session, String command, String location) {
                checked.add(command + " " + location);
                if (deny) {
                    throw new SecurityException("Insufficient credentials to access " + location);
                }
            }
        };
        dir = tmp.getRoot().toPath().toRealPath();
        secret = dir.resolve("secret.txt");
        Files.write(secret, (SECRET + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        factory.stop();
        threadIO.stop();
    }

    private String execute(String command) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true);
        Session session = factory.create(new ByteArrayInputStream(new byte[0]), ps, ps);
        try {
            Object result = session.execute(command);
            if (result != null) {
                ps.println(result);
            }
        } catch (Exception e) {
            ps.println(e);
        } finally {
            session.close();
        }
        return out.toString();
    }

    private String uri(Path path) {
        return path.toUri().toString();
    }

    @Test
    public void allowedFileIsCheckedThenRead() {
        assertTrue(execute("cat " + secret).contains(SECRET));
        assertEquals(Collections.singletonList("cat " + uri(secret)), checked);
    }

    @Test
    public void deniedFilesAreNotRead() {
        deny = true;
        for (String command : new String[] {"cat", "head", "tail", "wc -c", "sort", "less", "grep -i S3CR3T"}) {
            checked.clear();
            String output = execute(command + " " + secret);
            assertFalse(command + " read the file: " + output, output.contains(SECRET) || output.contains(" 15 "));
            assertTrue(command + ": " + output, output.contains("Insufficient credentials"));
            String name = command.split(" ")[0];
            // grep's pattern isn't a file
            assertEquals(Collections.singletonList(name + " " + uri(secret)), checked);
        }
    }

    @Test
    public void standardInputIsNotChecked() {
        deny = true;
        assertOutput("echo hello | grep --color=never hell", "hello");
        assertOutput("echo hello | grep --color=never hell -", "hello");
        assertOutput("echo hello | cat", "hello");
        assertOutput("echo hello | cat -", "hello");
        assertOutput("echo hello | head -n 1 -", "hello");
        assertOutput("echo hello | tail -n 1", "hello");
        assertOutput("echo hello | sort", "hello");
        assertOutput("echo hello | wc -l", "1");
        // less needs a terminal, which a headless session doesn't have: only check that it isn't refused
        String less = execute("echo hello | less");
        assertFalse(less, less.contains("Insufficient credentials"));
        assertOutput("echo hello | tac", "hello");
        assertEquals(Collections.emptyList(), checked);
    }

    private void assertOutput(String command, String expected) {
        String output = execute(command);
        assertTrue(command + ": " + output, output.contains(expected) && !output.contains("Insufficient credentials"));
    }

    @Test
    public void tacDoesNotWriteADeniedFile() {
        deny = true;
        Path target = dir.resolve("written.txt");
        String output = execute("echo hello | tac " + target);
        assertTrue(output, output.contains("Insufficient credentials"));
        assertFalse(Files.exists(target));
        assertEquals(Collections.singletonList("tac " + uri(target)), checked);

        checked.clear();
        execute("echo hello | tac -a " + target);
        assertFalse(Files.exists(target));
        assertEquals(Collections.singletonList("tac " + uri(target)), checked);
    }

    @Test
    public void lsAndCdAreChecked() {
        deny = true;
        String output = execute("shell:ls " + dir);
        assertFalse(output, output.contains("secret.txt"));
        assertEquals(Collections.singletonList("ls " + uri(dir)), checked);

        checked.clear();
        execute("shell:ls");
        assertEquals(1, checked.size());
        assertTrue(checked.get(0), checked.get(0).startsWith("ls file:"));

        checked.clear();
        execute("cd " + dir);
        assertEquals(Collections.singletonList("cd " + uri(dir)), checked);
    }

    @Test
    public void urlsAreCheckedBeforeTheyAreOpened() {
        deny = true;
        String output = execute("cat http://127.0.0.1:1/internal");
        assertTrue(output, output.contains("Insufficient credentials"));
        assertFalse(output, output.contains("ConnectException"));
        assertEquals(Collections.singletonList("cat http://127.0.0.1:1/internal"), checked);

        checked.clear();
        execute("sort file://" + secret);
        assertEquals(Collections.singletonList("sort " + uri(secret)), checked);
    }

    @Test
    public void relativeLocationsAreResolvedAndNormalized() throws Exception {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        String output = execute("cd " + sub + " ; cat ../secret.txt ; head ../sub/../secret.txt");
        assertTrue(output, output.contains(SECRET));
        assertEquals(List.of("cd " + uri(sub), "cat " + uri(secret), "head " + uri(secret)), checked);
    }

    @Test
    public void commandsWithoutFilesAreNotChecked() {
        deny = true;
        assertTrue(execute("echo " + secret).contains(secret.toString()));
        execute("pwd");
        execute("date");
        assertEquals(Collections.emptyList(), checked);
        assertTrue(new File(secret.toString()).exists());
    }
}
