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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.internal.util.JsonReader;
import org.apache.karaf.features.internal.util.JsonWriter;

public abstract class StateStorage {

    public void load(State state) throws IOException {
        state.repositories.clear();
        state.features.clear();
        state.installedFeatures.clear();
        state.managedBundles.clear();
        InputStream is = getInputStream();
        if (is != null) {
            try {
                Map json = (Map) JsonReader.read(is);
                state.bootDone.set((Boolean) json.get("bootDone"));
                state.repositories.addAll(toStringSet((Collection) json.get("repositories")));
                state.features.putAll(toStringStringSetMap((Map) json.get("features")));
                state.installedFeatures.putAll(toStringStringSetMap((Map) json.get("installed")));
                state.managedBundles.putAll(toStringLongSetMap((Map) json.get("managed")));
                state.bundleChecksums.putAll(toLongLongMap((Map) json.get("checksums")));
            } finally {
                close(is);
            }
        }
    }

    public void save(State state) throws IOException {
        OutputStream os = getOutputStream();
        if (os != null) {
            try {
                Map<String, Object> json = new HashMap<String, Object>();
                json.put("bootDone", state.bootDone.get());
                json.put("repositories", state.repositories);
                json.put("features", state.features);
                json.put("installed", state.installedFeatures);
                json.put("managed", state.managedBundles);
                json.put("checksums", toStringLongMap(state.bundleChecksums));
                JsonWriter.write(os, json);
            } finally {
                close(os);
            }
        }
    }

    protected abstract InputStream getInputStream() throws IOException;
    protected abstract OutputStream getOutputStream() throws IOException;

    protected Map<String, Set<String>> toStringStringSetMap(Map<?,?> map) {
        Map<String, Set<String>> nm = new HashMap<String, Set<String>>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toStringSet((Collection) entry.getValue()));
        }
        return nm;
    }

    protected Map<String, Set<Long>> toStringLongSetMap(Map<?,?> map) {
        Map<String, Set<Long>> nm = new HashMap<String, Set<Long>>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toLongSet((Collection) entry.getValue()));
        }
        return nm;
    }

    protected Set<String> toStringSet(Collection<?> col) {
        Set<String> ns = new TreeSet<String>();
        for (Object o : col) {
            ns.add(o.toString());
        }
        return ns;
    }

    protected Set<Long> toLongSet(Collection<?> set) {
        Set<Long> ns = new TreeSet<Long>();
        for (Object o : set) {
            ns.add(toLong(o));
        }
        return ns;
    }

    protected Map<Long, Long> toLongLongMap(Map<?,?> map) {
        Map<Long, Long> nm = new HashMap<Long, Long>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(toLong(entry.getKey()), toLong(entry.getValue()));
        }
        return nm;
    }

    protected Map<String, Long> toStringLongMap(Map<?,?> map) {
        Map<String, Long> nm = new HashMap<String, Long>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toLong(entry.getValue()));
        }
        return nm;
    }

    static long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        } else {
            return Long.parseLong(o.toString());
        }
    }

    static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
