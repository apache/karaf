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
package org.apache.felix.dm.runtime;

import java.util.EnumSet;

/**
 * List of all valid attribute a DependencyManager component descriptor may contain.
 * @see DescriptorParser for the descriptor syntax.
 */
public enum DescriptorParam
{
    /* Service attribute for the init method (the parsed value is a String) */
    init,

    /* Service attribute for the start method (the parsed value is a String) */
    start,

    /* Stop attribute for the stop method (the parsed value is a String) */
    stop,

    /* Service attribute for the destroy method (the parsed value is a String) */
    destroy,

    /* Service attribute for the impl service class name (the parsed value is a String) */
    impl,

    /* Service attribute for the provided services class names (the parsed value is a String[]) */
    provide,

    /* Service attribute for the provided service properties (the parsed value is a Hashtable) */
    properties,

    /* Service attribute for the factory class name (the parsed value is a String) */
    factory,

    /* Service attribute for the factory method name (the parsed value is a String) */
    factoryMethod,

    /* Service attribute for the composition method name (the parsed value is a String) */
    composition,

    /* ServiceDependency attribute for the interface we depend on (the parsed value is a String) */
    service,

    /* ServiceDependency attribute for the service filter (the parsed value is a String) */
    filter,

    /* ServiceDependency attribute for the service default impl (the parsed value is a String) */
    defaultImpl,

    /* ServiceDependency attribute for the required booleean (the parsed value is a String ("false"|"true") */
    required,

    /* ServiceDependency attribute for the added callback name (the parsed value is a String) */
    added,

    /* ServiceDependency attribute for the added changed name (the parsed value is a String) */
    changed,

    /* ServiceDependency attribute for the added removed name (the parsed value is a String) */
    removed,

    /* ServiceDependency attribute for the auto config field name (the parsed value is a String) */
    autoConfig,

    /* ConfigurationDependency attribute for the PID (the parsed value is a String) */
    pid,

    /* ConfigurationDependency attribute for the propagate boolean (the parsed value is a String ("false"|"true") */
    propagate,

    /* ConfigurationDependency attribute for the updated callback method (the parsed value is a String */
    updated,
    
    /* TemporalServiceDependency attribute for the timeout (the parsed value is a String) */
    timeout,
    
    /* AdapterService attribute for the adapter interface (the parsed value is a String) */
    adapterService,
    
    /* AdapterService attribute for the adapter service properties (the parsed value is a Hashtable) */
    adapterProperties,
    
    /* AdapterService attribute for the adaptee service (the parsed value is a String) */
    adapteeService,
    
    /* AdapterService attribute for the adaptee service filter (the parsed value is a String) */
    adapteeFilter,
    
    /* BundleDependency attribute for the state mask bundle (the parsed value is a string) */
    stateMask;
    
    /**
     * Indicates if a given attribute is a Service attribute.
     * @param attr a Descriptor attribute
     * @return true if the descriptor is a Service attribute, false if not
     */
    public static boolean isServiceAttribute(DescriptorParam attr) {
        return serviceAttribute.contains(attr);
    }
    
    /**
     * Indicates if a given attribute is a ServiceDependency attribute.
     * @param attr a Descriptor attribute
     * @return true if the descriptor is a Service attribute, false if not
     */
    public static boolean isServiceDepependencyAttribute(DescriptorParam attr) {
        return serviceDependencyAttribute.contains(attr);
    }

    /**
     * Indicates if a given attribute is a TemporalServiceDependency attribute.
     * @param attr a Descriptor attribute
     * @return true if the descriptor is a Temporal Service attribute, false if not
     */
    public static boolean isTemporalServiceDepependencyAttribute(DescriptorParam attr) {
        return serviceDependencyAttribute.contains(attr) || attr == timeout;
    }

    /**
     * Indicates if a given attribute is a ServiceDependency attribute.
     * @param attr a Descriptor attribute
     * @return true if the descriptor is a Service attribute, false if not
     */
    public static boolean isConfigurationDepependencyAttribute(DescriptorParam attr) {
        return configurationDependencyAttribute.contains(attr);
    }

    /**
     * List of Service attributes
     */
    private final static EnumSet serviceAttribute = EnumSet.range(init, factoryMethod);

    /**
     * List of ServiceDependency attributes
     */
    private final static EnumSet serviceDependencyAttribute = EnumSet.range(service, autoConfig);

    /**
     * List of ConfigurationDependency attributes
     */
    private final static EnumSet configurationDependencyAttribute = EnumSet.range(pid, updated);
}
