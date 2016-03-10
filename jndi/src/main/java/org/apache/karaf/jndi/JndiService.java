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
package org.apache.karaf.jndi;

import java.util.List;
import java.util.Map;

/**
 * JNDI Service.
 */
public interface JndiService {

    /**
     * List the current JNDI names (with the bound class name).
     *
     * @return The JNDI names.
     * @throws Exception If the service fails.
     */
    Map<String, String> names() throws Exception;

    /**
     * List the current JNDI names in the given context.
     *
     * @param context The JNDI context.
     * @return The JNDI names in the context.
     * @throws Exception If the service fails.
     */
    Map<String, String> names(String context) throws Exception;

    /**
     * List all JNDI sub-contexts.
     *
     * @return A {@link List} containing the sub-context names.
     * @throws Exception If the service fails.
     */
    List<String> contexts() throws Exception;

    /**
     * List the JNDI sub-context from a given context.
     *
     * @param context The base JNDI context.
     * @return A {@link List} containing the sub-context names.
     * @throws Exception If the service fails.
     */
    List<String> contexts(String context) throws Exception;

    /**
     * Create a sub-context.
     *
     * @param context The new sub-context name to create.
     * @throws Exception If the service fails.
     */
    void create(String context) throws Exception;

    /**
     * Delete a sub-context.
     *
     * @param context The sub-context name to delete.
     * @throws Exception If the service fails.
     */
    void delete(String context) throws Exception;

    /**
     * Create an alias on a given JNDI name.
     *
     * @param name The JNDI name.
     * @param alias The alias.
     * @throws Exception If the service fails.
     */
    void alias(String name, String alias) throws Exception;

    /**
     * Bind a given OSGi service to a JNDI name.
     *
     * @param serviceId The OSGi service ID.
     * @param name The JNDI name.
     * @throws Exception If the service fails.
     */
    void bind(long serviceId, String name) throws Exception;

    /**
     * Unbind an existing name.
     *
     * @param name The JNDI name to unbind.
     * @throws Exception If the service fails.
     */
    void unbind(String name) throws Exception;

}
