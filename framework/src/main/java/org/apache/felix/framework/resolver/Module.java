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
package org.apache.felix.framework.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public interface Module
{
    final static int EAGER_ACTIVATION = 0;
    final static int LAZY_ACTIVATION = 1;

    // Metadata access methods.
    Map getHeaders();
    boolean isExtension();
    String getSymbolicName();
    Version getVersion();
    List<Capability> getCapabilities();
    List<Requirement> getRequirements();
    List<Requirement> getDynamicRequirements();
    List<R4Library> getNativeLibraries();
    int getDeclaredActivationPolicy();

    // Run-time data access methods.
    Bundle getBundle();
    String getId();
    List<Wire> getWires();
    boolean isResolved();
    Object getSecurityContext();

    // Content access methods.
    Content getContent();
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