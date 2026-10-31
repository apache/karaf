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
package org.apache.karaf.features.internal.region;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.easymock.EasyMock;
import org.eclipse.equinox.region.RegionDigraph;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DigraphHelperTest {

    private static final String EMPTY_DIGRAPH = "{\"regions\":{},\"edges\":[]}";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSaveDigraph() throws Exception {
        File file = new File(folder.getRoot(), DigraphHelper.DIGRAPH_FILE);
        Files.write(file.toPath(), "previous".getBytes(StandardCharsets.UTF_8));

        RegionDigraph digraph = EasyMock.createMock(RegionDigraph.class);
        EasyMock.expect(digraph.getRegions()).andReturn(Collections.emptySet()).anyTimes();
        EasyMock.replay(digraph);

        DigraphHelper.saveDigraph(file, digraph);

        assertEquals(EMPTY_DIGRAPH, Files.readString(file.toPath()));
        assertArrayEquals(new String[] { DigraphHelper.DIGRAPH_FILE }, folder.getRoot().list());
    }

    @Test
    public void testInterruptedSaveKeepsPreviousDigraph() throws Exception {
        File file = new File(folder.getRoot(), DigraphHelper.DIGRAPH_FILE);
        Files.write(file.toPath(), EMPTY_DIGRAPH.getBytes(StandardCharsets.UTF_8));

        // A digraph that fails while it is read stands in for what interrupts a save in
        // practice, the process dying or the disk filling up part way through, which a unit
        // test cannot cause: the save fails after it has opened the file it writes.
        RegionDigraph digraph = EasyMock.createMock(RegionDigraph.class);
        EasyMock.expect(digraph.getRegions()).andThrow(new IllegalStateException("digraph changed while saving")).anyTimes();
        EasyMock.replay(digraph);

        DigraphHelper.saveDigraph(file, digraph);

        assertEquals(EMPTY_DIGRAPH, Files.readString(file.toPath()));
    }
}
