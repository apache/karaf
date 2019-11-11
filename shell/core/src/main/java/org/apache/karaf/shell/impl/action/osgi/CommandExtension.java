/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.action.osgi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.extender.Extension;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.apache.karaf.shell.support.converter.GenericType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands extension
 */
@SuppressWarnings("rawtypes")
public class CommandExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExtension.class);

    private final Bundle bundle;
    private final Registry registry;
    private final CountDownLatch started;
    private final AggregateServiceTracker tracker;
    private final List<Class> classes = new ArrayList<>();
    private Manager manager;


    public CommandExtension(Bundle bundle, Registry registry) {
        this.bundle = bundle;
        this.registry = registry;
        this.started = new CountDownLatch(1);
        this.tracker = new AggregateServiceTracker(bundle.getBundleContext()) {
            @Override
            protected void updateState(State state) {
                CommandExtension.this.updateState(state);
            }
        };
    }

    public void start() throws Exception {
        try {
            String header = bundle.getHeaders().get(CommandExtender.KARAF_COMMANDS);
            Clause[] clauses = Parser.parseHeader(header);
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            for (Clause clause : clauses) {
                String name = clause.getName();
                int options = BundleWiring.LISTRESOURCES_LOCAL;
                name = name.replace('.', '/');
                if (name.endsWith("*")) {
                    options |= BundleWiring.LISTRESOURCES_RECURSE;
                    name = name.substring(0, name.length() - 1);
                }
                if (!name.startsWith("/")) {
                    name = "/" + name;
                }
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                Collection<String> classes = wiring.listResources(name, "*.class", options);
                for (String className : classes) {
                    className = className.replace('/', '.').replace(".class", "");
                    try {
                        inspectClass(bundle.loadClass(className));
                    } catch (final ClassNotFoundException | NoClassDefFoundError ex) {
                        LOGGER.info("Inspection of class {} failed.", className, ex);
                    }
                }
            }
            AggregateServiceTracker.State state = tracker.open();
            if (!state.isSatisfied()) {
                LOGGER.info("Command registration delayed for bundle {}/{}. Missing service: {}",
                        bundle.getSymbolicName(),
                        bundle.getVersion(),
                        state.getMissingServices());
            } else {
                updateState(state);
            }
        } finally {
            started.countDown();
        }
    }

    public void destroy() {
        try {
            if (started.getCount() > 0) {
                // Check to avoid InterruptedException in case we do not have to wait at all
                started.await(5000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("The wait for bundle " + bundle.getSymbolicName() + " being started before destruction has been interrupted.", e);
        }
        tracker.close();
    }

    @SuppressWarnings("unchecked")
    private synchronized void updateState(AggregateServiceTracker.State state) {
        boolean wasSatisfied = manager != null;
        boolean isSatisfied = state != null && state.isSatisfied();
        String action;
        if (wasSatisfied && isSatisfied) {
            action = "Updating";
        } else if (wasSatisfied) {
            action = "Unregistering";
        } else if (isSatisfied) {
            action = "Registering";
        } else {
            return;
        }
        LOGGER.info("{} commands for bundle {}/{}",
                action,
                bundle.getSymbolicName(),
                bundle.getVersion());
        if (wasSatisfied) {
            for (Class clazz : classes) {
                manager.unregister(clazz);
            }
            manager = null;
        }
        if (isSatisfied) {
            Registry reg = new RegistryImpl(registry);
            manager = new ManagerImpl(reg, registry);
            reg.register(bundle.getBundleContext());
            reg.register(manager);
            for (Map.Entry<Class, Object> entry : state.getSingleServices().entrySet()) {
                reg.register(entry.getValue());
            }
            for (final Map.Entry<Class, List> entry : state.getMultiServices().entrySet()) {
                reg.register((Callable) entry::getValue, entry.getKey());
            }
            for (Class clazz : classes) {
                manager.register(clazz);
            }
        }
    }

    private void inspectClass(final Class<?> clazz) throws Exception {
        Service reg = clazz.getAnnotation(Service.class);
        if (reg == null) {
            return;
        }
        // Create trackers
        for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
            for (Field field : cl.getDeclaredFields()) {
                Reference ref = field.getAnnotation(Reference.class);
                if (ref != null) {
                    GenericType type = new GenericType(field.getGenericType());
                    Class clazzRef = type.getRawClass() == List.class ? type.getActualTypeArgument(0).getRawClass() : type.getRawClass();
                    if (clazzRef != BundleContext.class
                            && clazzRef != Session.class
                            && clazzRef != Terminal.class
                            && clazzRef != History.class
                            && clazzRef != Registry.class
                            && clazzRef != SessionFactory.class
                            && !registry.hasService(clazzRef)) {
                        track(type, ref.optional(), ref.filter());
                    }
                }
            }
        }
        classes.add(clazz);
    }

    @SuppressWarnings("unchecked")
    protected void track(final GenericType type, boolean optional, String filter) {
        if (type.getRawClass() == List.class) {
            final Class clazzRef = type.getActualTypeArgument(0).getRawClass();
            tracker.trackList(clazzRef, filter);
        } else {
            final Class clazzRef = type.getRawClass();
            tracker.trackSingle(clazzRef, optional, filter);
        }
    }

}
