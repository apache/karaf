/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.utils;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * A very simplistic LRU cache based on LinkedHashMap. It grows up to the size specified in the constructor,
 * evicting least recently accessed entries to keep the size there.
 */
public final class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;

    private final int maxEntries;

    public SimpleLRUCache(int maxEntries) {
        this(16, 0.75f, maxEntries);
    }

    public SimpleLRUCache(int initialSize, float loadFactor, int maxEntries) {
        super(initialSize, loadFactor, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > maxEntries;
    }
}
