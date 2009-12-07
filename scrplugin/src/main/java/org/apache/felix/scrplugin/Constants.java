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
package org.apache.felix.scrplugin;

import java.io.File;

/**
 * The <code>Constants</code> interface provides use full constants for various
 * values used for processing SCR annotations and JavaDoc tags into SCR
 * descriptors.
 */
public interface Constants {

    /** Version 1.0 (R4.1) */
    public static final int VERSION_1_0 = 0;

    /** Version 1.1 (R4.2) */
    public static final int VERSION_1_1 = 1;

    /** Version 1.1-felix (R4.2 + FELIX-1893) */
    public static final int VERSION_1_1_FELIX = 2;

    /**
     * The name of the Bundle manifest header providing the list of service
     * component descriptor files.
     */
    public static final String SERVICE_COMPONENT = "Service-Component";

    public static final String COMPONENT = "scr.component";

    public static final String COMPONENT_NAME = "name";

    public static final String COMPONENT_LABEL = "label";

    public static final String COMPONENT_DESCRIPTION = "description";

    public static final String COMPONENT_ENABLED = "enabled";

    public static final String COMPONENT_FACTORY = "factory";

    public static final String COMPONENT_IMMEDIATE = "immediate";

    public static final String COMPONENT_INHERIT = "inherit";

    public static final String COMPONENT_METATYPE = "metatype";

    public static final String COMPONENT_ABSTRACT = "abstract";

    public static final String COMPONENT_DS = "ds";

    // Force the descriptor version (since 1.4.1)
    public static final String COMPONENT_DS_SPEC_VERSION = "specVersion";

    // Specification version identifier for SCR 1.0 (R 4.1)
    public static final String COMPONENT_DS_SPEC_VERSION_10 = "1.0";

    // Specification version identifier for SCR 1.1 (R 4.2)
    public static final String COMPONENT_DS_SPEC_VERSION_11 = "1.1";

    // Specification version identifier for SCR 1.1-felix (R 4.2+FELIX-1893)
    public static final String COMPONENT_DS_SPEC_VERSION_11_FELIX = "1.1-felix";

    public static final String COMPONENT_CREATE_PID = "create-pid";

    public static final String COMPONENT_SET_METATYPE_FACTORY_PID = "configurationFactory";

    // The component configuration policy (V1.1)
    public static final String COMPONENT_CONFIG_POLICY = "policy";
    public static final String COMPONENT_CONFIG_POLICY_OPTIONAL = "optional";
    public static final String COMPONENT_CONFIG_POLICY_REQUIRE = "require";
    public static final String COMPONENT_CONFIG_POLICY_IGNORE = "ignore";

    // The component activate method name (V1.1)
    public static final String COMPONENT_ACTIVATE = "activate";

    // The component activate method name (V1.1)
    public static final String COMPONENT_DEACTIVATE = "deactivate";

    // The component modified method name (V1.1)
    public static final String COMPONENT_MODIFIED = "modified";

    public static final String PROPERTY = "scr.property";

    public static final String PROPERTY_NAME = "name";

    public static final String PROPERTY_NAME_REF = "nameRef";

    public static final String PROPERTY_LABEL = "label";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PROPERTY_VALUE = "value";

    public static final String PROPERTY_MULTIVALUE_PREFIX = "values";

    public static final String PROPERTY_VALUE_REF = "valueRef";

    /** Property for multi value fields using references. */
    public static final String PROPERTY_MULTIVALUE_REF_PREFIX = "valueRefs";

    // property type
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_TYPE_STRING = "String";
    public static final String PROPERTY_TYPE_LONG = "Long";
    public static final String PROPERTY_TYPE_DOUBLE = "Double";
    public static final String PROPERTY_TYPE_FLOAT = "Float";
    public static final String PROPERTY_TYPE_INTEGER = "Integer";
    public static final String PROPERTY_TYPE_BYTE = "Byte";
    public static final String PROPERTY_TYPE_CHAR = "Char";
    public static final String PROPERTY_TYPE_CHAR_1_1 = "Character";
    public static final String PROPERTY_TYPE_BOOLEAN = "Boolean";
    public static final String PROPERTY_TYPE_SHORT = "Short";

    public static final String PROPERTY_CARDINALITY = "cardinality";

    public static final String PROPERTY_PRIVATE = "private";

    public static final String PROPERTY_OPTIONS = "options";

    public static final String SERVICE = "scr.service";

    public static final String SERVICE_INTERFACE = "interface";

    public static final String SERVICE_FACTORY = "servicefactory";

    public static final String REFERENCE = "scr.reference";

    public static final String REFERENCE_NAME = "name";

    public static final String REFERENCE_NAME_REF = "nameRef";

    public static final String REFERENCE_INTERFACE = "interface";

    public static final String REFERENCE_CARDINALITY = "cardinality";

    public static final String REFERENCE_POLICY = "policy";

    public static final String REFERENCE_TARGET = "target";

    public static final String REFERENCE_BIND = "bind";

    public static final String REFERENCE_UNDBIND = "unbind";

    public static final String REFERENCE_UPDATED = "updated";

    public static final String REFERENCE_CHECKED = "checked";

    /** Lookup strategy for references @since 1.0.9 */
    public static final String REFERENCE_STRATEGY = "strategy";
    public static final String REFERENCE_STRATEGY_LOOKUP = "lookup";
    public static final String REFERENCE_STRATEGY_EVENT = "event";

    public static final String ABSTRACT_DESCRIPTOR_FILENAME = "scrinfo.xml";

    public static final String ABSTRACT_DESCRIPTOR_ARCHIV_PATH = "OSGI-INF/scr-plugin/" + ABSTRACT_DESCRIPTOR_FILENAME;

    public static final String ABSTRACT_DESCRIPTOR_RELATIVE_PATH = ABSTRACT_DESCRIPTOR_ARCHIV_PATH.replace( '/',
        File.separatorChar );
}
