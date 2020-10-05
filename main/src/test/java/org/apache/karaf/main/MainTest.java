/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.karaf.main.ConfigProperties.PROP_KARAF_BASE;
import static org.apache.karaf.main.ConfigProperties.PROP_KARAF_HOME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MainTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Ensures "clean" arg is supported by karaf main.
     *
     * Impl note: since it is the first part of the main we just call launch but skip the execution thanks to ConfigProperties hack.
     */
    @Test
    public void ensureDataCanBeDelete() throws Exception {
        final Path data = temporaryFolder.getRoot().toPath().resolve("data");
        final Path child = data.resolve("child1").resolve("child2");
        Files.createDirectories(child);
        final Path foo = child.resolve("foo.txt");
        Files.write(foo, new byte[0]);

        // here foo exists
        assertTrue(Files.exists(foo));

        final Main main = new Main(new String[]{"clean"});
        assertTrue(Files.exists(foo));

        final Path base = new File(getClass().getClassLoader().getResource("foo").getPath()).toPath().getParent().resolve("test-karaf-home");
        final Collection<String> props = asList(PROP_KARAF_HOME, PROP_KARAF_BASE);
        System.setProperty("org.osgi.framework.startlevel.beginning", "0");
        System.setProperty("karaf.framework", "test");
        System.setProperty("karaf.framework.test", "test");
        System.setProperty("karaf.data", data.toString());
        props.forEach(k -> System.setProperty(k, base.normalize().toAbsolutePath().toString()));
        main.setConfig(new ConfigProperties() { // just to test clean phase, not the rest
            @Override
            public void performInit() throws Exception {
                throw new EagerExit();
            }
        });
        try {
            main.launch();
            fail();
        } catch (final EagerExit ee) {
            // expected
        } finally {
            props.forEach(System::clearProperty);
            System.clearProperty("org.osgi.framework.startlevel.beginning");
            System.clearProperty("karaf.data");
            System.clearProperty("karaf.framework");
            System.clearProperty("karaf.framework.test");
        }
        assertFalse(Files.exists(foo));
        assertFalse(Files.exists(child));
        assertTrue(Files.exists(data.resolve("tmp")));
    }

    public static class EagerExit extends Exception {}

}