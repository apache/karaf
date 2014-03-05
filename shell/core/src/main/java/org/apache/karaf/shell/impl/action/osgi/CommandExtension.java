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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.extender.Extension;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
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
public class CommandExtension implements Extension, Satisfiable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExtension.class);

    private final Bundle bundle;
    private final ManagerImpl manager;
    private final Registry registry;
    private final CountDownLatch started;
    private final MultiServiceTracker tracker;
    private final List<Satisfiable> satisfiables = new ArrayList<Satisfiable>();


    public CommandExtension(Bundle bundle, Registry registry) {
        this.bundle = bundle;
        this.registry = new RegistryImpl(registry);
        this.registry.register(bundle.getBundleContext());
        this.manager = new ManagerImpl(this.registry, registry);
        this.registry.register(this.manager);
        this.started = new CountDownLatch(1);
        this.tracker = new MultiServiceTracker(bundle.getBundleContext(), this);
    }

    @Override
    public void found() {
        for (Satisfiable s : satisfiables) {
            s.found();
        }
    }

    @Override
    public void updated() {
        for (Satisfiable s : satisfiables) {
            s.updated();
        }
    }

    @Override
    public void lost() {
        for (Satisfiable s : satisfiables) {
            s.lost();
        }
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
                    inspectClass(bundle.loadClass(className));
                }
            }
            tracker.open();
            if (!tracker.isSatisfied()) {
                LOGGER.info("Command registration delayed. Missing dependencies: " + tracker.getMissingServices());
            }
        } finally {
            started.countDown();
        }
    }

    public void destroy() {
        try {
            started.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("The wait for bundle being started before destruction has been interrupted.", e);
        }
        tracker.close();
    }

    private void inspectClass(final Class<?> clazz) throws Exception {
        Service reg = clazz.getAnnotation(Service.class);
        if (reg == null) {
            return;
        }
        // Create trackers
        for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
            for (Field field : cl.getDeclaredFields()) {
                if (field.getAnnotation(Reference.class) != null) {
                    GenericType type = new GenericType(field.getType());
                    Class clazzRef = type.getRawClass() == List.class ? type.getActualTypeArgument(0).getRawClass() : type.getRawClass();
                    if (clazzRef != BundleContext.class
                            && clazzRef != Session.class
                            && clazzRef != Terminal.class
                            && clazzRef != History.class
                            && clazzRef != Registry.class
                            && clazzRef != SessionFactory.class
                            && !registry.hasService(clazzRef)) {
                        track(clazzRef);
                    }
                }
            }
        }
        satisfiables.add(new AutoRegister(clazz));
    }

    protected void track(final Class clazzRef) {
        tracker.track(clazzRef);
        registry.register(new Callable() {
            @Override
            public Object call() throws Exception {
                return tracker.getService(clazzRef);
            }
        }, clazzRef);
    }

    public class AutoRegister implements Satisfiable {

        private final Class<?> clazz;

        public AutoRegister(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void found() {
            try {
                manager.register(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create service " + clazz.getName(), e);
            }
        }

        @Override
        public void updated() {
            lost();
            found();
        }

        @Override
        public void lost() {
            manager.unregister(clazz);
        }

    }

}
