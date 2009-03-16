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
package org.apache.felix.moduleloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface IModule
{
    void setSecurityContext(Object securityContext);
    Object getSecurityContext();

    // Metadata access methods.
    Map getHeaders();
    boolean isExtension();
    String getSymbolicName();
    Version getVersion();
    ICapability[] getCapabilities();
    IRequirement[] getRequirements();
    IRequirement[] getDynamicRequirements();
    R4Library[] getNativeLibraries();

    // Run-time data access methods.
    Bundle getBundle();
    String getId();
    IWire[] getWires();
    boolean isResolved();

    // Content access methods.
    IContent getContent();
    Class getClassByDelegation(String name) throws ClassNotFoundException;
    URL getResourceByDelegation(String name);
    Enumeration getResourcesByDelegation(String name);
    URL getEntry(String name);

    // TODO: ML - For expediency, the index argument was added to these methods
    // but it is not clear that this makes sense in the long run. This needs to
    // be readdressed in the future, perhaps by the spec to clearly indicate
    // how resources on the bundle class path are searched, which is why we
    // need the index number in the first place -- to differentiate among
    // resources with the same name on the bundle class path. This was previously
    // handled as part of the resource path, but that approach is not spec
    // compliant.
    boolean hasInputStream(int index, String urlPath)
        throws IOException;
    InputStream getInputStream(int index, String urlPath)
        throws IOException;
}