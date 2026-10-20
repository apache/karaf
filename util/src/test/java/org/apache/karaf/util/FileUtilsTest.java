/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testAtomicSave() throws Exception {
        File file = new File(folder.getRoot(), "state.json");
        Files.write(file.toPath(), "previous".getBytes(StandardCharsets.UTF_8));

        FileUtils.atomicSave(file, out -> out.write("new".getBytes(StandardCharsets.UTF_8)));

        assertEquals("new", Files.readString(file.toPath()));
        assertArrayEquals(new String[] { "state.json" }, folder.getRoot().list());
    }

    @Test
    public void testInterruptedAtomicSaveKeepsPreviousContent() throws Exception {
        File file = new File(folder.getRoot(), "state.json");
        Files.write(file.toPath(), "previous".getBytes(StandardCharsets.UTF_8));

        // A writer that fails part way through stands in for what interrupts a save in
        // practice, the process dying or the disk filling up, which a unit test cannot cause
        try {
            FileUtils.atomicSave(file, out -> {
                out.write("partial".getBytes(StandardCharsets.UTF_8));
                throw new IOException("disk full");
            });
            fail("A write that fails should fail the save");
        } catch (IOException e) {
            // Expected
        }

        assertEquals("previous", Files.readString(file.toPath()));
        assertArrayEquals(new String[] { "state.json" }, folder.getRoot().list());
    }

    @Test
    public void testOverlappingAtomicSavesKeepTheirOwnContent() throws Exception {
        File file = new File(folder.getRoot(), "state.json");

        // A save made while another save of the same file is still writing stands in for two
        // threads saving it at once, whose timing a unit test cannot control
        FileUtils.atomicSave(file, out -> {
            out.write("first".getBytes(StandardCharsets.UTF_8));
            FileUtils.atomicSave(file, other -> other.write("second".getBytes(StandardCharsets.UTF_8)));
            assertEquals("second", Files.readString(file.toPath()));
            out.write(" save".getBytes(StandardCharsets.UTF_8));
        });

        assertEquals("first save", Files.readString(file.toPath()));
        assertArrayEquals(new String[] { "state.json" }, folder.getRoot().list());
    }
}
