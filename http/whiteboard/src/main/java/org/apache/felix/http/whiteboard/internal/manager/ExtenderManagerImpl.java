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
package org.apache.felix.http.whiteboard.internal.manager;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import javax.servlet.Servlet;
import javax.servlet.Filter;
import java.util.HashMap;

public final class ExtenderManagerImpl
    implements ExtenderManager
{
    private final static String CONTEXT_ID_KEY = "contextId";
    private final static String PATTERN_KEY = "pattern";
    private final static String ALIAS_KEY = "alias";
    private final static String INIT_KEY_PREFIX = "init.";

    private HttpService httpService;
    private final HashMap<Object, AbstractMapping> mapping;
    private final HttpContextManager contextManager;

    public ExtenderManagerImpl()
    {
        this.mapping = new HashMap<Object, AbstractMapping>();
        this.contextManager = new HttpContextManager();
    }

    private String getStringProperty(ServiceReference ref, String key)
    {
        Object value = ref.getProperty(key);
        return (value instanceof String) ? (String)value : null;
    }

    private int getIntProperty(ServiceReference ref, String key, int defValue)
    {
        Object value = ref.getProperty(key);
        if (value == null) {
            return defValue;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defValue;
        }
    }

    private void addInitParams(ServiceReference ref, AbstractMapping mapping)
    {
        for (String key : ref.getPropertyKeys()) {
            if (key.startsWith(INIT_KEY_PREFIX)) {
                String paramKey = key.substring(INIT_KEY_PREFIX.length());
                String paramValue = getStringProperty(ref, key);

                if (paramValue != null) {
                    mapping.getInitParams().put(paramKey, paramValue);
                }
            }
        }
    }

    public void add(HttpContext service, ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextId = getStringProperty(ref, CONTEXT_ID_KEY);
        if (contextId != null) {
            this.contextManager.addHttpContext(bundle, contextId, service);
        }
    }

    public void remove(HttpContext service)
    {
        this.contextManager.removeHttpContext(service);
    }

    private HttpContext getHttpContext(ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextId = getStringProperty(ref, CONTEXT_ID_KEY);

        if (contextId != null) {
            return this.contextManager.getHttpContext(bundle, contextId);
        } else {
            return new DefaultHttpContext(bundle);
        }
    }

    public void add(Filter service, ServiceReference ref)
    {
        int ranking = getIntProperty(ref, Constants.SERVICE_RANKING, 0);
        String pattern = getStringProperty(ref, PATTERN_KEY);

        if (pattern == null) {
            return;
        }

        FilterMapping mapping = new FilterMapping(getHttpContext(ref), service, pattern, ranking);
        addInitParams(ref, mapping);
        addMapping(service, mapping);
    }

    public void remove(Filter service)
    {
        removeMapping(service);
    }

    public void add(Servlet service, ServiceReference ref)
    {
        String alias = getStringProperty(ref, ALIAS_KEY);
        if (alias == null) {
            return;
        }

        ServletMapping mapping = new ServletMapping(getHttpContext(ref), service, alias); 
        addInitParams(ref, mapping);
        addMapping(service, mapping);
    }

    public void remove(Servlet service)
    {
        removeMapping(service);
    }

    public synchronized void setHttpService(HttpService service)
    {
        this.httpService = service;
        if (this.httpService instanceof ExtHttpService) {
            SystemLogger.info("Detected extended HttpService. Filters enabled.");
        } else {
            SystemLogger.info("Detected standard HttpService. Filters disabled.");
        }

        registerAll();
    }

    public synchronized void unsetHttpService()
    {
        unregisterAll();
        this.httpService = null;
    }

    public synchronized void unregisterAll()
    {
        if (this.httpService != null) {
            for (AbstractMapping mapping : this.mapping.values()) {
                mapping.unregister(this.httpService);
            }
        }
    }

    private synchronized void registerAll()
    {
        if (this.httpService != null) {
            for (AbstractMapping mapping : this.mapping.values()) {
                mapping.register(this.httpService);
            }
        }
    }

    private synchronized void addMapping(Object key, AbstractMapping mapping)
    {
        this.mapping.put(key, mapping);
        if (this.httpService != null) {
            mapping.register(this.httpService);
        }
    }

    private synchronized void removeMapping(Object key)
    {
        AbstractMapping mapping = this.mapping.remove(key);
        if ((mapping != null) && (this.httpService != null)) {
            mapping.unregister(this.httpService);
        }
    }
}
