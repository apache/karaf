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
 * Constants
 */
public class Constants {

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

    public static final String COMPONENT_CREATE_PID = "create-pid";

    public static final String PROPERTY = "scr.property";

    public static final String PROPERTY_NAME = "name";

    public static final String PROPERTY_LABEL = "label";

    public static final String PROPERTY_DESCRIPTION = "description";

    public static final String PROPERTY_VALUE = "value";

    public static final String PROPERTY_MULTIVALUE_PREFIX = "values";

    public static final String PROPERTY_VALUE_REF = "valueRef";

    public static final String PROPERTY_MULTIVALUE_REF_PREFIX = "refValues";

    public static final String PROPERTY_TYPE = "type";

    public static final String PROPERTY_CARDINALITY = "cardinality";

    public static final String PROPERTY_PRIVATE = "private";

    public static final String PROPERTY_OPTIONS = "options";

    public static final String SERVICE = "scr.service";

    public static final String SERVICE_INTERFACE = "interface";

    public static final String SERVICE_FACTORY = "servicefactory";

    public static final String REFERENCE = "scr.reference";

    public static final String REFERENCE_NAME = "name";

    public static final String REFERENCE_INTERFACE = "interface";

    public static final String REFERENCE_CARDINALITY = "cardinality";

    public static final String REFERENCE_POLICY = "policy";

    public static final String REFERENCE_TARGET = "target";

    public static final String REFERENCE_BIND = "bind";

    public static final String REFERENCE_UNDBIND = "unbind";

    public static final String ABSTRACT_DESCRIPTOR_FILENAME = "scrinfo.xml";

    public static final String ABSTRACT_DESCRIPTOR_RELATIVE_PATH = "OSGI-INF" + File.separator + "scr-plugin" + File.separator + ABSTRACT_DESCRIPTOR_FILENAME;

    public static final String ABSTRACT_DESCRIPTOR_ARCHIV_PATH = "OSGI-INF/scr-plugin/" + ABSTRACT_DESCRIPTOR_FILENAME;

}
