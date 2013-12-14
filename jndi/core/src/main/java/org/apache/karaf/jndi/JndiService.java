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
     * @return the JNDI names.
     * @throws Exception
     */
    Map<String, String> names() throws Exception;

    /**
     * List the current JNDI names in the given context.
     *
     * @param context the JNDI context.
     * @return the JNDI names in the context.
     * @throws Exception
     */
    Map<String, String> names(String context) throws Exception;

    /**
     * List all JNDI sub-contexts.
     *
     * @return a list containing the sub-context names.
     * @throws Exception
     */
    List<String> contexts() throws Exception;

    /**
     * List the JNDI sub-context from a given context.
     *
     * @param context the base JNDI context.
     * @return a list containing the sub-context names.
     * @throws Exception
     */
    List<String> contexts(String context) throws Exception;

    /**
     * Create a sub-context.
     *
     * @param context the new sub-context name to create.
     * @throws Exception
     */
    void create(String context) throws Exception;

    /**
     * Delete a sub-context.
     *
     * @param context the sub-context name to delete.
     * @throws Exception
     */
    void delete(String context) throws Exception;

    /**
     * Create an alias on a given JNDI name.
     *
     * @param name the JNDI name.
     * @param alias the alias.
     * @throws Exception
     */
    void alias(String name, String alias) throws Exception;

    /**
     * Bind a given OSGi service to a JNDI name.
     *
     * @param serviceId the OSGi service ID.
     * @param name the JNDI name.
     * @throws Exception
     */
    void bind(long serviceId, String name) throws Exception;

    /**
     * Unbind an existing name.
     *
     * @param name the JNDI name to unbind.
     * @throws Exception
     */
    void unbind(String name) throws Exception;

}
