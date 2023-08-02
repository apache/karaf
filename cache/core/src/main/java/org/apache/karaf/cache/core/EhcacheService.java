/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cache.core;

import org.apache.karaf.cache.api.CacheService;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of CacheService based on Ehcache.
 */
public class EhcacheService implements CacheService {

    private final CacheManager cacheManager;
    private final Logger logger = LoggerFactory.getLogger(EhcacheService.class);

    public EhcacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <K, V> void createCache(String name, Configuration<K, V> configuration) {
        cacheManager.createCache(name, configuration);
    }

    @Override
    public void createCache(URL configFile, ClassLoader classLoader) {
        XmlConfiguration xmlConfiguration = new XmlConfiguration(configFile, classLoader);
        Map<String, CacheConfiguration<?, ?>> configurationMap = xmlConfiguration.getCacheConfigurations();
        configurationMap.forEach(
                (name, config) -> createCache(name, Eh107Configuration.fromEhcacheCacheConfiguration(config)));
        logger.info("Loaded ehcache xml configuration from " + configFile);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V get(String name, K key) {
        return (V) getCache(name).get(key);
    }

    @Override
    public <K, V> void put(String name, K key, V value) {
        cacheManager.getCache(name).put(key, value);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        return cacheManager.getCache(name);
    }

    @Override
    public void invalidateCache(String name) {
        cacheManager.destroyCache(name);
    }

    @Override
    public List<String> listCaches() {
        List<String> cacheNames = new ArrayList<>();
        cacheManager.getCacheNames().iterator().forEachRemaining(cacheNames::add);
        return cacheNames;
    }
}