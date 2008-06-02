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
package org.apache.felix.deploymentadmin;

public interface Constants extends org.osgi.framework.Constants {

    // manifest main attribute header constants
    public static final String DEPLOYMENTPACKAGE_SYMBOLICMAME = "DeploymentPackage-SymbolicName";
    public static final String DEPLOYMENTPACKAGE_VERSION = "DeploymentPackage-Version";
    public static final String DEPLOYMENTPACKAGE_FIXPACK = "DeploymentPackage-FixPack";

    // manifest 'name' section header constants
    public static final String RESOURCE_PROCESSOR = "Resource-Processor";
    public static final String DEPLOYMENTPACKAGE_MISSING = "DeploymentPackage-Missing";
    public static final String DEPLOYMENTPACKAGE_CUSTOMIZER = "DeploymentPackage-Customizer";

    // event topics and properties
    public static final String EVENTTOPIC_INSTALL = "org/osgi/service/deployment/INSTALL";
    public static final String EVENTTOPIC_UNINSTALL = "org/osgi/service/deployment/UNINSTALL";
    public static final String EVENTTOPIC_COMPLETE = "org/osgi/service/deployment/COMPLETE";
    public static final String EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME = "deploymentpackage.name";
    public static final String EVENTPROPERTY_SUCCESSFUL = "successful";

    // miscellaneous constants
    public static final String BUNDLE_LOCATION_PREFIX = "osgi-dp:";
}