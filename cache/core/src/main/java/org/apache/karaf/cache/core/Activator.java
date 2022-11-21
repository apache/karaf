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
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.ManagedService;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.util.Arrays;
import java.util.Optional;

@Services(provides = @ProvideService(CacheService.class))
public class Activator extends BaseActivator implements ManagedService {

    private EhcacheService ehcacheService;

    @Override
    protected void doStart() {
        logger.info("Cache bundle starting..");
        setUpEhcacheClassloading();
        CachingProvider provider = Caching.getCachingProvider();
        ehcacheService = new EhcacheService(provider.getCacheManager());
        register(CacheService.class, ehcacheService);
        logger.info("Cache bundle started!");
    }

    /**
     * Tells JSR 107 where the implementation by its classloader to the ehcache bundle,
     * otherwise Caching.getCachingProvider() throws an exception saying that it couldn't find any providers
     *
     */
    private void setUpEhcacheClassloading() {
        logger.debug("Replacing JSR 107 classloader with the ehcache bundle");
        Optional<Bundle> ehcacheBundleOpt = Arrays.asList(bundleContext.getBundles()).stream()
                .filter(bundle -> bundle.getSymbolicName().equals("org.ehcache"))
                .findFirst();
        if (ehcacheBundleOpt.isPresent()) {
            Bundle ehcacheBundle = ehcacheBundleOpt.get();
            BundleWiring bundleWiring = ehcacheBundle.adapt(BundleWiring.class);
            ClassLoader classLoader = bundleWiring.getClassLoader();
            Caching.setDefaultClassLoader(classLoader);
        } else {
            throw new IllegalStateException("No ehcache bundle found!");
        }
    }

    @Override
    protected void doStop() {
        super.doStop();
        ehcacheService = null;
    }
}
