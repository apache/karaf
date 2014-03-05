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
package org.apache.karaf.shell.api.console;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Service registry.
 *
 * The registry can be used to register various services used during injection along
 * with {@link Command}s.
 *
 * @see org.apache.karaf.shell.api.console.SessionFactory
 * @see org.apache.karaf.shell.api.console.Session
 */
public interface Registry {

    /**
     * Return a list of available commands.
     */
    List<Command> getCommands();

    /**
     * Register a delayed service (or factory).
     * In cases where instances must be created for each injection,
     * a {@link Callable} can be registered and each injection will
     * call it to obtain the actual service implementation.
     *
     * @param factory
     * @param clazz
     * @param <T>
     */
    <T> void register(Callable<T> factory, Class<T> clazz);

    /**
     * Register a service.
     */
    void register(Object service);

    /**
     * Unregister a service.
     * If the registration has been done using a factory, the same
     * factory should be used to unregister.
     */
    void unregister(Object service);

    /**
     * Obtain a service implementing the given class.
     */
    <T> T getService(Class<T> clazz);

    /**
     * Obtain a list of services implementing the given class.
     */
    <T> List<T> getServices(Class<T> clazz);

    /**
     * Check whether the registry has a service of the given class.
     */
    boolean hasService(Class<?> clazz);

}
