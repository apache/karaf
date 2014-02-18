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
package org.apache.karaf.shell.inject.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Function;
import org.apache.felix.utils.extender.Extension;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.inject.Destroy;
import org.apache.karaf.shell.inject.Init;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands extension
 */
public class InjectionExtension implements Extension, Satisfiable {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionExtension.class);

    private final Bundle bundle;
    private final CountDownLatch started;
    private final MultiServiceTracker tracker;
    private final List<Satisfiable> satisfiables = new ArrayList<Satisfiable>();


    public InjectionExtension(Bundle bundle) {
        this.bundle = bundle;
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
            String header = bundle.getHeaders().get(InjectionExtender.KARAF_COMMANDS);
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
        if (Action.class.isAssignableFrom(clazz)) {
            final Command cmd = clazz.getAnnotation(Command.class);
            if (cmd == null) {
                throw new IllegalArgumentException("Command " + clazz.getName() + " is not annotated with @Command");
            }
            // Create trackers
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() != BundleContext.class) {
                            tracker.track(field.getType());
                        }
                    }
                }
            }
            satisfiables.add(new AutoRegisterCommand((Class<? extends Action>) clazz));
        }
        if (Completer.class.isAssignableFrom(clazz)) {
            // Create trackers
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() != BundleContext.class) {
                            tracker.track(field.getType());
                        }
                    }
                }
            }
            satisfiables.add(new AutoRegisterService(clazz));
        }
    }

    public class AutoRegisterService implements Satisfiable {

        private final Class<?> clazz;
        private Object service;
        private ServiceRegistration registration;

        public AutoRegisterService(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void found() {
            try {
                // Create completer
                service = clazz.newInstance();
                Set<String> classes = new HashSet<String>();
                // Inject services
                for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                    classes.add(cl.getName());
                    for (Class c : cl.getInterfaces()) {
                        classes.add(c.getName());
                    }
                    for (Field field : cl.getDeclaredFields()) {
                        if (field.getAnnotation(Reference.class) != null) {
                            Object value;
                            if (field.getType() == BundleContext.class) {
                                value = InjectionExtension.this.bundle.getBundleContext();
                            } else {
                                value = InjectionExtension.this.tracker.getService(field.getType());
                            }
                            if (value == null) {
                                throw new RuntimeException("No OSGi service matching " + field.getType().getName());
                            }
                            field.setAccessible(true);
                            field.set(service, value);
                        }
                    }
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    Init ann = method.getAnnotation(Init.class);
                    if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                        method.setAccessible(true);
                        method.invoke(service);
                    }
                }
                Hashtable<String, String> props = new Hashtable<String, String>();
                registration = bundle.getBundleContext().registerService(classes.toArray(new String[classes.size()]), service, props);
            } catch (Exception e) {
                throw new RuntimeException("Unable to creation service " + clazz.getName(), e);
            }
        }

        @Override
        public void updated() {
            lost();
            found();
        }

        @Override
        public void lost() {
            if (registration != null) {
                for (Method method : clazz.getDeclaredMethods()) {
                    Destroy ann = method.getAnnotation(Destroy.class);
                    if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                        method.setAccessible(true);
                        try {
                            method.invoke(service);
                        } catch (Exception e) {
                            LOGGER.warn("Error destroying service", e);
                        }
                    }
                }
                registration.unregister();
                registration = null;
            }
        }

    }

    public class AutoRegisterCommand extends AbstractCommand implements Satisfiable, CompletableFunction {

        private final Class<? extends Action> actionClass;
        private ServiceRegistration registration;

        public AutoRegisterCommand(Class<? extends Action> actionClass) {
            this.actionClass = actionClass;
        }

        @Override
        public void found() {
            // Register command
            Command cmd = actionClass.getAnnotation(Command.class);
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(CommandProcessor.COMMAND_SCOPE, cmd.scope());
            props.put(CommandProcessor.COMMAND_FUNCTION, cmd.name());
            String[] classes = {
                    Function.class.getName(),
                    CompletableFunction.class.getName(),
                    CommandWithAction.class.getName(),
                    AbstractCommand.class.getName()
            };
            registration = bundle.getBundleContext().registerService(classes, this, props);
        }

        @Override
        public void updated() {

        }

        @Override
        public void lost() {
            if (registration != null) {
                registration.unregister();
                registration = null;
            }
        }

        @Override
        public Action createNewAction() {
            try {
                Action action = actionClass.newInstance();
                // Inject services
                for (Class<?> cl = actionClass; cl != Object.class; cl = cl.getSuperclass()) {
                    for (Field field : cl.getDeclaredFields()) {
                        if (field.getAnnotation(Reference.class) != null) {
                            Object value;
                            if (field.getType() == BundleContext.class) {
                                value = InjectionExtension.this.bundle.getBundleContext();
                            } else {
                                value = InjectionExtension.this.tracker.getService(field.getType());
                            }
                            if (value == null) {
                                throw new RuntimeException("No OSGi service matching " + field.getType().getName());
                            }
                            field.setAccessible(true);
                            field.set(action, value);
                        }
                    }
                }
                if (action instanceof BundleContextAware) {
                    ((BundleContextAware) action).setBundleContext(bundle.getBundleContext());
                }
                for (Method method : actionClass.getDeclaredMethods()) {
                    Init ann = method.getAnnotation(Init.class);
                    if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                        method.setAccessible(true);
                        method.invoke(action);
                    }
                }
                return action;
            } catch (Exception e) {
                throw new RuntimeException("Unable to creation command action " + actionClass.getName(), e);
            }
        }

        @Override
        public void releaseAction(Action action) throws Exception {
            for (Method method : actionClass.getDeclaredMethods()) {
                Destroy ann = method.getAnnotation(Destroy.class);
                if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                    method.setAccessible(true);
                    method.invoke(action);
                }
            }
            super.releaseAction(action);
        }

        @Override
        public List<Completer> getCompleters() {
            return null;
        }

        @Override
        public Map<String, Completer> getOptionalCompleters() {
            return null;
        }
    }
}
