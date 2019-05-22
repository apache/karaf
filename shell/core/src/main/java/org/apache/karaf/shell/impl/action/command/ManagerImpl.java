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
package org.apache.karaf.shell.impl.action.command;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Destroy;
import org.apache.karaf.shell.api.action.lifecycle.Init;
import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.support.converter.GenericType;

public class ManagerImpl implements Manager {

    private final Registry dependencies;
    private final Registry registrations;
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final boolean allowCustomServices;

    public ManagerImpl(Registry dependencies, Registry registrations) {
        this(dependencies, registrations, false);
    }

    public ManagerImpl(Registry dependencies, Registry registrations, boolean allowCustomServices) {
        this.dependencies = dependencies;
        this.registrations = registrations;
        this.allowCustomServices = allowCustomServices;
    }

    public <T> T instantiate(Class<? extends T> clazz) throws Exception {
        return instantiate(clazz, dependencies);
    }

    public <T> T instantiate(Class<? extends T> clazz, Registry registry) throws Exception {
        if (!allowCustomServices) {
            Service reg = clazz.getAnnotation(Service.class);
            if (reg == null) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Service");
            }
        }
        T instance = clazz.newInstance();
        // Inject services
        for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
            for (Field field : cl.getDeclaredFields()) {
                Reference ref = field.getAnnotation(Reference.class);
                if (ref != null) {
                    GenericType type = new GenericType(field.getGenericType());
                    Object value;
                    if (type.getRawClass() == List.class) {
                        Set<Object> set =
                            new HashSet<>(registry.getServices(type.getActualTypeArgument(0).getRawClass()));
                        if (registry != this.dependencies) {
                            set.addAll(this.dependencies.getServices(type.getActualTypeArgument(0).getRawClass()));
                        }
                        value = new ArrayList<>(set);
                    } else {
                        value = registry.getService(type.getRawClass());
                        if (value == null && registry != this.dependencies) {
                            value = this.dependencies.getService(type.getRawClass());
                        }
                    }
                    if (!allowCustomServices && value == null && !ref.optional()) {
                        throw new IllegalStateException("No service matching " + field.getType().getName());
                    }
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            Init ann = method.getAnnotation(Init.class);
            if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                method.setAccessible(true);
                method.invoke(instance);
            }
        }
        return instance;
    }

    public void release(Object instance) throws Exception {
        Class<?> clazz = instance.getClass();
        if (!allowCustomServices) {
            Service reg = clazz.getAnnotation(Service.class);
            if (reg == null) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Service");
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            Destroy ann = method.getAnnotation(Destroy.class);
            if (ann != null && method.getParameterTypes().length == 0 && method.getReturnType() == void.class) {
                method.setAccessible(true);
                method.invoke(instance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(Class<?> clazz) {
        if (!allowCustomServices) {
            Service reg = clazz.getAnnotation(Service.class);
            if (reg == null ) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Service");
            }
        }
        if (Action.class.isAssignableFrom(clazz)) {
            final Command cmd = clazz.getAnnotation(Command.class);
            if (cmd == null) {
                throw new IllegalArgumentException("Command " + clazz.getName() + " is not annotated with @Command");
            }
            Object command = new ActionCommand(this, (Class<? extends Action>) clazz);
            synchronized (instances) {
                instances.put(clazz, command);
            }
            registrations.register(command);
        }
        if (allowCustomServices
                || Completer.class.isAssignableFrom(clazz)
                || Parser.class.isAssignableFrom(clazz)) {
            try {
                // Create completer
                Object completer = instantiate(clazz);
                synchronized (instances) {
                    instances.put(clazz, completer);
                }
                registrations.register(completer);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void unregister(Class<?> clazz) {
        Object object;
        synchronized (instances) {
            object = instances.remove(clazz);
        }
        if (object != null) {
            registrations.unregister(object);
            if (object instanceof Completer) {
                try {
                    release(object);
                } catch (Exception e) {
                    // TODO: log exception
                }
            }
        }
    }
}
