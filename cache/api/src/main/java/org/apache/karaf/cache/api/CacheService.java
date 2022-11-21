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

package org.apache.karaf.cache.api;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;
import java.net.URL;
import java.util.List;

/**
 * A simple caching facade that lets use caching using JSR 107 APIs.
 */
public interface CacheService {

    /**
     * Creates a new cache.
     * @param name name of the cache to create
     * @param configuration cache configuration, such as its key and value type.
     *                      Specific configurations (such as max size and caching duration)
     *                      will depend on the cache provider.
     * @param <K> type of cache keys, e.g. Long or String
     * @param <V> type of cache values, e.g. Long or String
     */
    <K, V> void createCache(String name, Configuration<K, V> configuration);

    /**
     * Creates a new cache from a configuration file (located in Karaf's etc directory).
     * @param configFile config file
     * @param classLoader class loader can be passed in case the cache complex custom keys and/or values
     */
    void createCache(URL configFile, ClassLoader classLoader);

    /**
     * Get a single cached value by providing cache's name and a key.
     * @param name name of the cache to get the value from
     * @param key key of the cached item
     * @param <K> cache key type
     * @param <V> cache value type
     * @return
     */
    <K, V> V get(String name, K key);

    /**
     * Store a new value in a cache.
     * @param name name of the cache to put new value into
     * @param key key under which the value will be cached
     * @param value value to cache
     * @param <K> cache key type
     * @param <V> cache value type
     */
    <K, V> void put(String name, K key, V value);

    /**
     * Get the cache object directly.
     * @param name name of the cache
     * @param <K> cache key type
     * @param <V> cache value type
     * @return
     */
    <K, V> Cache<K, V> getCache(String name);

    /**
     * Invalidates a cache.
     * @param name name of the cache to invalidate
     */
    void invalidateCache(String name);

    /**
     * Lists all available caches by their names.
     * @return list of cache names
     */
    List<String> listCaches();
}
