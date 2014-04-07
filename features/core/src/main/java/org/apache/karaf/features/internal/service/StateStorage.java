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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.Feature;

public abstract class StateStorage {

    public void load(State state) throws IOException {
        state.repositories.clear();
        state.features.clear();
        state.installedFeatures.clear();
        state.managedBundles.clear();
        InputStream is = getInputStream();
        if (is != null) {
            try {
                Properties props = new Properties();
                props.load(is);
                state.bootDone.set(loadBool(props, "bootDone"));
                state.repositories.addAll(loadSet(props, "repositories."));
                state.features.addAll(loadSet(props, "features."));
                state.installedFeatures.addAll(loadSet(props, "installed."));
                state.managedBundles.addAll(toLongSet(loadSet(props, "managed.")));
                state.bundleChecksums.putAll(toStringLongMap(loadMap(props, "checksums.")));
            } finally {
                close(is);
            }
        }
    }

    public void save(State state) throws IOException {
        OutputStream os = getOutputStream();
        if (os != null) {
            try {
                Properties props = new Properties();
                saveBool(props, "bootDone", state.bootDone.get());
                saveSet(props, "repositories.", state.repositories);
                saveSet(props, "features.", state.features);
                saveSet(props, "installed.", state.installedFeatures);
                saveSet(props, "managed.", toStringSet(state.managedBundles));
                saveMap(props, "checksums.", toStringStringMap(state.bundleChecksums));
                props.store(os, "FeaturesService State");
            } finally {
                close(os);
            }
        }
    }

    protected abstract InputStream getInputStream() throws IOException;
    protected abstract OutputStream getOutputStream() throws IOException;

    protected boolean loadBool(Properties props, String key) {
        return Boolean.parseBoolean(props.getProperty(key));
    }

    protected void saveBool(Properties props, String key, boolean val) {
        props.setProperty(key, Boolean.toString(val));
    }

    protected Set<String> toStringSet(Set<Long> set) {
        Set<String> ns = new TreeSet<String>();
        for (long l : set) {
            ns.add(Long.toString(l));
        }
        return ns;
    }

    protected Set<Long> toLongSet(Set<String> set) {
        Set<Long> ns = new TreeSet<Long>();
        for (String s : set) {
            ns.add(Long.parseLong(s));
        }
        return ns;
    }

    protected void saveSet(Properties props, String prefix, Set<String> set) {
        List<String> l = new ArrayList<String>(set);
        props.put(prefix + "count", Integer.toString(l.size()));
        for (int i = 0; i < l.size(); i++) {
            props.put(prefix + "item." + i, l.get(i));
        }
    }

    protected Set<String> loadSet(Properties props, String prefix) {
        Set<String> l = new HashSet<String>();
        String countStr = (String) props.get(prefix + "count");
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                l.add((String) props.get(prefix + "item." + i));
            }
        }
        return l;
    }

    protected Map<String, String> toStringStringMap(Map<String, Long> map) {
        Map<String, String> nm = new HashMap<String, String>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            nm.put(entry.getKey(), Long.toString(entry.getValue()));
        }
        return nm;
    }

    protected Map<String, Long> toStringLongMap(Map<String, String> map) {
        Map<String, Long> nm = new HashMap<String, Long>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            nm.put(entry.getKey(), Long.parseLong(entry.getValue()));
        }
        return nm;
    }


    protected void saveMap(Properties props, String prefix, Map<String, String> map) {
        List<Map.Entry<String, String>> l = new ArrayList<Map.Entry<String, String>>(map.entrySet());
        props.put(prefix + "count", Integer.toString(l.size()));
        for (int i = 0; i < l.size(); i++) {
            props.put(prefix + "key." + i, l.get(i).getKey());
            props.put(prefix + "val." + i, l.get(i).getValue());
        }
    }

    protected Map<String, String> loadMap(Properties props, String prefix) {
        Map<String, String> l = new HashMap<String, String>();
        String countStr = (String) props.get(prefix + "count");
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                String key = (String) props.get(prefix + "key." + i);
                String val = (String) props.get(prefix + "val." + i);
                l.put(key, val);
            }
        }
        return l;
    }


    protected void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
