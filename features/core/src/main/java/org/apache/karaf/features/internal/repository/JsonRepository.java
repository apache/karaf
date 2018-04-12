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
package org.apache.karaf.features.internal.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.util.json.JsonReader;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository using a JSON representation of resource metadata.
 * The json should be a map: the key is the resource uri and the
 * value is a map of resource headers.
 * The content of the URL can be gzipped.
 */
public class JsonRepository extends BaseRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRepository.class);

    private final boolean ignoreFailures;
    private final UrlLoader loader;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public JsonRepository(String url, long expiration, boolean ignoreFailures) {
        loader = new UrlLoader(url, expiration) {
            @Override
            protected boolean doRead(InputStream is) throws IOException {
                return JsonRepository.this.doRead(is);
            }
        };
        this.ignoreFailures = ignoreFailures;
    }

    @Override
    public List<Resource> getResources() {
        checkAndLoadCache();
        lock.readLock().lock();
        try {
            return super.getResources();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        checkAndLoadCache();
        lock.readLock().lock();
        try {
            return super.findProviders(requirements);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void checkAndLoadCache() {
        try {
            loader.checkAndLoadCache();
        } catch (Exception e) {
            if (ignoreFailures) {
                logger.warn("Ignoring failure: " + e.getMessage(), e);
            } else {
                throw e;
            }
        }
    }

    protected boolean doRead(InputStream is) throws IOException {
        Map<String, Map<String, String>> metadatas = verify(JsonReader.read(is));
        lock.writeLock().lock();
        try {
            resources.clear();
            capSets.clear();
            for (Map.Entry<String, Map<String, String>> metadata : metadatas.entrySet()) {
                try {
                    Resource resource = ResourceBuilder.build(metadata.getKey(), metadata.getValue());
                    addResource(resource);
                } catch (Exception e) {
                    LOGGER.info("Unable to build resource for " + metadata.getKey(), e);
                }
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> verify(Object value) {
        Map<?, ?> obj = Map.class.cast(value);
        for (Map.Entry<?, ?> entry : obj.entrySet()) {
            String.class.cast(entry.getKey());
            Map<?, ?> child = Map.class.cast(entry.getValue());
            for (Map.Entry<?, ?> ce : child.entrySet()) {
                String.class.cast(ce.getKey());
                String.class.cast(ce.getValue());
            }
        }
        return (Map<String, Map<String, String>>) obj;
    }

}
