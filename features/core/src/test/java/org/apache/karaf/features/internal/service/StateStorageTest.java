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
package org.apache.karaf.features.internal.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StateStorageTest {

    @Test
    public void testStoreLoad() throws Exception {

        State oldState = new State();
        oldState.bootDone.set(true);
        oldState.bundleChecksums.put(4l, 32794l);
        oldState.requirements.put("bar", Collections.singleton("f1"));
        oldState.managedBundles.put("reg", Collections.singleton(32l));
        oldState.managedBundles.put("reg2", new HashSet<>(Arrays.asList(24l, 43l)));
        oldState.repositories.add("repo");

        TestStorage storage = new TestStorage();

        storage.save(oldState);

        System.out.println(storage.baos.toString());

        State newState = new State();
        storage.load(newState);

        assertEquals(oldState.bootDone.get(), newState.bootDone.get());
        assertEquals(oldState.bundleChecksums, newState.bundleChecksums);
        assertEquals(oldState.requirements, newState.requirements);
        assertEquals(oldState.managedBundles, newState.managedBundles);
        assertEquals(oldState.repositories, newState.repositories);
    }

    static class TestStorage extends StateStorage {
        ByteArrayOutputStream baos;

        @Override
        protected InputStream getInputStream() throws IOException {
            if (baos != null) {
                return new ByteArrayInputStream(baos.toByteArray());
            }
            return null;
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            baos = new ByteArrayOutputStream();
            return baos;
        }
    }


}
