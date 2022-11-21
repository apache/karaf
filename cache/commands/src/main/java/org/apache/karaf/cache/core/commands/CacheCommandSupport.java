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

package org.apache.karaf.cache.core.commands;

import org.apache.karaf.cache.api.CacheService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;

public abstract class CacheCommandSupport implements Action
{

    @Reference
    private CacheService cacheService;

    @Override
    public Object execute() throws Exception
    {
        if (cacheService == null) {
            throw new IllegalStateException("CacheService not found");
        }
        return doExecute(cacheService);
    }

    protected abstract Object doExecute(CacheService cacheService) throws Exception;

    @SuppressWarnings("unchecked")
    protected Object castKey(String cacheName, Object key) {
        Cache cache = cacheService.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cache " + cacheName + " not found!");
        }
        return cast(key, cacheService.getCache(cacheName).getConfiguration(Configuration.class).getKeyType());
    }

    @SuppressWarnings("unchecked")
    protected Object castValue(String cacheName, Object value) {
        return cast(value, cacheService.getCache(cacheName).getConfiguration(Configuration.class).getValueType());
    }

    private Object cast(Object argument, Class type)
    {
        if (type.equals(Short.class)) {
            return Short.parseShort((String) argument);
        }
        if (type.equals(Integer.class)) {
            return Integer.parseInt((String) argument);
        }
        if (type.equals(Long.class)) {
            return Long.parseLong((String) argument);
        }
        if (type.equals(Boolean.class)) {
            return Boolean.parseBoolean((String) argument);
        }

        return argument;
    }
}
