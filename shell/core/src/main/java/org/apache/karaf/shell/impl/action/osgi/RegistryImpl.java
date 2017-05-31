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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Registry;

public class RegistryImpl implements Registry {

    private final Registry parent;
    private final Map<Object, Object> services = new LinkedHashMap<>();
    private final Map<String, List<Command>> commands = new HashMap<>();

    public RegistryImpl(Registry parent) {
        this.parent = parent;
    }

    @Override
    public List<Command> getCommands() {
        return getServices(Command.class);
    }

    @Override
    public Command getCommand(String scope, String name) {
        if (parent != null) {
            Command command = parent.getCommand(scope, name);
            if (command != null) {
                return command;
            }
        }
        synchronized (services) {
            List<Command> cmds = commands.get(scope + ":" + name);
            if (cmds != null && !cmds.isEmpty()) {
                return cmds.get(0);
            }
        }
        return null;
    }

    @Override
    public <T> void register(Callable<T> factory, Class<T> clazz) {
        synchronized (services) {
            services.put(clazz, new Factory<>(clazz, factory));
        }
    }

    @Override
    public void register(Object service) {
        synchronized (services) {
            services.put(service, service);
            if (service instanceof Command) {
                Command cmd = (Command) service;
                String key = cmd.getScope() + ":" + cmd.getName();
                commands.computeIfAbsent(key, k -> new ArrayList<>()).add(cmd);
            }
        }
    }

    @Override
    public void unregister(Object service) {
        synchronized (services) {
            services.remove(service);
            if (service instanceof Command) {
                Command cmd = (Command) service;
                String key = cmd.getScope() + ":" + cmd.getName();
                List<Command> cmds = commands.get(key);
                if (cmds != null) {
                    cmds.remove(cmd);
                    if (cmds.isEmpty()) {
                        commands.remove(key);
                    }
                }
            }
        }
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        synchronized (services) {
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory<?>) service).clazz)) {
                        if (isVisible(service)) {
                            try {
                                Object value = ((Factory<?>) service).callable.call();
                                if (value instanceof List) {
                                    for (Object v : (List<?>) value) {
                                        return clazz.cast(v);
                                    }
                                } else {
                                    return clazz.cast(value);
                                }
                            } catch (Exception e) {
                                // TODO: log exception
                            }
                        }
                    }
                } else if (clazz.isInstance(service)) {
                    if (isVisible(service)) {
                        return clazz.cast(service);
                    }
                }
            }
        }
        if (parent != null) {
            return parent.getService(clazz);
        }
        return null;
    }

    @Override
    public <T> List<T> getServices(Class<T> clazz) {
        List<T> list = new ArrayList<>();
        synchronized (services) {
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory<?>) service).clazz)) {
                        if (isVisible(service)) {
                            try {
                                Object value = ((Factory<?>) service).callable.call();
                                if (value instanceof List) {
                                    for (Object v : (List<?>) value) {
                                        list.add(clazz.cast(v));
                                    }
                                } else {
                                    list.add(clazz.cast(value));
                                }
                            } catch (Exception e) {
                                // TODO: log exception
                            }
                        }
                    }
                } else if (clazz.isInstance(service)) {
                    if (isVisible(service)) {
                        list.add(clazz.cast(service));
                    }
                }
            }
        }
        if (parent != null) {
            list.addAll(parent.getServices(clazz));
        }
        return list;
    }

    @Override
    public boolean hasService(Class<?> clazz) {
        synchronized (services) {
            for (Object service : services.values()) {
                if (service instanceof Factory) {
                    if (clazz.isAssignableFrom(((Factory<?>) service).clazz)) {
                        if (isVisible(service)) {
                            return true;
                        }
                    }
                } else if (clazz.isInstance(service)) {
                    if (isVisible(service)) {
                        return true;
                    }
                }
            }
        }
        if (parent != null) {
            return parent.hasService(clazz);
        }
        return false;
    }

    protected boolean isVisible(Object service) {
        return true;
    }

    static class Factory<T> {

        final Class<T> clazz;
        final Callable<T> callable;

        Factory(Class<T> clazz, Callable<T> callable) {
            this.clazz = clazz;
            this.callable = callable;
        }

    }

}
