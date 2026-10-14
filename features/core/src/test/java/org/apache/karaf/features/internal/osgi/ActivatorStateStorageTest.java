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
package org.apache.karaf.features.internal.osgi;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.apache.karaf.features.internal.service.State;
import org.apache.karaf.features.internal.service.StateStorage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;

public class ActivatorStateStorageTest {

    private static final String STATE_FILE = "state.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private StateStorage storage;

    @Before
    public void setUp() throws Exception {
        BundleContext bundleContext = mock(BundleContext.class);
        expect(bundleContext.getDataFile(anyString()))
                .andAnswer(() -> new File(folder.getRoot(), (String) getCurrentArguments()[0]))
                .anyTimes();
        replay(bundleContext);

        Method createStateStorage = Activator.class.getDeclaredMethod("createStateStorage");
        createStateStorage.setAccessible(true);
        storage = (StateStorage) createStateStorage.invoke(new StorageActivator(bundleContext));
    }

    @Test
    public void testSaveState() throws Exception {
        State state = state();

        storage.save(state);

        State loaded = new State();
        storage.load(loaded);
        assertEquals(state.requirements, loaded.requirements);
        assertEquals(state.installedFeatures, loaded.installedFeatures);
        assertFalse(new File(folder.getRoot(), STATE_FILE + ".tmp").exists());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testInterruptedSaveKeepsPreviousState() throws Exception {
        State state = state();
        storage.save(state);
        File file = new File(folder.getRoot(), STATE_FILE);
        byte[] saved = Files.readAllBytes(file.toPath());

        // A value JSON cannot write stands in for what interrupts a save in practice, the
        // process dying or the disk filling up part way through, which a unit test cannot
        // cause: the save fails after it has started writing.
        State unwritable = state.copy();
        ((Map) unwritable.requirements).put("unwritable", new Object());
        try {
            storage.save(unwritable);
            fail("A state holding a value JSON cannot write should fail to save");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        assertArrayEquals(saved, Files.readAllBytes(file.toPath()));
        State loaded = new State();
        storage.load(loaded);
        assertEquals(state.requirements, loaded.requirements);
        assertEquals(state.installedFeatures, loaded.installedFeatures);
    }

    private static State state() {
        State state = new State();
        state.requirements.put("root", Collections.singleton("feature:example/[1.0.0,1.0.0]"));
        state.installedFeatures.put("root", Collections.singleton("example/1.0.0"));
        return state;
    }

    private static class StorageActivator extends Activator {
        StorageActivator(BundleContext bundleContext) {
            this.bundleContext = bundleContext;
        }
    }
}
