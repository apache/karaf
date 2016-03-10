/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.api.action.lifecycle;

/**
 * <p>The <code>Manager</code> service can be used to programmatically
 * register {@link org.apache.karaf.shell.api.action.Action}s or
 * {@link org.apache.karaf.shell.api.console.Completer}s.</p>
 *
 * <p>Registered objects must be annotated with the {@link Service} annotation.</p>
 *
 * <p>Objects will be registered in the {@link org.apache.karaf.shell.api.console.Registry}
 * associated with this <code>Manager</code>.</p>
 *
 * @see org.apache.karaf.shell.api.console.Registry
 * @see org.apache.karaf.shell.api.action.lifecycle.Service
 */
public interface Manager {

    /**
     * Register a service.
     * If the given class is an {@link org.apache.karaf.shell.api.action.Action},
     * a {@link org.apache.karaf.shell.api.console.Command} will be created and registered,
     * else, an instance of the class will be created, injected and registered.
     *
     * @param clazz the Action class to register.
     */
    void register(Class<?> clazz);

    /**
     * Unregister a previously registered class.
     *
     * @param clazz the Action class to unregister.
     */
    void unregister(Class<?> clazz);

}
