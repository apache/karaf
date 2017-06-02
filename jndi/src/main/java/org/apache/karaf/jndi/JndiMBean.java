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

import javax.management.MBeanException;
import java.util.List;
import java.util.Map;

/**
 * JNDI Service MBean
 */
public interface JndiMBean {

    /**
     * Get a map of JNDI names/class names (as attribute).
     *
     * @return The MBean attribute containing the {@link Map} of names/class names.
     * @throws MBeanException If the MBean fails.
     */
    Map<String, String> getNames() throws MBeanException;

    /**
     * Get a list of JNDI sub-contexts (as attribute).
     *
     * @return The MBean attribute containing the {@link List} of sub-contexts.
     * @throws MBeanException If the MBean fails.
     */
    List<String> getContexts() throws MBeanException;

    /**
     * Get a {@link Map} of JNDI names/class names children of a given base context.
     *
     * @param context The base context.
     * @return The {@link Map} of names/class names.
     * @throws MBeanException If the MBean fails.
     */
    Map<String, String> getNames(String context) throws MBeanException;

    /**
     * Get a {@link List} of JNDI sub-contexts children of a given base context.
     *
     * @param context The base context.
     * @return The {@link List} of sub-contexts.
     * @throws MBeanException If the MBean fails.
     */
    List<String> getContexts(String context) throws MBeanException;

    /**
     * Create a JNDI sub-context.
     *
     * @param context The JNDI sub-context name.
     * @throws MBeanException If the MBean fails.
     */
    void create(String context) throws MBeanException;

    /**
     * Delete a JNDI sub-context.
     *
     * @param context The JNDI sub-context name.
     * @throws MBeanException If the MBean fails.
     */
    void delete(String context) throws MBeanException;

    /**
     * Create another JNDI name (alias) for a given one.
     *
     * @param name The "source" JNDI name.
     * @param alias The JNDI alias name.
     * @throws MBeanException If the MBean fails.
     */
    void alias(String name, String alias) throws MBeanException;

    /**
     * Bind an OSGi service with a JNDI name.
     *
     * @param serviceId The OSGi service id (service.id property on the service, created by the framework).
     * @param name The JNDI name.
     * @throws MBeanException If the MBean fails.
     */
    void bind(Long serviceId, String name) throws MBeanException;

    /**
     * Unbind a given JNDI name.
     *
     * @param name The JNDI name.
     * @throws MBeanException If the MBean fails.
     */
    void unbind(String name) throws MBeanException;

}
