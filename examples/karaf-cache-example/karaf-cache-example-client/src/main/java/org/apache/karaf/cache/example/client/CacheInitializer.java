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

package org.apache.karaf.cache.example.client;

import org.apache.karaf.cache.api.CacheService;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;

@Component(immediate = true)
public class CacheInitializer {

    private final Logger logger = LoggerFactory.getLogger(CacheInitializer.class);

    @Reference
    private CacheService cacheService;

    @Activate
    public void activate() {
        initCaches();
    }

    private void initCaches() {
        initConfigBuilderCache();
        initXmlConfigCache();
    }

    private void initConfigBuilderCache() {
        String cacheName = "ExampleCache";
        logger.info("Creating " + cacheName + " and populating it with some values..");

        CacheConfiguration<Long, Book> cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Long.class, Book.class, ResourcePoolsBuilder.heap(50))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(1)))
                .build();
        cacheService.createCache(cacheName, Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
        cacheService.put(cacheName, 1L, new Book("Effective Java", 123));
        cacheService.put(cacheName, 2L, new Book("OSGi in Action", 456));
        logger.info("Cache value under key 1: " + cacheService.get(cacheName, 1L));
        logger.info("Cache value under key 2: " + cacheService.get(cacheName, 2L));
    }

    private void initXmlConfigCache() {
        String cacheName = "BookCache";
        logger.info("Creating " + cacheName + " and populating it with some values..");

        URL url = FrameworkUtil.getBundle(this.getClass()).getEntry("/book-cache.xml");
        cacheService.createCache(url, this.getClass().getClassLoader());
        cacheService.put(cacheName, 1L, new Book("Apache Karaf Cookbook", 789));

        logger.info("Cache value under key 1: " + cacheService.get(cacheName, 1L));
    }
}
