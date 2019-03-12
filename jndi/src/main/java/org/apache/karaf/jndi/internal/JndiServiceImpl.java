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
package org.apache.karaf.jndi.internal;

import org.apache.aries.proxy.ProxyManager;
import org.apache.karaf.jndi.JndiService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.naming.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the JNDI Service.
 */
public class JndiServiceImpl implements JndiService {

    private BundleContext bundleContext;
    private ProxyManager proxyManager;

    private final static String OSGI_JNDI_CONTEXT_PREFIX = "osgi:service";
    private final static String OSGI_JNDI_SERVICE_PROPERTY = "osgi.jndi.service.name";

    @Override
    public Map<String, String> names() throws Exception {
        Map<String, String> result = names("/");
        result.putAll(names(OSGI_JNDI_CONTEXT_PREFIX));
        return result;
    }

    @Override
    public Map<String, String> names(String name) throws Exception {
        Map<String, String> map = new HashMap<>();
        if (name.startsWith(OSGI_JNDI_CONTEXT_PREFIX)) {
            // OSGi service binding
            // make a lookup using directly the OSGi service
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                ServiceReference<?>[] services = bundle.getRegisteredServices();
                if (services != null) {
                    for (ServiceReference service : services) {
                        if (service.getProperty(OSGI_JNDI_SERVICE_PROPERTY) != null) {
                            Object actualService = bundleContext.getService(service);
                            if (proxyManager.isProxy(actualService)) {
                                actualService = proxyManager.unwrap(actualService).call();
                            }
                            if (service.getProperty(OSGI_JNDI_SERVICE_PROPERTY).toString().startsWith("/"))
                                map.put(OSGI_JNDI_CONTEXT_PREFIX + service.getProperty(OSGI_JNDI_SERVICE_PROPERTY), actualService.getClass().getName());
                            else map.put(OSGI_JNDI_CONTEXT_PREFIX + "/" + service.getProperty(OSGI_JNDI_SERVICE_PROPERTY), actualService.getClass().getName());
                            bundleContext.ungetService(service);
                        }
                    }
                }
            }
        } else {
            // "real" JNDI lookup
            Context context = new InitialContext();
            NamingEnumeration<NameClassPair> pairs = context.list(name);
            while (pairs.hasMoreElements()) {
                NameClassPair pair = pairs.nextElement();
                Object o;
                if (name != null) {
                    o = context.lookup(name + "/" + pair.getName());
                } else {
                    o = context.lookup(pair.getName());
                }
                if (o instanceof Context) {
                    StringBuilder sb = new StringBuilder();
                    if (pair.getName().contains(":"))
                        sb.append(pair.getName());
                    else sb.append("/").append(pair.getName());
                    names((Context) o, sb, map);
                } else {
                    if (pair.getName().contains(":"))
                        map.put(pair.getName(), pair.getClassName());
                    else map.put("/" + pair.getName(), pair.getClassName());
                }
            }
        }
        return map;
    }

    public List<String> contexts() throws Exception {
        return contexts("/");
    }

    public List<String> contexts(String name) throws Exception {
        List<String> contexts = new ArrayList<>();
        Context context = new InitialContext();
        NamingEnumeration<NameClassPair> pairs = context.list(name);
        while (pairs.hasMoreElements()) {
            NameClassPair pair = pairs.nextElement();
            Object o;
            if (name != null) {
                o = context.lookup(name + "/" + pair.getName());
            } else {
                o = context.lookup(pair.getName());
            }
            if (o instanceof Context) {
                StringBuilder sb = new StringBuilder();
                sb.append("/").append(pair.getName());
                contexts((Context) o, sb, contexts);
            }
        }
        return contexts;
    }

    private void contexts(Context context, StringBuilder sb, List<String> contexts) throws Exception {
        NamingEnumeration list = context.listBindings("");
        while (list.hasMore()) {
            Binding item = (Binding) list.next();
            String name = item.getName();
            Object o = item.getObject();
            if (o instanceof Context) {
                if (((Context) o).list("").hasMoreElements()) {
                    sb.append("/").append(name);
                    contexts((Context) o, sb, contexts);
                } else {
                    contexts.add(sb.toString() + "/" + name);
                }
            }
        }
    }

    /**
     * Recursively list a context/names
     *
     * @param ctx the startup context.
     * @param sb the string builder where to construct the full qualified name.
     * @param map the final map containing name/class name pairs.
     * @throws Exception
     */
    private static final void names(Context ctx, StringBuilder sb, Map<String, String> map) throws Exception {
        NamingEnumeration list = ctx.listBindings("");
        while (list.hasMore()) {
            Binding item = (Binding) list.next();
            String className = item.getClassName();
            String name = item.getName();
            Object o = item.getObject();
            if (o instanceof Context) {
                sb.append("/").append(name);
                names((Context) o, sb, map);
            } else {
                map.put(sb.toString() + "/" + name, className);
            }
        }
    }

    @Override
    public void create(String name) throws Exception {
        Context context = new InitialContext();
        String[] splitted = name.split("/");
        if (splitted.length > 0) {
            for (String split : splitted) {
                try {
                    Object o = context.lookup(split);
                    if (!(o instanceof Context)) {
                        throw new NamingException("Name " + split + " already exists");
                    }
                } catch (NameNotFoundException e) {
                    context.createSubcontext(split);
                }
                context = (Context) context.lookup(split);
            }
        } else {
            context.createSubcontext(name);
        }
    }

    @Override
    public void delete(String name) throws Exception {
        Context context = new InitialContext();
        context.destroySubcontext(name);
    }

    @Override
    public void bind(long serviceId, String name) throws Exception {
        Context context = new InitialContext();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference<?>[] services = bundle.getRegisteredServices();
            if (services != null) {
                for (ServiceReference service : services) {
                    if (service.getProperty(Constants.SERVICE_ID) != null && ((Long) service.getProperty(Constants.SERVICE_ID)) == serviceId) {
                        Object actualService = bundleContext.getService(service);
                        if (proxyManager.isProxy(actualService)) {
                            actualService = proxyManager.unwrap(actualService).call();
                        }
                        try {
                            String[] splitted = name.split("/");
                            if (splitted.length > 0) {
                                for (int i = 0; i < splitted.length - 1; i++) {
                                    try {
                                        Object o = context.lookup(splitted[i]);
                                        if (!(o instanceof Context)) {
                                            throw new NamingException("Name " + splitted[i] + " already exists");
                                        }
                                    } catch (NameNotFoundException nnfe) {
                                        context.createSubcontext(splitted[i]);
                                    }
                                    context = (Context) context.lookup(splitted[i]);
                                }
                                name = splitted[splitted.length - 1];
                            }
                            context.bind(name, actualService);
                        } finally {
                            bundleContext.ungetService(service);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void alias(String name, String alias) throws Exception {
        Context context = new InitialContext();
        if (name.startsWith(OSGI_JNDI_CONTEXT_PREFIX)) {
            // get the object
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                ServiceReference<?>[] services = bundle.getRegisteredServices();
                if (services != null) {
                    for (ServiceReference service : services) {
                        if (service.getProperty(OSGI_JNDI_SERVICE_PROPERTY) != null && service.getProperty(OSGI_JNDI_SERVICE_PROPERTY).equals(name.substring(OSGI_JNDI_CONTEXT_PREFIX.length() + 1))) {
                            Object actualService = bundleContext.getService(service);
                            try {
                                if (proxyManager.isProxy(actualService)) {
                                    actualService = proxyManager.unwrap(actualService).call();
                                }
                                String[] splitted = alias.split("/");
                                if (splitted.length > 0) {
                                    for (int i = 0; i < splitted.length - 1; i++) {
                                        try {
                                            Object o = context.lookup(splitted[i]);
                                            if (!(o instanceof Context)) {
                                                throw new NamingException("Name " + splitted[i] + " already exists");
                                            }
                                        } catch (NameNotFoundException nnfe) {
                                            context.createSubcontext(splitted[i]);
                                        }
                                        context = (Context) context.lookup(splitted[i]);
                                    }
                                    alias = splitted[splitted.length -1];
                                }
                                context.bind(alias, actualService);
                            } finally {
                                bundleContext.ungetService(service);
                            }
                        }
                    }
                }
            }
        } else {
            Object object = context.lookup(name);
            String[] splitted = alias.split("/");
            if (splitted.length > 0) {
                for (int i = 0; i < splitted.length - 1; i++) {
                    try {
                        Object o = context.lookup(splitted[i]);
                        if (!(o instanceof Context)) {
                            throw new NamingException("Name " + splitted[i] + " already exists");
                        }
                    } catch (NameNotFoundException nnfe) {
                        context.createSubcontext(splitted[i]);
                    }
                    context = (Context) context.lookup(splitted[i]);
                }
                alias = splitted[splitted.length - 1];
            }
            context.bind(alias, object);
        }
    }

    @Override
    public void unbind(String name) throws Exception {
        InitialContext context = new InitialContext();
        if (name.startsWith(OSGI_JNDI_CONTEXT_PREFIX)) {
            throw new IllegalArgumentException("You can't unbind a name from the " + OSGI_JNDI_CONTEXT_PREFIX + " JNDI context.");
        }
        context.unbind(name);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public void setProxyManager(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

}
