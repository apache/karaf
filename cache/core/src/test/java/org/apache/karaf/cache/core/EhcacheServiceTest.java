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

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

@RunWith(EasyMockRunner.class)
public class EhcacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Test
    public void testCreateCacheWithConfigurationObject() {
        String cacheName = "Test";
        Cache<String, Long> cache = mock(Cache.class);

        ResourcePools pools = ResourcePoolsBuilder.heap(100).build();
        Configuration<String, Long> configuration = Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Long.class, pools).build());
        expect(cacheManager.createCache(cacheName, configuration)).andReturn(cache);

        replay(cacheManager, cache);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);

        ehcacheService.createCache(cacheName, configuration);

        verify(cacheManager, cache);
    }

    @Test
    public void testCreateCacheWithConfigurationUrl() {
        String cacheName = "TestCache";
        URL configFile = getClass().getResource("/test-cache-config.xml");

        Cache<Object, Object> cache = mock(Cache.class);
        expect(cacheManager.createCache(eq(cacheName), anyObject(Configuration.class)))
                .andReturn(cache);

        replay(cacheManager, cache);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);

        ehcacheService.createCache(configFile, this.getClass().getClassLoader());

        verify(cacheManager);
    }

    @Test
    public void testListCaches() {
        List<String> cacheNames = Arrays.asList("C1", "C2");
        expect(cacheManager.getCacheNames()).andReturn(cacheNames);

        replay(cacheManager);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);
        List<String> caches = ehcacheService.listCaches();
        assertEquals(cacheNames, caches);
    }

    @Test
    public void testInvalidateCache() {
        String cacheName = "Test";
        cacheManager.destroyCache(cacheName);
        expectLastCall();

        replay(cacheManager);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);
        ehcacheService.invalidateCache(cacheName);
        verify(cacheManager);
    }

    @Test
    public void testGetCache() {
        String cacheName = "Test";
        Cache<Object, Object> objCache = mock(Cache.class);
        expect(cacheManager.getCache(cacheName)).andReturn(objCache);

        replay(cacheManager);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);

        ehcacheService.getCache(cacheName);

        verify(cacheManager);
    }

    @Test
    public void testGet() {
        String cacheName = "Test";
        String key = "k";
        String value = "v";

        Cache<Object, Object> cache = mock(Cache.class);
        expect(cacheManager.getCache(cacheName)).andReturn(cache);
        expect(cache.get(key)).andReturn(value);

        replay(cacheManager, cache);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);

        Object result = ehcacheService.get(cacheName, key);
        assertEquals(value, result);
        verify(cacheManager);
    }

    @Test
    public void testPut() {
        String cacheName = "Test";
        String key = "k";
        String value = "v";

        Cache<Object, Object> cache = mock(Cache.class);
        expect(cacheManager.getCache(cacheName)).andReturn(cache);
        cache.put(key, value);
        expectLastCall();

        replay(cacheManager, cache);

        EhcacheService ehcacheService = new EhcacheService(cacheManager);

        ehcacheService.put(cacheName, key, value);
        verify(cacheManager);
    }
}
