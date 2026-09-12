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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class PathUtilsTest {

    @Test
    public void childInsideParentIsAccepted() throws Exception {
        Path parent = Files.createTempDirectory("pathutils");
        File child = new File(parent.toFile(), "sub/child.cfg");
        assertTrue(PathUtils.isWithin(parent.toFile(), child));
        PathUtils.checkWithin(parent.toFile(), child);
    }

    @Test
    public void relativeTraversalIsRejected() throws Exception {
        Path parent = Files.createTempDirectory("pathutils");
        File child = new File(parent.toFile(), "../escape.cfg");
        assertFalse(PathUtils.isWithin(parent.toFile(), child));
        try {
            PathUtils.checkWithin(parent.toFile(), child);
            fail("expected traversal to be rejected");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void absolutePathOutsideParentIsRejected() throws Exception {
        Path parent = Files.createTempDirectory("pathutils");
        File child = new File(System.getProperty("java.io.tmpdir"), "definitely-not-under-parent.cfg");
        assertFalse(PathUtils.isWithin(parent.toFile(), child));
    }

    @Test
    public void siblingWithSharedPrefixIsRejected() throws Exception {
        Path base = Files.createTempDirectory("pathutils");
        File parent = new File(base.toFile(), "etc");
        File child = new File(base.toFile(), "etc-evil/child.cfg");
        assertFalse(PathUtils.isWithin(parent, child));
    }
}
